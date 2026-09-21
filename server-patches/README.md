# Optional SOL server optimization

This is a narrow patch for the existing SOL Python chat server, not a standalone backend. The Android app continues to use the existing public API contract. No API schema or GPT Action re-import is required.

The server generates a supplemental search query with a model before response-cache lookup. Identical repeat inputs previously paid that model cost again. This patch adds an in-memory LRU cache of up to 128 generated queries, expiring after 300 seconds. Keys include the full model prompt, backend profile, backend URL, model ID and sampling settings. Only generated query strings are reused; retrieval results, live telemetry and final answers are not cached by this new layer. Model failures are not cached. A cached empty query still runs the existing deterministic fallback against current retrieval metadata.

The first request still incurs generation cost. Changed context misses the cache. Cache contents disappear when the service restarts. Concurrent identical misses may each compute; the global cache lock is not held during model inference. A hit does not bypass backend availability checks.

Apply only to a compatible server checkout after a backup and review:

```sh
git apply --check /path/to/supplemental-query-cache.patch
git apply /path/to/supplemental-query-cache.patch
PYTHONDONTWRITEBYTECODE=1 python3 /path/to/check_supplemental_cache.py ./bin/sol_chat_api.py
```

Run the server's existing API contract checks before restarting it. The included isolated test covers exact repeat hits, context/profile/model separation, TTL expiry, eviction, fallback preservation and uncached failures. It uses mocked generation and makes no model or network requests.
