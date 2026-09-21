#include <stdio.h>
#include <stdlib.h>
#include <string.h>
static void baseline(
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

static void candidate(
    const float *restrict input,
    int rows,
    int in_dim,
    int out_dim,
    const float *restrict weight,
    const float *restrict bias,
    float *restrict output
) {
    /* Output tiling keeps all active rows in L1; weights are reused across rows.
       Each output retains the original in_index accumulation order. */
    for (int row = 0; row < rows; ++row)
        memcpy(output + (size_t)row * out_dim, bias, (size_t)out_dim * sizeof(float));
    for (int base = 0; base < out_dim; base += 128) {
        int width = out_dim - base < 128 ? out_dim - base : 128;
        for (int in_index = 0; in_index < in_dim; ++in_index) {
            const float *weight_row = weight + (size_t)in_index * out_dim + base;
            for (int row = 0; row < rows; ++row) {
                float value = input[(size_t)row * in_dim + in_index];
                float *dst = output + (size_t)row * out_dim + base;
                for (int out_index = 0; out_index < width; ++out_index)
                    dst[out_index] += value * weight_row[out_index];
            }
        }
    }
}

int main(){int rows=16,in=768,out=3072;float *x=malloc(rows*in*4),*w=malloc(in*out*4),*b=malloc(out*4),*a=malloc(rows*out*4),*z=malloc(rows*out*4);for(int i=0;i<rows*in;i++)x[i]=(i%29-14)*.031f;for(int i=0;i<in*out;i++)w[i]=(i%37-18)*.023f;for(int i=0;i<out;i++)b[i]=(i%5)*.01f;for(int n=1;n<=16;n*=2){baseline(x,n,in,out,w,b,a);candidate(x,n,in,out,w,b,z);if(memcmp(a,z,n*out*4)){puts("Numerical mismatch");return 1;}}puts("Linear 1/2/4/8/16-row outputs bit-identical on host fixture");return 0;}