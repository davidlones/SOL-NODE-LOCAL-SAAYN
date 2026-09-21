# Architecture checkpoint

The implemented app is a UI plus a bound, non-exported local service. Public network requests and transcript coordination are still owned partly by the Activity. Native inference and telemetry are behind the service.

## Next milestone: durable request ownership

Move request/conversation coordination into a service-owned repository with stable request IDs and persisted states. The UI submits actions and observes snapshots/events. Rotation, leaving the screen, reconnecting or restarting should not silently create duplicate work. Define explicit states for queued, running, streaming, cancelling, completed, failed and interrupted requests; retain a deliberate recovery policy for work Android kills.

Do not promise always-online availability merely by extending a Service class. Background lifecycle, restart behavior and idle power need physical-device tests on API18.

## Then: connect to the existing control system

The wider system already has separate device-control capabilities. Discover and reuse their actual authentication and authority model before designing another transport. A node interface should bind a revocable node identity and authenticated caller to explicitly allowed capabilities, arguments, deadlines, request IDs, replay policy and audit results.

Local telemetry, inference, camera and shell do not share the same authority. Remote tool execution remains disabled until that protocol and enforcement boundary exist. A successful chat request or possession of a session identifier does not confer node-control authority.

## Local inference limits

The application reproduces the existing wrapper's dialogue primer, role markers, response cleanup and 24-token default. The native helper sees only the last 16 tokens and recomputes them for each step. The stored transcript is not the model context. The tokenizer currently accepts ASCII and wider equivalence has not been established.

The current optimized helper reuses weights across context rows in linear layers. Tokenizer maps are cached in Java. No model quantization, permanent tensor rewrite, persistent native worker, JNI migration or attention KV cache is claimed.
