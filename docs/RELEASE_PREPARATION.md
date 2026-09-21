# Source publication

Destination: [davidlones/SOL-NODE-LOCAL-SAAYN](https://github.com/davidlones/SOL-NODE-LOCAL-SAAYN), public, main branch.

This checkout is a sanitized source export of SOL Node 0.3.0. It excludes operational transcripts, device serials, household network addresses, SSH aliases, BlueBubbles automation, personal filesystem locations, APKs, private signing keys and model tensors.

The repository owner's initial commit and MIT license are preserved. Third-party provenance and dependency terms are documented separately. Local validation is recorded in [VALIDATION.md](VALIDATION.md); GitHub Actions runs host checks on pushes and pull requests.

Before distributing binaries:

- Choose a release-signing identity; the provided script produces development builds.
- Complete the dependency notice review for the intended binary distribution.
- Configure and verify the intended public SOL endpoint.
- Perform device acceptance testing on that exact APK.
- Retain the distinction between tested runtime behavior, model quality and unfinished remote-node authority.

This publication contains source and recorded UI evidence. It does not publish an APK or model weights, or update an installed device.
