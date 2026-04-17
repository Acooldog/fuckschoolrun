<p align="center">
  <img src="./docs/images/牢大.jpg" height="80" alt="Toolbox logo" />
</p>

<div align="center">

[![GitHub stars](https://img.shields.io/github/stars/Acooldog/fuckschoolrun?style=for-the-badge&logo=github)](https://github.com/Acooldog/fuckschoolrun/stargazers)
[![GPLv3 License](https://img.shields.io/badge/license-GPLv3-blue?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0.html)

</div>

# 牢大肘击校园跑

一个独立维护的 Android 虚拟定位、路线模拟与 NFC 工具项目，面向 Android 8.0+。

## 当前能力

- 路线绘制、地点搜索、本地保存、共享上传与下载
- 路线模拟，支持记住上次速度、循环次数和最近路线
- NFC 读取、模拟、共享与下载，支持记住上次 URL 与包名
- 后端管理面板，可修改和删除用户上传的共享数据

## 操作说明

1. 首页先看“操作提示”卡片，按步骤进入功能页。
2. 绘制路线时可以先搜索地点，再点击地图绘制路线。
3. 路线模拟默认值为 `15m/s`、循环 `100`，并会自动记住你上次配置。
4. NFC 工具会自动记住你上次输入的 URL、包名和来源。

## 本地配置

请把本地敏感配置写在 `local.properties`：

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
MAPS_API_KEY=your_baidu_android_ak
MAPS_SAFE_CODE=SHA1;com.acooldog.toolbox
SHARE_API_BASE_URL=http://your-server-host:8080/
```

当前 APK 包名：

```text
com.acooldog.toolbox
```

## 免责声明

本项目仅供合法合规的开发、调试、学习与研究用途。

- 请勿将本项目用于作弊、绕过平台规则、冒充第三方软件或其他违法违规用途。
- 使用者应自行承担使用行为带来的全部后果。
- 维护者不对因不当使用本项目造成的直接或间接损失承担责任。

## 开源协议

本项目继续遵循：

- `GPL-3.0-only`

## 上游说明

本项目已作为独立项目维护，但在 README 中保留来源说明：

- 使用并演化自上游开源项目：`https://github.com/ZCShou/GoGoGo`

## 文档

- 后端接口文档：[docs/shared-backend-api.md](./docs/shared-backend-api.md)
- 地图配置文档：[docs/map-config.md](./docs/map-config.md)
