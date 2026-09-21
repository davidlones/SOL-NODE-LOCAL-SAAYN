#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
compiler="${SOL_ZIG:-zig}"
command -v "$compiler" >/dev/null || { echo 'Set SOL_ZIG to Zig 0.16.0 or install zig on PATH.' >&2; exit 1; }
mkdir -p assets
"$compiler" cc -target arm-linux-musleabihf -mcpu=cortex_a9 -O3 -std=c11 -static -s native/saayn_s4_optimized.c -lm -o assets/saayn_s4_native
