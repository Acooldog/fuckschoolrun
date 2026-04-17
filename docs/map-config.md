# Map Configuration

This project uses the Baidu Android Map SDK. A working APK needs a Baidu Android AK that matches the APK package name and signing certificate SHA1.

The default package name is:

```text
com.zcshou.gogogo
```

Do not commit real API keys or signing keys. Put local values in `local.properties`:

```properties
MAPS_API_KEY=your_baidu_android_ak
MAPS_SAFE_CODE=SHA1;com.zcshou.gogogo
```

If `MAPS_API_KEY` is empty or does not match the package/SHA1 binding in Baidu LBS, the map view can render as a blank white grid and log `baidumapsdk: Authentication Error`.
