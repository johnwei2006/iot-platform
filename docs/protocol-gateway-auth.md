# 网关认证协议（协议 5.0 扩展，传输版本 2）

## 1. 概述

网关首次联网（含每次断线重连）时，必须先向平台完成认证，取得 10 字节 token 后才能进行业务报文收发。

- 认证凭据：`gateway_id` + 预共享密钥（网关入库时由平台录入）
- token：10 字节二进制随机数，由 ProxyTcp 签发并验证
- 生命周期：token 与 TCP 连接绑定；连接断开即失效，重连后必须重新申请
- 验证位置：ProxyTcp（传输层），认证报文在 ProxyTcp 终结，不进入 dtm / MQ
- 兼容性：传输版本 1（旧固件，无 token）不再被接受

## 2. 传输层定长段扩展（41 → 51 字节）

传输版本 `transport_version` 由 1 升级为 2。定长段在原 41 字节末尾追加 10 字节 token：

| 偏移 | 长度 | 字段 | 说明 |
|---|---|---|---|
| 0 | 11 | gateway_id | ASCII，右补空格 |
| 11 | 11 | node_id | ASCII，网关报文填空格 |
| 22 | 6 | message_create_time | YY MM DD HH MM SS |
| 28 | 4 | message_id | 大端 |
| 32 | 1 | service_type | 命令字 |
| 33 | 1 | service_version | |
| 34 | 1 | service_data_encoding | 1=JSON 2=BINARY |
| 35 | 1 | service_data_protocol_version | |
| 36 | 1 | service_data_encryption | |
| 37 | 4 | event_id | 大端 |
| **41** | **10** | **token** | 认证通过后由平台签发；认证请求报文填全 0 |

外层帧与 CRC 不变：`0xFF 0xEE` + 2 字节小端长度 + 载荷（4 字节头 + 定长段 + serviceData）+ 1 字节和校验（覆盖除末字节外全部字节）。

## 3. 认证命令字

| 值 | 名称 | 方向 | 说明 |
|---|---|---|---|
| 105 (0x69) | GATEWAY_AUTH_REQUEST | 上行 | 网关 → 平台认证请求，在 ProxyTcp 终结 |
| 106 (0x6A) | GATEWAY_AUTH_RESPONSE | 下行 | 平台 → 网关认证响应 |

### 3.1 认证请求（上行）

- `token` 字段：全 0
- `serviceData`：预共享密钥 ASCII 字节（≤32 字节）

### 3.2 认证响应（下行）

- `token` 字段：认证成功时为新签发的 token；失败时全 0
- `serviceData`（11 字节）：`result(1) + token(10)`

| result | 含义 |
|---|---|
| 0 | SUCCESS 认证成功，token 有效 |
| 1 | UNKNOWN_DEVICE 网关未入库 |
| 2 | INVALID_SECRET 密钥错误 |
| 3 | ERROR 平台内部错误 |

## 4. 认证时序

1. 网关 TCP 连接建立 → ProxyTcp 记录连接
2. 网关发送 `GATEWAY_AUTH_REQUEST`（gateway_id + 密钥，token 全 0）
3. ProxyTcp 读取平台库（iot_platform.devices）校验 gateway_id 是否入库、密钥是否匹配
4. 校验通过：作废旧 token → 签发新 10 字节随机 token → 写 token 申请记录 → 回 `GATEWAY_AUTH_RESPONSE`（result=0 + token）
5. 校验失败：写 token 申请记录（含失败原因）→ 回 `GATEWAY_AUTH_RESPONSE`（result=1/2）
6. 认证通过后，网关所有上行报文在定长段携带 token；ProxyTcp 逐报文校验：
   - 通过：正常 ACK 并向 MQ 转发
   - 无效/缺失/未认证：回错误 ACK、丢弃、记日志，不转发
7. 平台下行报文由 ProxyTcp 填充该网关当前有效 token；网关侧对称校验下行 token
8. TCP 断开：token 立即失效；网关重连后从第 2 步重新开始（每次重连新 token）

## 5. 平台记录（ProxyTcp → Mongo proxy 库）

- **gateway_reconnect_record**：deviceId、ip、port、connectTime、type（FIRST 首次 / RECONNECT 重连）
- **gateway_token_request_record**：deviceId、ip、port、requestTime、result、tokenHex（成功时）
- **gateway_token**：deviceId、tokenHex、issuedAt、active（每网关仅一条 active）

提供 REST 查询接口（ProxyTcp 8010 端口）：

- `GET /api/gateway/reconnect-records?deviceId=&page=&size=`
- `GET /api/gateway/token-requests?deviceId=&page=&size=`
- `POST /api/gateway/credentials`（h2 模式下录入网关密钥的运维接口；mongo 模式密钥由 platform_java 的 devices 集合管理）
