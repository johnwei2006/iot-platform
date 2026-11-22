# admin-service

独立系统管理服务（端口 8084）：多级机构、菜单/按钮、角色、用户管理与按业务实体的数据权限。

> 构建要求：JDK 17+、Maven 3.6.3+。技术栈：Java 17 / Spring Boot 3.3.5 / MyBatis-Plus 3.5.7 + MySQL 8 / jjwt 0.12.6 / JUnit 5（2026-11 由 Java 8 / Boot 2.1.6 迁移，JPA 已切换为 MyBatis-Plus）。
> 机构/角色/菜单按钮持久化在 MySQL `iot-platform` 库的 `sys_org`/`sys_role`/`sys_menu`；用户仍由 auth-service 通过 HMAC 内部 API 管理，存 `sys_user`。表结构由共享 Liquibase changelog（`db/changelog`）在启动时自动维护，`sql/sys-management-mysql.sql` 仅作参考。

## 架构定位

- **auth-service（8083）**：身份凭证与令牌签发中心（BCrypt 密码、JWT RS256/JWKS、SSO、tokenVersion）。
- **admin-service（8084）**：组织与权限管理中心（机构/菜单/按钮/角色/数据权限配置），通过 HMAC 内部 API 代理用户管理。
- **platform_java（8081）**：业务服务，按 JWT 中的 `org`/`ds` 声明本地过滤数据。

## 机构编码（4-2-2-2）

一级 4 位数字，逐级 +2 位（6/8/10 位），最深 4 级；子编码以父编码为前缀，天然表达层级：

```
0001（总部）
├── 000101（研发部）
│   └── 00010103（平台组）
└── 000102（运营部）
```

层级查询用编码前缀匹配（`orgCode startsWith user.org`），免递归查树。

## 数据权限模型（简化四级）

每业务实体独立配置，多角色合并时**每实体取最宽范围**；未配置的已知实体默认 SELF：

| 范围 | 语义 |
|---|---|
| ALL | 全部数据 |
| ORG_AND_CHILDREN | 本机构及以下（编码前缀匹配） |
| ORG_ONLY | 仅本机构（编码等值） |
| SELF | 仅本人（ownerUserId 等值） |

**扩展新业务实体**：`admin.entities` 配置注册（admin-service）+ 业务服务实现 `DataScopeRule` bean 注册（platform_java）即可。

## 主要端点（除 /api/admin/me 外仅 SUPER_ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/admin/me | 当前管理员上下文：动态菜单树/权限点/数据权限 |
| GET/POST/PUT/DELETE | /api/admin/orgs[...] | 机构 CRUD + /tree 层级树；删除前校验无子机构且无用户关联 |
| GET/POST/PUT/DELETE | /api/admin/menus[...] | 菜单/按钮树 CRUD |
| GET/POST/PUT/DELETE | /api/admin/roles[...] | 角色 CRUD + /entities 实体注册表查询 |
| GET/POST | /api/admin/users[...] | 用户列表/创建（代理 auth-service 内部 API） |
| PUT | /api/admin/users/{id} | 改名/启停（禁用强制下线） |
| POST | /api/admin/users/{id}/password | 重置密码（tokenVersion 递增，全端下线） |
| POST | /api/admin/users/{id}/assign | 分配机构/角色（合并 dataScopes 并同步 auth-service） |

## 鉴权整合

- **本地验签**：从 auth-service `/.well-known/jwks.json` 拉取 RS256 公钥（kid 平滑轮换）+ Redis 撤销检查（`auth:` 前缀共享）；公钥未就绪回退远程 `/api/auth/verify`。
- **内部 HMAC**：与 auth-service `/internal/users/**` 通信，签名
  `X-Signature = HMAC-SHA256(secret, serviceId \n timestamp \n method \n path \n sha256hex(body))`；
  密钥 `AUTH_SERVICE_ADMIN_SECRET` 两侧必须一致（启动 fail-fast 校验）。
- **RBAC**：管理接口标注 `@RequireRoles({"SUPER_ADMIN"})`，`AdminRoleInterceptor` 拦截。

## 启动初始化（幂等）

根机构（`ADMIN_ROOT_ORG_CODE`，默认 0001 总部）、SUPER_ADMIN 内置角色（全部菜单 + device:ALL）、
系统管理菜单树（机构/用户/角色/菜单四页 + 按钮权限点）、初始管理员 `admin`（经内部 API 创建，
密码 `ADMIN_BOOTSTRAP_PASSWORD`，首次登录后请立即修改）。

## 前端接入

web_vue3 通过 `/admin-api` 代理（生产同域反代）访问；登录后拉取 `/api/admin/me` 生成动态路由，
按钮用 `v-permission` 指令控制，系统管理四页面位于 `src/views/system/{org,user,role,menu}`。
