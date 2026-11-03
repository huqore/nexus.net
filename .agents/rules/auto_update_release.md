# Auto-Release and Versioning Rule

## Rules for Versioning:
- **#1**: Patch / bugfix -> `+0.0.1` (e.g. `3.0.1` -> `3.0.2`)
- **#2**: New features / screens -> `+0.1.0` minor bump (e.g. `3.0.1` -> `3.1.0`)
- **#3**: Major redesign / rewrite -> `+1.0.0` major bump (e.g. `3.0.1` -> `4.0.0`)

## Workflow for Test Versions:
1. Test builds are **STRICTLY LOCAL**.
2. **NEVER push test builds or test commits to GitHub / remote!** Git push is FORBIDDEN for test builds.
3. APK is saved strictly into `apks/test/nexus-net-v<version>-test.apk`.
4. NEVER create APK files in the project root folder.

## Workflow for Official Releases (only when user explicitly requests release):
1. Increment `versionCode` and bump `versionName` in `app/build.gradle.kts`.
2. Save APK strictly into `apks/release/nexus-net-v<version>.apk`.
3. Commit and push to GitHub repository `huqore/nexus.net`.
4. Publish a GitHub Release with tag `v<versionName>` and attach release APKs.
