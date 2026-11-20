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
- 单点登出通过递增用户 `tokenVersion` + Redis 黑名单实现。
