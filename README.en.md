# SchoolRun

[![GitHub stars](https://img.shields.io/github/stars/Acooldog/fuckschoolrun?style=for-the-badge&logo=github)](https://github.com/Acooldog/fuckschoolrun/stargazers)
[![GitHub downloads](https://img.shields.io/github/downloads/Acooldog/fuckschoolrun/total?style=for-the-badge)](https://github.com/Acooldog/fuckschoolrun/releases)
[![GPLv3 License](https://img.shields.io/badge/license-GPLv3-blue?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0.html)

An independently maintained Android mock-location, route simulation and NFC utility project for Android 8.0+.

## Features

- Route drawing, place search, local save, share upload and download
- Route simulation with remembered speed, loop count and recent route
- NFC read, simulate, share and download with remembered values
- Backend admin panel for shared content management
- Gitee release checking with update log dialog

## Local Configuration

Put local secrets in `local.properties`:

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
MAPS_API_KEY=your_baidu_android_ak
MAPS_SAFE_CODE=SHA1;com.acooldog.toolbox
SHARE_API_BASE_URL=http://your-server-host:8080/
```

Current package name:

```text
com.acooldog.toolbox
```

## Build Guide

- [docs/build-guide.md](./docs/build-guide.md)

## Disclaimer

This project is provided only for lawful development, debugging, learning and research.

- Do not use it for cheating, bypassing platform rules, impersonating third-party apps or any illegal purpose.
- Users are solely responsible for the consequences of their own usage.
- Maintainers are not liable for direct or indirect loss caused by misuse.

## License

- `GPL-3.0-only`

## Upstream Notice

This project is maintained independently, while still acknowledging upstream origin:

- Based on and evolved from: `https://github.com/ZCShou/GoGoGo`
