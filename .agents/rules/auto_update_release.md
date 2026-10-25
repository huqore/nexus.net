# Auto-Release and Versioning Rule

Whenever a new feature, update, or bug fix is completed:
1. Increment `versionCode` (e.g. +1) and bump `versionName` (e.g. `1.0.1` -> `1.0.2`) in `app/build.gradle.kts`.
2. Commit and push changes to GitHub repository `huqore/nexus.net` using the authenticated remote.
3. Build the release APK (`assembleRelease`).
4. Publish a new GitHub Release with the tag matching `v<versionName>` and attach `app-universal-release.apk`.
