# Dependency and build notes

`dependencies.lock.json` records official Maven URLs and SHA256 digests for complete AAR/JAR downloads. Downloads are validated before resource extraction. The build uses legacy dependency versions intentionally for API18 compatibility; this is not a claim of current support or security maintenance.

The native helper is built from source with Zig 0.16.0 targeting static ARMv7 Linux-musl, `cortex_a9`, `-O3`, without a fast-math flag. Generated executables are excluded from Git. The original reference C implementation remains available for regression comparison.

The API18 APK requires a v1 signature. The build verifies the signed development artifact using apksigner with min-sdk-version 18. Release signing is intentionally not automated by this preparation.

Public CA certificate sources:

- https://pki.goog/repo/certs/gtsr4.pem
- https://letsencrypt.org/certs/isrgrootx1.pem

Google provider integration: https://developer.android.com/privacy-and-security/security-gms-provider

The installed device's Google Play Services provider is used when public chat starts. Bundling old provider client classes does not install a current TLS engine or establish current security updates on the phone. Legacy third-party D8 debug-table warnings are separate from compilation, signature verification and runtime acceptance.
