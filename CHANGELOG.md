# Changelog

## 0.3.0 — development milestone

- Added wrapper-derived local dialogue generation and full transcript view.
- Cached tokenizer tables and tiled native linear layers; measured 1.96× speedup for the 16-token fixture with exact sampled token/logit parity.
- Confirmed a generated public SOL response using the existing no-fallback request option.
- Preserved one-token diagnostic, telemetry, local inference confirmation and disabled remote tools.
- Prepared portable source build, archive-level dependency locking and host CI.

Known limits: 16-token local model context, repetitive base-model output, Activity-owned request orchestration, cold public startup disconnects, and no authenticated node/tool protocol.

## Public endpoint and performance follow-up — 2026-09-21

- Default public endpoint restored to `https://sol.system42.one`; optional origin override and explicit local-only build retained.
- Increased bounded public-chat network waits for slow server preparation.
- Snapshot saved history once per streaming request, for public and local generation.
- Added a scoped server patch caching identical supplemental search-query generation for five minutes, plus isolated regression checks.
- Added later physical-device validation and screenshot evidence.
