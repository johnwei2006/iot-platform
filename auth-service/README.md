# auth-service

独立认证中心：标准 JWT + Refresh、自建 SSO、微信小程序登录、RBAC、内部服务 HMAC 密钥下发/校验。

## 技术栈

- Java 8 / Spring Boot 2.1.6（与 platform-java / dtm / ProxyTcp 一致）
- MongoDB（用户、SSO 客户端、审计）、Redis（refresh token、黑名单、SSO ticket）
- jjwt 0.9.1（HS256 标准 JWT）、spring-security-crypto（BCrypt）

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
- `jwt-secret`、微信 AppSecret、服务密钥只从环境变量/配置注入，不写日志、不下发前端。
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
- 单点登出/全局失效通过递增用户 `tokenVersion` + Redis 黑名单实现。

## 主要端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/auth/login | 账密登录，签发 access + refresh（并写 Cookie） |
| POST | /api/auth/refresh | 刷新（轮换 refresh）；body 可为空，走 Cookie |
| POST | /api/auth/logout | 登出：撤销 refresh、拉黑 access、清 Cookie |
| POST | /api/auth/revoke-all | 全局撤销某用户全部会话（递增 tokenVersion） |
| POST | /api/auth/verify | 内部验签（签名/过期/黑名单/用户状态/tokenVersion） |
| POST | /api/sso/ticket | 已登录会话下签发一次性 SSO ticket |
| POST | /api/sso/token | ticket + clientId + clientSecret + redirectUri 换令牌 |
| POST | /api/auth/wechat/login | 微信小程序 code 换令牌 |
