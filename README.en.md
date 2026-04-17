# SchoolRun

An independently maintained Android mock-location, route and NFC utility project for Android 8.0+.

## Current Repository

- Repository: `https://github.com/Acooldog/fuckschoolrun`
- Maintainer: `Acooldog`
- Status: independently maintained

## Features

- Mock location and joystick movement
- Route drawing, local save and import
- Shared route upload and download
- Privacy route simulation with white map masking
- NFC read, simulate, share and download

## Local Configuration

Put local secrets in `local.properties` and do not commit real keys.

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
MAPS_API_KEY=your_baidu_android_ak
MAPS_SAFE_CODE=SHA1;com.acooldog.toolbox
SHARE_API_BASE_URL=http://your-server-host:8080/
```

Important:

- The APK `applicationId` is now `com.acooldog.toolbox`
- If your Baidu Android AK was bound to the old package name, you need to rebind or recreate it
- Update `MAPS_SAFE_CODE` if your signing certificate changes

## Build

```bash
./gradlew :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Output files:

- `SchoolRun_<version>_arm64-v8a_debug.apk`
- `SchoolRun_<version>_arm64-v8a_release.apk`

## Backend Integration

- [docs/shared-backend-api.md](./docs/shared-backend-api.md)
- [docs/map-config.md](./docs/map-config.md)

## Independence Notes

This repository has been separated at the product and distribution level:

- repo-facing docs now point to the current repository
- APK package identity uses the current project application id
- output artifact names use the current project name

The internal Java package layout is still kept as-is for stability. That does not affect APK identity or independent distribution.

## License and Upstream Origin

This repository includes code evolved from an upstream open-source project and continues to comply with:

- `GPL-3.0-only`

If you redistribute or modify this project, keep the required license and attribution notices.
