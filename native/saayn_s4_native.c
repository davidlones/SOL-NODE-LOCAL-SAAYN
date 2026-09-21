#define _POSIX_C_SOURCE 200809L

#include <errno.h>
#include <fcntl.h>
#include <float.h>
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

#define EMBED_DIM 768
#define NUM_HEADS 12
#define HEAD_DIM 64
#define NUM_LAYERS 12
#define VOCAB_SIZE 50257
#define MAX_CONTEXT 16
#define PATH_BUFFER 4096

typedef struct {
    void *mapping;
    size_t mapping_len;
    const float *data;
} mapped_array;

typedef struct {
    mapped_array ln_1_g;
    mapped_array ln_1_b;
    mapped_array attn_w;
    mapped_array attn_b;
    mapped_array attn_proj_w;
    mapped_array attn_proj_b;
    mapped_array ln_2_g;
    mapped_array ln_2_b;
    mapped_array mlp_fc_w;
    mapped_array mlp_fc_b;
    mapped_array mlp_proj_w;
    mapped_array mlp_proj_b;
} block_maps;

static void die(const char *message) {
    fprintf(stderr, "saayn_s4_native: %s\n", message);
    exit(2);
}

static void die_path(const char *message, const char *path) {
    fprintf(stderr, "saayn_s4_native: %s: %s (%s)\n", message, path, strerror(errno));
    exit(2);
}

static uint16_t read_u16le(const unsigned char *p) {
    return (uint16_t)p[0] | ((uint16_t)p[1] << 8);
}

static uint32_t read_u32le(const unsigned char *p) {
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) |
           ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}

static mapped_array map_npy(const char *cache_dir, const char *name) {
    char path[PATH_BUFFER];
    mapped_array result;
    memset(&result, 0, sizeof(result));
    if (snprintf(path, sizeof(path), "%s/%s.npy", cache_dir, name) >= (int)sizeof(path)) {
        die("cache path is too long");
    }

    int fd = open(path, O_RDONLY);
    if (fd < 0) die_path("cannot open array", path);
    struct stat st;
    if (fstat(fd, &st) != 0) die_path("cannot stat array", path);
    if (st.st_size < 16) die("invalid .npy file");

    void *mapping = mmap(NULL, (size_t)st.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
    close(fd);
    if (mapping == MAP_FAILED) die_path("cannot map array", path);

    const unsigned char *bytes = (const unsigned char *)mapping;
    if (memcmp(bytes, "\x93NUMPY", 6) != 0) die("invalid .npy magic");
    unsigned int major = bytes[6];
    size_t header_len;
    size_t data_offset;
    if (major == 1) {
        header_len = read_u16le(bytes + 8);
        data_offset = 10 + header_len;
    } else if (major == 2 || major == 3) {
        header_len = read_u32le(bytes + 8);
        data_offset = 12 + header_len;
    } else {
        die("unsupported .npy version");
    }
    if (data_offset >= (size_t)st.st_size) die("invalid .npy header length");

    const char *header_source = (const char *)(bytes + (major == 1 ? 10 : 12));
    char *header = (char *)malloc(header_len + 1);
    if (header == NULL) die("out of memory while reading .npy header");
    memcpy(header, header_source, header_len);
    header[header_len] = '\0';
    if (strstr(header, "'descr': '<f4'") == NULL &&
        strstr(header, "\"descr\": \"<f4\"") == NULL) {
        die("expected little-endian float32 .npy array");
    }
    if (strstr(header, "'fortran_order': True") != NULL) {
        die("Fortran-order .npy arrays are not supported");
    }
    free(header);

    result.mapping = mapping;
    result.mapping_len = (size_t)st.st_size;
    result.data = (const float *)(bytes + data_offset);
    return result;
}

static void unmap_array(mapped_array *array) {
    if (array->mapping != NULL) munmap(array->mapping, array->mapping_len);
    memset(array, 0, sizeof(*array));
}

static float *allocate_floats(size_t count) {
    float *result = (float *)malloc(count * sizeof(float));
    if (result == NULL) die("out of memory");
    return result;
}

static void layer_norm(
    const float *input,
    int rows,
    int cols,
    const float *gain,
    const float *bias,
    float *output
) {
    for (int row = 0; row < rows; ++row) {
        const float *src = input + (size_t)row * cols;
        float *dst = output + (size_t)row * cols;
        double sum = 0.0;
        for (int col = 0; col < cols; ++col) sum += src[col];
        float mean = (float)(sum / cols);
        double variance_sum = 0.0;
        for (int col = 0; col < cols; ++col) {
            double delta = (double)src[col] - mean;
            variance_sum += delta * delta;
        }
        float inverse_std = 1.0f / sqrtf((float)(variance_sum / cols) + 1e-5f);
        for (int col = 0; col < cols; ++col) {
            dst[col] = (src[col] - mean) * inverse_std * gain[col] + bias[col];
        }
    }
}

static void linear(
    const float *restrict input,
    int rows,
    int in_dim,
    int out_dim,
    const float *restrict weight,
    const float *restrict bias,
    float *restrict output
) {
    for (int row = 0; row < rows; ++row) {
        float *dst = output + (size_t)row * out_dim;
        memcpy(dst, bias, (size_t)out_dim * sizeof(float));
        const float *src = input + (size_t)row * in_dim;
        for (int in_index = 0; in_index < in_dim; ++in_index) {
            const float value = src[in_index];
            const float *weight_row = weight + (size_t)in_index * out_dim;
            for (int out_index = 0; out_index < out_dim; ++out_index) {
                dst[out_index] += value * weight_row[out_index];
            }
        }
    }
}

static void attention(const float *qkv, int tokens, float *output) {
    const int qkv_stride = 3 * EMBED_DIM;
    if (tokens == 1) {
        memcpy(output, qkv + 2 * EMBED_DIM, EMBED_DIM * sizeof(float));
        return;
    }

    float scores[MAX_CONTEXT];
    float probabilities[MAX_CONTEXT];
    const float scale = 1.0f / sqrtf((float)HEAD_DIM);
    memset(output, 0, (size_t)tokens * EMBED_DIM * sizeof(float));

    for (int head = 0; head < NUM_HEADS; ++head) {
        const int head_offset = head * HEAD_DIM;
        for (int position = 0; position < tokens; ++position) {
            const float *query = qkv + (size_t)position * qkv_stride + head_offset;
            float max_score = -FLT_MAX;
            for (int source = 0; source <= position; ++source) {
                const float *key = qkv + (size_t)source * qkv_stride + EMBED_DIM + head_offset;
                float dot = 0.0f;
                for (int i = 0; i < HEAD_DIM; ++i) dot += query[i] * key[i];
                scores[source] = dot * scale;
                if (scores[source] > max_score) max_score = scores[source];
            }
            float total = 0.0f;
            for (int source = 0; source <= position; ++source) {
                probabilities[source] = expf(scores[source] - max_score);
                total += probabilities[source];
            }
            float *dst = output + (size_t)position * EMBED_DIM + head_offset;
            for (int source = 0; source <= position; ++source) {
                const float probability = probabilities[source] / total;
                const float *value = qkv + (size_t)source * qkv_stride + 2 * EMBED_DIM + head_offset;
                for (int i = 0; i < HEAD_DIM; ++i) dst[i] += probability * value[i];
            }
        }
    }
}

static void gelu_in_place(float *values, size_t count) {
    const float factor = sqrtf(2.0f / 3.14159265358979323846f);
    for (size_t i = 0; i < count; ++i) {
        const float x = values[i];
        values[i] = 0.5f * x * (1.0f + tanhf(factor * (x + 0.044715f * x * x * x)));
    }
}

static mapped_array map_block_array(const char *cache_dir, int layer, const char *suffix) {
    char name[128];
    if (snprintf(name, sizeof(name), "model_h%d_%s", layer, suffix) >= (int)sizeof(name)) {
        die("block array name is too long");
    }
    return map_npy(cache_dir, name);
}

static block_maps map_block(const char *cache_dir, int layer) {
    block_maps block;
    memset(&block, 0, sizeof(block));
    block.ln_1_g = map_block_array(cache_dir, layer, "ln_1_g");
    block.ln_1_b = map_block_array(cache_dir, layer, "ln_1_b");
    block.attn_w = map_block_array(cache_dir, layer, "attn_c_attn_w");
    block.attn_b = map_block_array(cache_dir, layer, "attn_c_attn_b");
    block.attn_proj_w = map_block_array(cache_dir, layer, "attn_c_proj_w");
    block.attn_proj_b = map_block_array(cache_dir, layer, "attn_c_proj_b");
    block.ln_2_g = map_block_array(cache_dir, layer, "ln_2_g");
    block.ln_2_b = map_block_array(cache_dir, layer, "ln_2_b");
    block.mlp_fc_w = map_block_array(cache_dir, layer, "mlp_c_fc_w");
    block.mlp_fc_b = map_block_array(cache_dir, layer, "mlp_c_fc_b");
    block.mlp_proj_w = map_block_array(cache_dir, layer, "mlp_c_proj_w");
    block.mlp_proj_b = map_block_array(cache_dir, layer, "mlp_c_proj_b");
    return block;
}

static void unmap_block(block_maps *block) {
    unmap_array(&block->ln_1_g);
    unmap_array(&block->ln_1_b);
    unmap_array(&block->attn_w);
    unmap_array(&block->attn_b);
    unmap_array(&block->attn_proj_w);
    unmap_array(&block->attn_proj_b);
    unmap_array(&block->ln_2_g);
    unmap_array(&block->ln_2_b);
    unmap_array(&block->mlp_fc_w);
    unmap_array(&block->mlp_fc_b);
    unmap_array(&block->mlp_proj_w);
    unmap_array(&block->mlp_proj_b);
}

static double monotonic_seconds(void) {
    struct timespec now;
    if (clock_gettime(CLOCK_MONOTONIC, &now) != 0) return 0.0;
    return (double)now.tv_sec + (double)now.tv_nsec / 1000000000.0;
}

int main(int argc, char **argv) {
    if (argc < 3) {
        fprintf(stderr, "usage: %s CACHE_DIR TOKEN_ID [TOKEN_ID ...]\n", argv[0]);
        return 2;
    }
    const char *cache_dir = argv[1];
    const int tokens = argc - 2;
    if (tokens < 1 || tokens > MAX_CONTEXT) die("context must contain 1 to 16 tokens");

    int token_ids[MAX_CONTEXT];
    for (int i = 0; i < tokens; ++i) {
        char *end = NULL;
        long value = strtol(argv[i + 2], &end, 10);
        if (end == argv[i + 2] || *end != '\0' || value < 0 || value >= VOCAB_SIZE) {
            die("invalid token ID");
        }
        token_ids[i] = (int)value;
    }

    const double started = monotonic_seconds();
    mapped_array wte = map_npy(cache_dir, "model_wte");
    mapped_array wpe = map_npy(cache_dir, "model_wpe");
    mapped_array ln_f_g = map_npy(cache_dir, "model_ln_f_g");
    mapped_array ln_f_b = map_npy(cache_dir, "model_ln_f_b");

    float *x = allocate_floats((size_t)tokens * EMBED_DIM);
    for (int position = 0; position < tokens; ++position) {
        const float *token_row = wte.data + (size_t)token_ids[position] * EMBED_DIM;
        const float *position_row = wpe.data + (size_t)position * EMBED_DIM;
        float *dst = x + (size_t)position * EMBED_DIM;
        for (int i = 0; i < EMBED_DIM; ++i) dst[i] = token_row[i] + position_row[i];
    }

    float *norm = allocate_floats((size_t)tokens * EMBED_DIM);
    float *qkv = allocate_floats((size_t)tokens * 3 * EMBED_DIM);
    float *attn_out = allocate_floats((size_t)tokens * EMBED_DIM);
    float *projection = allocate_floats((size_t)tokens * EMBED_DIM);
    float *mlp = allocate_floats((size_t)tokens * 4 * EMBED_DIM);

    for (int layer = 0; layer < NUM_LAYERS; ++layer) {
        fprintf(stderr, "SAAYN layer %d/%d\n", layer + 1, NUM_LAYERS);
        block_maps block = map_block(cache_dir, layer);

        layer_norm(x, tokens, EMBED_DIM, block.ln_1_g.data, block.ln_1_b.data, norm);
        linear(norm, tokens, EMBED_DIM, 3 * EMBED_DIM, block.attn_w.data, block.attn_b.data, qkv);
        attention(qkv, tokens, attn_out);
        linear(attn_out, tokens, EMBED_DIM, EMBED_DIM,
               block.attn_proj_w.data, block.attn_proj_b.data, projection);
        for (size_t i = 0; i < (size_t)tokens * EMBED_DIM; ++i) x[i] += projection[i];

        layer_norm(x, tokens, EMBED_DIM, block.ln_2_g.data, block.ln_2_b.data, norm);
        linear(norm, tokens, EMBED_DIM, 4 * EMBED_DIM,
               block.mlp_fc_w.data, block.mlp_fc_b.data, mlp);
        gelu_in_place(mlp, (size_t)tokens * 4 * EMBED_DIM);
        linear(mlp, tokens, 4 * EMBED_DIM, EMBED_DIM,
               block.mlp_proj_w.data, block.mlp_proj_b.data, projection);
        for (size_t i = 0; i < (size_t)tokens * EMBED_DIM; ++i) x[i] += projection[i];

        unmap_block(&block);
    }

    float final_embedding[EMBED_DIM];
    layer_norm(x + (size_t)(tokens - 1) * EMBED_DIM, 1, EMBED_DIM,
               ln_f_g.data, ln_f_b.data, final_embedding);

    int best_id = 0;
    float best_logit = -FLT_MAX;
    for (int token_id = 0; token_id < VOCAB_SIZE; ++token_id) {
        const float *embedding = wte.data + (size_t)token_id * EMBED_DIM;
        float logit = 0.0f;
        for (int i = 0; i < EMBED_DIM; ++i) logit += embedding[i] * final_embedding[i];
        if (logit > best_logit) {
            best_logit = logit;
            best_id = token_id;
        }
    }

    const double elapsed = monotonic_seconds() - started;
    printf("{\"token_id\":%d,\"logit\":%.9g,\"context_tokens\":%d,\"elapsed_seconds\":%.3f}\n",
           best_id, best_logit, tokens, elapsed);

    free(mlp);
    free(projection);
    free(attn_out);
    free(qkv);
    free(norm);
    free(x);
    unmap_array(&ln_f_b);
    unmap_array(&ln_f_g);
    unmap_array(&wpe);
    unmap_array(&wte);
    return 0;
}
