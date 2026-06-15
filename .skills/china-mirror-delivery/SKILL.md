---
name: china-mirror-delivery
description: Use for this project when downloading tools or dependencies, changing code, or delivering commits; prefer China-hosted mirrors first and push Chinese commits after each completed implementation slice.
license: MIT
---

# China Mirror Delivery

## 下载与仓库源

- 下载 Gradle、Maven 依赖、SDK 组件、二进制工具或外部资源时，优先尝试中国国内镜像源。
- 可配置 URL 时，把国内镜像放在官方源之前；官方源只作为镜像不可用时的兜底。
- 常用优先级：
  - Gradle 分发包：腾讯云 `https://mirrors.cloud.tencent.com/gradle/`，再用华为云 `https://repo.huaweicloud.com/gradle/`，最后官方 `https://services.gradle.org/distributions/`。
  - Maven/Google 依赖：阿里云 `https://maven.aliyun.com/repository/google`、`https://maven.aliyun.com/repository/central`、`https://maven.aliyun.com/repository/public`，再用官方 `google()` / `mavenCentral()`。
  - Android SDK：优先使用可用国内镜像；如果 `sdkmanager` 只能使用官方仓库，先说明原因，再用官方仓库兜底。

## 提交与推送

- 每完成一个可验证的实现切片后，运行必要检查，暂存相关文件，使用中文 commit message 提交，并推送到当前远端分支。
- 不提交未验证的半成品；如果构建或测试失败，先修复，再提交推送。
- 不暂存或回滚与当前任务无关的用户改动。
