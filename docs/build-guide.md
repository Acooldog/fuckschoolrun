# 构建与打包说明

## 1. 编译 Debug

在项目根目录执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

输出文件：

```text
app\build\outputs\apk\debug\SchoolRun_v1.0.0_arm64-v8a_debug.apk
```

如果想只校验 Java 编译是否通过：

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

## 2. 编译发行版

在项目根目录执行：

```powershell
.\gradlew.bat :app:assembleRelease
```

输出文件：

```text
app\build\outputs\apk\release\SchoolRun_v1.0.0_arm64-v8a_release.apk
```

如果存在签名文件，Gradle 会优先使用：

- `keystore/SchoolRun.jks`
- 若不存在，则回退到 `keystore/GoGoGo.jks`

## 3. 如何改包名

当前包名是：

```text
com.acooldog.toolbox
```

### 3.1 修改 Gradle 中的 applicationId

文件：

- `app/build.gradle`

修改这一行：

```groovy
applicationId "com.acooldog.toolbox"
```

### 3.2 修改 namespace

同样在：

- `app/build.gradle`

修改这一行：

```groovy
namespace 'com.acooldog.toolbox'
```

### 3.3 修改百度地图安全码

文件：

- `local.properties`
- `local.properties.example`

修改：

```properties
MAPS_SAFE_CODE=SHA1;com.acooldog.toolbox
```

如果你更换了包名或签名证书，必须同步去百度地图 / 百度 LBS 后台重新绑定 Android AK。

### 3.4 重新编译

包名改完后建议先执行：

```powershell
.\gradlew.bat clean :app:assembleDebug
```

如果要出正式版：

```powershell
.\gradlew.bat clean :app:assembleRelease
```

## 4. 说明

- 当前 Android 内部 Java 命名空间已经迁移到 `com.acooldog.toolbox`
- 若要安装到模拟器，可用 `adb install -r <apk路径>`
