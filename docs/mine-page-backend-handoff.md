# “我的”页后端交接提示词

请为 `fuckschoolrun` 客户端提供“我的”页聚合接口，前端已经预留异步加载、加载中转圈、成功展示和失败 fallback 状态。后端只需要实现数据接口，不要改动前端页面结构。

## 接口一：我的页聚合数据

建议接口：

```http
GET /api/mine/overview
Authorization: Bearer <token，可选>
X-Client-Variant: fuckschoolrun
X-App-Version: <客户端版本>
```

无 token 时允许返回公开联系人信息，但统计字段必须返回 `null` 或明确的登录限制状态；客户端会把今日模拟和累计模拟显示为“登录可见”。

建议响应：

```json
{
  "account": {
    "loggedIn": true,
    "displayName": "example"
  },
  "simulationStats": {
    "todayKilometers": 12.34,
    "totalKilometers": 456.78
  },
  "preferredRoute": {
    "name": "校园环线",
    "points": [
      { "latitude": 31.2304, "longitude": 121.4737 },
      { "latitude": 31.2310, "longitude": 121.4752 }
    ]
  },
  "routeSharing": {
    "uploadedRouteCount": 12,
    "helpedUserCount": 86
  },
  "authorContact": {
    "qq": "2622138410",
    "qqGroup": "951300122",
    "bilibiliName": "牢大_科比",
    "bilibiliUrl": "https://space.bilibili.com/1492911803"
  },
  "disclaimerText": "本软件为免费开源软件。如果是购买软件，或者花钱买教程才得到的软件，请立即退款；如果得到了软件但是对方要求你付费，请勿付费。\n作者qq: 2622138410 作者bilibili: 牢大_科比 作者github名字: Acooldog"
}
```

约定：

- `todayKilometers`、`totalKilometers` 使用公里，保留后端真实精度，前端只负责格式化。
- `preferredRoute.points` 使用 WGS84 经纬度，按路线顺序返回；少于两个点时前端显示“暂无偏好路线”。
- 公开联系人字段缺失时，客户端使用默认值：QQ `2622138410`、QQ群 `951300122`、哔哩哔哩 `牢大_科比`、链接 `https://space.bilibili.com/1492911803`。
- `disclaimerText` 为空时，客户端使用页面内置的免费开源声明。
- 后端失败、超时或网络不可用时，客户端不应阻塞页面，联系人使用默认值，统计和路线显示“暂无数据/暂无偏好路线”。

## 接口二：模拟完成事件批量上传

今日模拟统计依赖模拟完成事件。客户端会在路线模拟完成后异步排队，不能阻塞模拟 UI；后端建议提供：

```http
POST /api/mine/simulation-events/batch
Authorization: Bearer <token>
Content-Type: application/json
```

请求示例：

```json
{
  "events": [
    {
      "idempotencyKey": "device-or-client-generated-uuid",
      "routeId": "local-or-shared-route-id",
      "distanceKilometers": 3.42,
      "completedAt": "2026-10-14T08:30:00Z",
      "source": "route_simulation"
    }
  ]
}
```

要求：

- 支持重复请求幂等，按 `idempotencyKey` 去重。
- 成功返回每条事件的接受结果和累计统计刷新标记。
- 建议返回 HTTP `202 Accepted`，客户端可以继续处理本地队列。
- 服务端按用户和日期聚合今日公里数，按用户聚合累计公里数。
- 未登录事件不应写入账号统计；客户端未登录时只展示“登录可见”。

## 接口三：路线共享统计

如果不希望把路线共享统计放在聚合接口中，也可以提供：

```http
GET /api/mine/route-sharing
Authorization: Bearer <token>
```

返回：

```json
{
  "uploadedRouteCount": 12,
  "helpedUserCount": 86
}
```

`helpedUserCount` 的含义是下载过该用户公开路线的去重用户数，不是下载次数。上传路线完成后应异步更新统计，不能让上传请求阻塞路线模拟或页面操作。

## 验收要求

1. 接口失败时返回可读错误，但不能泄露数据库、堆栈或内部 token。
2. 所有统计接口都必须校验 Bearer token 对应的用户权限。
3. 联系方式和底部声明允许公开读取，但应支持后台配置。
4. 聚合接口字段缺失时返回稳定的空值，不要改变字段类型。
5. 给出接口版本、字段说明、错误码和一份成功/未登录/空数据响应样例。
