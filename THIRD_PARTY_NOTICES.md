# Provenance and third-party notices

## SOL Node and SAAYN lineage

The Android dialogue policy was ported from the existing local `saayn_chat.py`, `saayn_chat_py2.py`, and `saayn_openai/engines.py` wrappers. The numerical helper derives from the local Android SAAYN implementation; its reference source is preserved as `native/saayn_s4_native.c`. `native/saayn_s4_optimized.c` adds output tiling while preserving per-output accumulation order.

The underlying experiment builds on [Spreadsheets Are All You Need](https://github.com/ianand/spreadsheets-are-all-you-need). The wrappers and Android helper inspected during preparation were local additions rather than tracked upstream files. No original workbook or model tensor is distributed here. Upstream naming and model references do not imply endorsement or a license grant.

## Build dependencies

- OkHttp 3.12.13 and Okio 1.15.0: Apache-2.0 project licenses.
- Android support libraries 25.2.0: Android project distribution terms; consult the downloaded artifacts and upstream notices when distributing binaries.
- Google Play Services base/basement/tasks 11.0.4: Google distribution terms. These are fetched at build time, not vendored into Git.
- Android SDK platform 18 and Build Tools 35.0.0: installed separately under their applicable SDK terms.
- Zig 0.16.0: installed separately. Consult the toolchain's licenses and runtime notices when distributing compiled artifacts.
- The static native helper links musl. Its bundled copyright/license notice is preserved in `docs/licenses/musl-COPYRIGHT.txt`.
- GTS Root R4 and ISRG Root X1 are public CA certificates obtained from their issuers; they contain no private keys.

A production binary release needs its own complete dependency notice review and signing process. This source preparation does not claim that a project-wide license overrides any of these terms.
