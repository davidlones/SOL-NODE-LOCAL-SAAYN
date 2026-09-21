# Source-export validation

Preparation date: September 21, 2026.

Passed locally in this separate checkout:

- API18 manifest, non-exported service and permission/network boundary checks.
- SSE framing, multiline data, CRLF handling, premature termination and size-limit tests.
- Existing SAAYN dialogue primer, role-stop, cleanup and sanitization tests.
- Bit-identical current-source linear fixtures for 1/2/4/8/16 rows on the host.
- Tracked-source export scan and Git whitespace checks.
- Native ARM source build with Zig 0.16.0.
- Full APK build using JDK17, Android platform18 and Build Tools35.0.0.
- Complete AAR/JAR hash verification using the existing local download cache.
- Development APK signature verification for API18: v1, v2 and v3 verified.

The build used no configured public API origin. It did not contact the production SOL chat endpoint or install to a phone. Download caching was used because a fresh Maven transfer was slow; this is not a claim of a completed uncached network bootstrap. Legacy dependency debug-table warnings from D8 remain in the build log.

GitHub host checks also [passed on the initial publication commit](https://github.com/davidlones/SOL-NODE-LOCAL-SAAYN/actions/runs/35667386803) (`bdb168e`). The workflow tests host behavior and export boundaries; it does not build or run an Android APK. Physical-device measurements and UI screenshots in the README describe the preceding development installation, not a new acceptance run of the source export.
