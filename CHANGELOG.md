# Changelog

## 0.3.0 — development milestone

- Added wrapper-derived local dialogue generation and full transcript view.
- Cached tokenizer tables and tiled native linear layers; measured 1.96× speedup for the 16-token fixture with exact sampled token/logit parity.
- Confirmed a generated public SOL response using the existing no-fallback request option.
- Preserved one-token diagnostic, telemetry, local inference confirmation and disabled remote tools.
- Prepared portable source build, archive-level dependency locking and host CI.

Known limits: 16-token local model context, repetitive base-model output, Activity-owned request orchestration, cold public startup disconnects, and no authenticated node/tool protocol.
