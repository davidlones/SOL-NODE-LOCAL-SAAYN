#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test
python3 tests/verify_static.py
javac -d build/test src/one/system42/solnode/SseReader.java tests/SseReaderTest.java src/one/system42/solnode/DialoguePolicy.java tests/DialoguePolicyTest.java
java -cp build/test SseReaderTest
java -cp build/test DialoguePolicyTest
python3 tests/verify_native_parity.py
