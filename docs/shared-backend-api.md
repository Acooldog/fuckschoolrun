# 共享路线 / 共享 NFC 后端对接文档

## 1. 客户端配置

Android 客户端通过 `SHARE_API_BASE_URL` 读取后端地址。

在 `local.properties` 或环境变量中配置:

```properties
SHARE_API_BASE_URL=http://your-server-host:8080/
```

客户端当前固定请求以下接口:

- `GET /api/shared/routes`
- `POST /api/shared/routes`
- `GET /api/shared/routes/{id}`
- `GET /api/shared/nfc`
- `POST /api/shared/nfc`

## 2. 返回包裹格式

客户端兼容以下几种返回形式，后端推荐使用第一种。

对象:

```json
{
  "data": {
    "id": "route_001",
    "name": "操场夜跑",
    "privacyMode": true,
    "createdAt": 1760000000000,
    "points": []
  }
}
```

或直接返回对象:

```json
{
  "id": "route_001",
  "name": "操场夜跑",
  "privacyMode": true,
  "createdAt": 1760000000000,
  "points": []
}
```

列表:

```json
{
  "items": []
}
```

或:

```json
{
  "data": {
    "items": []
  }
}
```

也支持直接返回数组。

## 3. 共享路线

### 3.1 上传共享路线

`POST /api/shared/routes`

请求体:

```json
{
  "name": "操场夜跑",
  "privacyMode": true,
  "points": [
    {
      "bdLongitude": 116.404,
      "bdLatitude": 39.915,
      "wgsLongitude": 116.397,
      "wgsLatitude": 39.908,
      "altitude": 55.0
    }
  ]
}
```

字段说明:

- `name`: 共享路线名称，必填。
- `privacyMode`: 是否隐私路线，必填。
- `points`: 路线点数组，至少 2 个点。
- `bdLongitude` / `bdLatitude`: 百度坐标，客户端绘图使用。
- `wgsLongitude` / `wgsLatitude`: WGS84 坐标，客户端模拟定位使用。
- `altitude`: 海拔，客户端默认 55。

推荐响应:

```json
{
  "data": {
    "id": "route_001",
    "name": "操场夜跑",
    "privacyMode": true,
    "createdAt": 1760000000000,
    "points": [
      {
        "bdLongitude": 116.404,
        "bdLatitude": 39.915,
        "wgsLongitude": 116.397,
        "wgsLatitude": 39.908,
        "altitude": 55.0
      }
    ]
  }
}
```

### 3.2 获取共享路线列表

`GET /api/shared/routes`

推荐响应:

```json
{
  "items": [
    {
      "id": "route_001",
      "name": "操场夜跑",
      "privacyMode": true,
      "pointCount": 128,
      "createdAt": 1760000000000
    }
  ]
}
```

兼容字段:

- `id` 也可返回为 `shareId` 或 `routeId`
- `privacyMode` 也可返回为 `privacy`
- `pointCount` 也可返回为 `pointsCount`
- 如果列表项里直接包含 `points`，客户端也能从中推断点数

### 3.3 下载共享路线详情

`GET /api/shared/routes/{id}`

推荐响应:

```json
{
  "data": {
    "id": "route_001",
    "name": "操场夜跑",
    "privacyMode": true,
    "createdAt": 1760000000000,
    "points": [
      {
        "bdLongitude": 116.404,
        "bdLatitude": 39.915,
        "wgsLongitude": 116.397,
        "wgsLatitude": 39.908,
        "altitude": 55.0
      }
    ]
  }
}
```

### 3.4 隐私路线规则

如果 `privacyMode=true`:

- 上传者本地保存的路线仍可正常查看地图。
- 其他用户下载该路线后，客户端会把该路线标记为“共享下载 + 隐私模式”。
- 在路线模拟页面中:
  - 地图区域显示为白色遮罩。
  - 文案显示“分享者已开启隐私模式，地图内容已隐藏，但仍可正常模拟路线”。
  - 模拟定位逻辑不受影响，仍正常推送定位。

后端不需要额外做白色地图处理，只需要正确返回 `privacyMode=true`。

## 4. 共享 NFC

### 4.1 上传共享 NFC

`POST /api/shared/nfc`

请求体:

```json
{
  "name": "校园门禁",
  "url": "https://example.com/nfc/entry",
  "packageName": "com.example.app",
  "source": "manual"
}
```

字段说明:

- `name`: 共享 NFC 名称，必填。
- `url`: 跳转 URL，必填。
- `packageName`: 目标应用包名，必填。
- `source`: 来源标记，推荐保留。客户端可能传 `manual`、`tag`、`shared` 等。

推荐响应:

```json
{
  "data": {
    "id": "nfc_001",
    "name": "校园门禁",
    "url": "https://example.com/nfc/entry",
    "packageName": "com.example.app",
    "source": "manual",
    "createdAt": 1760000000000
  }
}
```

### 4.2 获取共享 NFC 列表

`GET /api/shared/nfc`

推荐响应:

```json
{
  "items": [
    {
      "id": "nfc_001",
      "name": "校园门禁",
      "url": "https://example.com/nfc/entry",
      "packageName": "com.example.app",
      "source": "manual",
      "createdAt": 1760000000000
    }
  ]
}
```

兼容字段:

- `id` 也可返回为 `shareId` 或 `nfcId`
- `url` 也可返回为 `uri` 或 `link`
- `packageName` 也可返回为 `package` 或 `packageId`
- `createdAt` 也可返回为 `createTime`

客户端下载共享 NFC 后，会直接把 `url` 和 `packageName` 填回 NFC 工具页，用户可继续复制、编辑或模拟。

## 5. 错误响应建议

推荐 HTTP 状态码:

- `200/201`: 成功
- `400`: 参数错误
- `404`: 资源不存在
- `500`: 服务内部错误

客户端会直接显示错误响应文本，因此建议错误体尽量简洁，例如:

```json
{
  "message": "route not found"
}
```

或纯文本:

```text
route not found
```

## 6. 存储建议

路线表建议字段:

- `id`
- `name`
- `privacy_mode`
- `points_json`
- `point_count`
- `created_at`
- `updated_at`

NFC 表建议字段:

- `id`
- `name`
- `url`
- `package_name`
- `source`
- `created_at`
- `updated_at`

## 7. 联调检查清单

- `SHARE_API_BASE_URL` 已正确配置到 Android 本地环境。
- `GET /api/shared/routes` 能返回列表。
- `POST /api/shared/routes` 能返回包含 `id` 的对象。
- `GET /api/shared/routes/{id}` 能返回完整 `points`。
- `GET /api/shared/nfc` 能返回 `url` 和 `packageName`。
- `POST /api/shared/nfc` 能保存并返回新记录。
- 隐私路线返回 `privacyMode=true` 后，客户端下载并模拟时能显示白色遮罩。
