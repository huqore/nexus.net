# Auto-Release and Versioning Rule

Whenever a new feature, update, or bug fix is completed:
1. Increment `versionCode` (e.g. +1) and bump `versionName` (e.g. `1.0.1` -> `1.0.2` or `x.x.x-test`) in `app/build.gradle.kts`.
2. Commit and push changes to GitHub repository `huqore/nexus.net` using the authenticated remote.
3. Build the release APK (`assembleRelease`).
4. **APK Storage locations (NEVER create APKs in the project root):**
   - Test APKs: save strictly into `apks/test/nexus-net-v<version>.apk`.
   - Release APKs: save strictly into `apks/release/nexus-net-v<version>.apk`.
   - **Do not put APK files in the project root!**
5. For official releases: Publish a new GitHub Release with the tag matching `v<versionName>` and attach `nexus-net-v<versionName>.apk` and `app-universal-release.apk`.
