# auth-service

独立认证中心：RS256 非对称签名 JWT（JWKS）+ Refresh 轮换、自建 SSO（统一登录页 + 中央会话）、微信小程序登录、RBAC、登录限流与审计。

## 技术栈

- Java 17 / Spring Boot 3.3.5（2026-11 由 Java 8 / Boot 2.1.6 迁移，全部 Java 模块统一）
- MySQL 8 + MyBatis-Plus 3.5.7（用户表 `sys_user`：凭证、角色、机构与数据权限；表结构由 `db/changelog` 的 Liquibase 自动维护）
- MongoDB（SSO 客户端、审计）、Redis（refresh token、黑名单、SSO ticket）
- jjwt 0.12.6（RS256/HS256，`Jwts.SIG` 新 API）、spring-security-crypto（BCrypt）
- JUnit 5（`org.junit.jupiter`）

> 构建要求：JDK 17+、Maven 3.6.3+（本机用 `C:\Programs\apache-maven-3.9.9\bin\mvn.cmd`）。

## 启动

必填环境变量：`AUTH_JWT_SECRET`（至少 32 字符）。示例见 `.env.example`。

```bash
# 设置 AUTH_JWT_SECRET 等环境变量后
mvn -f auth-service/pom.xml spring-boot:run
# 或
mvn -f auth-service/pom.xml package
java -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar
```

健康检查：`GET http://localhost:8083/health`

## 模块结构（feature-first）

```
com.iot.auth
├── common/       统一响应、错误码、异常、全局处理
├── config/       AuthProperties（启动校验）
├── health/       健康检查
├── identity/     用户模型、仓储、BCrypt 密码
├── token/        JWT 签发/验证、Refresh、黑名单（逐步实现）
├── sso/          SSO 客户端、Ticket、授权码交换（逐步实现）
├── wechat/       微信 code2session、openid 绑定（逐步实现）
└── internal/     HMAC 服务密钥与签名工具（逐步实现）
```

## 安全约定

- 口令只在 auth-service 以 BCrypt 存储，其他服务不再接触密码。
- `jwt-private-key`（RS256）、`jwt-secret`（HS256 兼容）、微信 AppSecret、服务密钥只从
  环境变量/配置注入，不写日志、不下发前端。
- **非对称签名（推荐）**：配置 `AUTH_JWT_PRIVATE_KEY`（PKCS#8 PEM）后使用 RS256，
  下游经 `GET /.well-known/jwks.json` 获取公钥本地验签；JWT 带 `kid` 头，
  更换私钥并更新 `AUTH_JWT_KEY_ID` 即完成平滑轮换；claims 强校验 `iss`/`aud`。
- Access Token 短有效期（默认 30 分钟），Refresh Token 存 Redis 并可撤销。
- **Refresh Token 轮换**：每次刷新作废旧 refresh 并签发新 token；已轮换 token 再次提交
  视为泄露（重用检测），立即递增 `tokenVersion` 撤销该用户全部会话。
- **verify 全量校验**：`/api/auth/verify` 除验签外，还核对用户状态与 `tokenVersion`
  （Redis 短缓存 60 秒，`revoke-all` 时主动失效）。
- **全局撤销**：`POST /api/auth/revoke-all` 递增 `tokenVersion` 使该用户所有历史
  access/refresh 立即失效；改密、禁用、管理员踢人时调用（带 userId 需 ADMIN 角色）。
- **Refresh Token HttpOnly Cookie**：浏览器端 refresh 不落 localStorage，由
  `auth_refresh` Cookie（HttpOnly + SameSite=Strict，Path=/api/auth）承载；
  生产 HTTPS 必须设置 `AUTH_COOKIE_SECURE=true`；跨域无法携带 Cookie 时可
  `AUTH_COOKIE_ENABLED=false` 退回 body 模式（小程序/原生仍走响应体）。
- **SSO 兑票强校验**：`POST /api/sso/token` 需 `clientSecret`（客户端已配置密钥时
  常量时间比对）且 `redirectUri` 与签票绑定一致，校验失败不消费 ticket。
- **登录限流**：同一用户名/来源 IP 在 15 分钟窗口内失败超 10 次即拒绝（429），
  可通过 `AUTH_RATE_LIMIT_*` 调整。
- **审计**：LOGIN_SUCCESS/LOGIN_FAILED/LOGOUT/REVOKE_ALL/REFRESH_REUSE/SSO_EXCHANGE
  写入 MongoDB `auth_events` 集合（写入失败仅告警不影响主流程）。
- 单点登出/全局失效通过递增用户 `tokenVersion` + Redis 黑名单实现。

## 主要端点

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /sso/login | 统一登录页（auth-service 内置静态页） |
| GET | /sso/logout | 单点登出（销毁中央会话 + 撤销全部令牌），可带 postLogoutRedirectUri |
| POST | /api/auth/login | 账密登录，签发 access + refresh（写 refresh Cookie + 中央会话 Cookie） |
| POST | /api/auth/refresh | 刷新（轮换 refresh）；body 可为空，走 Cookie |
| POST | /api/auth/logout | 登出单个会话：撤销 refresh、拉黑 access、清 Cookie |
| POST | /api/auth/revoke-all | 全局撤销某用户全部会话（递增 tokenVersion） |
| POST | /api/auth/verify | 内部验签（签名/过期/黑名单/用户状态/tokenVersion）+ org/ds 声明 |
| GET | /api/sso/session | 统一登录页探测中央会话 |
| POST | /api/sso/ticket | 签发一次性 ticket（Bearer 或中央会话 Cookie 认证） |
| POST | /api/sso/token | ticket + clientId + clientSecret + redirectUri 换令牌 |
| POST | /api/sso/logout | 单点登出（fetch 版，供前端 JS 调用） |
| POST | /api/auth/wechat/login | 微信小程序 code 换令牌 |

## 内部服务 API（/internal/**，HMAC 鉴权）

供 admin-service 等内部服务调用；`X-Service-Id / X-Timestamp / X-Signature` 三头
HMAC-SHA256 签名（serviceId + \n + timestamp(±5min) + \n + method + \n + path + \n + sha256hex(body)），
密钥经 `auth.service-keys` 注册（admin_service → `AUTH_SERVICE_ADMIN_SECRET`）。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /internal/users | 用户列表 |
| POST | /internal/users | 创建用户（凭证，BCrypt） |
| PUT | /internal/users/{id} | 改名/角色/启停（禁用强制下线） |
| POST | /internal/users/{id}/password | 重置密码（tokenVersion 递增） |
| POST | /internal/users/{id}/identity | 同步机构/数据权限（orgId/orgCode/dataScopes） |

**JWT org/ds 声明**：User 由 admin-service 维护 `orgId/orgCode/dataScopes`（4-2-2-2 机构编码 + 
实体→数据权限映射），登录/刷新签发 JWT 时写入 `org`/`ds` claims；`/api/auth/verify` 同步返回，
业务服务（platform_java DataScopeRegistry）据此本地过滤数据。

## SSO 登录流程（自建，轻量）

1. 客户端跳转 `GET /sso/login?clientId=web&redirectUri=<注册回跳>&redirect=<深层链接>`；
2. 登录页探测中央会话（`auth_sso` Cookie，Redis 8h）：
   - 已登录 → 直接签发 ticket；
   - 未登录 → 展示账密表单，`POST /api/auth/login` 成功后建立中央会话；
3. 登录页携 ticket 跳回 `redirectUri?ticket=xxx&redirect=...`；
4. 客户端回调页 `POST /api/sso/token`（ticket + clientId + clientSecret + redirectUri）换 JWT；
5. 单点登出：任一端调用 `GET /sso/logout` 或 `POST /api/sso/logout`，
   销毁中央会话并递增 tokenVersion，所有客户端的已签发令牌立即失效。
