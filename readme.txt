IoT 平台（iot-platform）
========================

单一整体仓库，包含以下模块：
- Gateway4GCat1/          网关固件（RT-Thread + STM32F4 + 4G Cat1）
- ProxyTcp/               TCP 接入网关（Netty，端口 50001；REST 端口 8010）
- dtm/                    协议解码/编码服务（端口 8082）
- platform_java/          业务平台服务（端口 8081，MySQL/MyBatis-Plus + TDengine）
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
6. 测试万能 token：ProxyTcp 配置 gateway.auth.master-token（默认 MASTER0001，
   置空关闭），携带它的报文无需认证即可通过；仅限测试环境，生产务必置空。

系统管理数据持久化（MySQL，2026-11 新增）
------------------------------------------
- 机构/角色/菜单按钮在 admin-service，用户在 auth-service，现统一持久化到
  MySQL localhost:3306 / iot-platform（本地默认 root/jwmhw5154，可用
  MYSQL_HOST/PORT/DB/USER/PASSWORD 覆盖）。
- 表结构：sql/sys-management-mysql.sql，表为 sys_org / sys_role / sys_menu / sys_user；
  按钮仍存 sys_menu 中 type=BUTTON 的节点；菜单/角色/用户的集合字段用 JSON 列。
- 历史数据：scripts/mongo-to-mysql 一次性迁移工具，从 admin_service 的
  orgs/roles/menus 与 auth_service 的 users 导入，保留原始 id 与 BCrypt 哈希，
  可重复执行（详见该目录 README.md）。
- platform_java 的设备/数据记录/告警也已迁到 MySQL：sql/platform-mysql.sql 定义
  iot_device / iot_data_record / iot_alarm；传感器读数仍写 TDengine，实时状态仍用 Redis。
- admin-service / auth-service / platform_java 统一用 MyBatis-Plus（3.5.7）访问 MySQL；
  迁移工具同时导入上述七个集合/表，重复执行不新增记录。
- auth-service 的审计日志/SSO/refresh token 仍在 MongoDB；ProxyTcp（JPA + H2）不在本次迁移范围。

构建环境（2026-11 迁移后）
--------------------------
全部 Java 后端模块（auth-service / admin-service / platform_java / dtm / ProxyTcp）
已统一迁移到 JDK 17 + Spring Boot 3.3.5：

- 构建要求：JDK 17+、Maven 3.6.3+（本机使用 C:\Programs\apache-maven-3.9.9\bin\mvn.cmd）。
- 测试框架统一为 JUnit 5（org.junit.jupiter），JWT 库统一为 jjwt 0.12.6 三件套。
- 命名空间迁移：javax.servlet/validation/annotation/persistence -> jakarta.*；
  Redis 连接配置 spring.redis.* -> spring.data.redis.*（platform_java）。
- API 变更要点：RestTemplateBuilder 超时改用 SimpleClientHttpRequestFactory
  （见各模块 RestTemplates 工具类）；Spring AMQP 3 的 setReturnsCallback；
  Spring Data 3 中 PagingAndSortingRepository 不再继承 CrudRepository；
  Mongo 驱动 4.x 用 MongoClients/ConnectionString。
- 五模块回归：297 个测试全部通过（60 + 12 + 131 + 51 + 43）。

版本升级注意事项
----------------
- 传输层定长段由 41 字节扩展为 51 字节，protocolVersion(transport) 1 -> 2，
  固件、ProxyTcp 必须同步升级；dtm 只处理 serviceData，无需改动。
- 网关认证命令字：上行 105 (0x69) 认证请求，下行 106 (0x6A) 认证响应，
  仅在 ProxyTcp 终结处理，不进入 dtm/MQ。
