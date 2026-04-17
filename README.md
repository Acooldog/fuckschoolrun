<p align="center">
  <img src="./docs/images/LOGO.png" height="80" alt="SchoolRun logo" />
</p>

# 牢大肘击校园跑

一个独立维护的 Android 虚拟定位与路线/NFC 工具项目，面向 Android 8.0+。

## 项目状态

- 当前仓库: `https://github.com/Acooldog/fuckschoolrun`
- 当前维护者: `Acooldog`
- 当前定位: 独立维护，不再跟随上游项目的发布节奏

## 当前能力

- 虚拟定位与摇杆移动
- 路线绘制、本地保存、导入
- 共享路线上传与下载
- 隐私路线白色地图模拟遮罩
- NFC 读取、模拟、共享与下载

## 本地配置

请把本地敏感配置写在 `local.properties`，不要提交真实密钥。

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
MAPS_API_KEY=your_baidu_android_ak
MAPS_SAFE_CODE=SHA1;com.acooldog.toolbox
SHARE_API_BASE_URL=http://your-server-host:8080/
```

注意：

- 当前 APK 包名已经切到 `com.acooldog.toolbox`
- 如果你之前的百度地图 Android AK 绑定的是旧包名，需要重新绑定或重新申请
- 如果你使用自定义签名，`MAPS_SAFE_CODE` 里的 `SHA1;包名` 也要同步更新

## 构建

```bash
./gradlew :app:assembleDebug
```

Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

默认输出文件名:

- `SchoolRun_<version>_arm64-v8a_debug.apk`
- `SchoolRun_<version>_arm64-v8a_release.apk`

## 后端对接

共享路线和共享 NFC 的接口文档见:

- [docs/shared-backend-api.md](./docs/shared-backend-api.md)

地图 AK 与包名绑定说明见:

- [docs/map-config.md](./docs/map-config.md)

## 独立维护说明

这个仓库已经按当前项目身份独立维护，包括：

- 仓库文档与对外链接切换到当前仓库
- 安装包 `applicationId` 切换到当前项目
- 构建产物命名切换到当前项目
- 设置页中的项目链接切换为当前维护地址

为避免一次性大范围重构带来不必要风险，当前 Java 源码内部包路径仍保留历史结构；这不会影响 APK 的独立发布与安装包身份。

## 开源与来源

本仓库包含基于上游开源项目继续演化的代码，当前继续遵循原许可证：

- `GPL-3.0-only`

如果你继续分发、修改或再发布本项目，请一并遵守 GPL 许可证要求，并保留必要的来源与许可证信息。
