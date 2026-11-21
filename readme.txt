IoT 平台（iot-platform）
========================

单一整体仓库，包含以下模块：
- Gateway4GCat1/          网关固件（RT-Thread + STM32F4 + 4G Cat1）
- ProxyTcp/               TCP 接入网关（Netty，端口 50001；REST 端口 8010）
- dtm/                    协议解码/编码服务（端口 8082）
- platform_java/          业务平台服务（端口 8081，MongoDB + TDengine）
- auth-service/           Web 用户认证服务（JWT，端口 8083）
- web_vue3/               管理端前端（Vue3 + TS + Element Plus）
- mini_uniapp/            小程序端
- dtm_bootloader 等       配套模块

网关认证机制（协议 5.0 扩展，传输版本 2）
----------------------------------------
详见 docs/protocol-gateway-auth.md。要点：

1. 网关首次联网（含每次重连）必须先用 gateway_id + 预共享密钥认证；
   密钥在网关入库时录入（platform_java 设备管理，或固件控制台
   #ST GATEWAY_SECRET <secret>）。
2. 认证通过后平台签发 10 字节随机 token，写入网关与平台两侧；
   之后所有收发报文在传输层定长段（51 字节，比 v1 多 10 字节 token）携带 token。
3. ProxyTcp 统一校验 token：无效/未认证报文回错误 ACK（NAK）并丢弃、不转发；
   连接断开 token 立即失效，重连必须重新申请（每次重连新 token）。
4. ProxyTcp 将重连记录（gateway_reconnect_record）与 token 申请记录
   （gateway_token_request_record）落库，并在 web 管理端"网关认证记录"页面查询；
   REST 接口：GET /api/gateway/reconnect-records、GET /api/gateway/token-requests。
5. 传输版本 1（旧固件，无 token）不再被接受，网关固件需升级到配套版本。

版本升级注意事项
----------------
- 传输层定长段由 41 字节扩展为 51 字节，protocolVersion(transport) 1 -> 2，
  固件、ProxyTcp 必须同步升级；dtm 只处理 serviceData，无需改动。
- 网关认证命令字：上行 105 (0x69) 认证请求，下行 106 (0x6A) 认证响应，
  仅在 ProxyTcp 终结处理，不进入 dtm/MQ。
