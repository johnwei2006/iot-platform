# Mongo → MySQL 系统管理数据迁移工具

一次性把 MongoDB 中的系统管理数据迁移到 MySQL `iot-platform` 库：

| 源（MongoDB） | 目标（MySQL） |
|---|---|
| `admin_service.orgs` | `sys_org` |
| `admin_service.roles` | `sys_role` |
| `admin_service.menus` | `sys_menu` |
| `auth_service.users` | `sys_user` |

## 前置条件

1. MySQL 已创建 `iot-platform` 库并执行 `sql/sys-management-mysql.sql`（应用 `ddl-auto=update` 也可自动建表）。
2. JDK 17+、Maven 3.9.9（本机：`C:\Programs\apache-maven-3.9.9\bin\mvn.cmd`）。注意 PATH 中的 `java` 可能是 JDK 8，运行时请使用 `C:\Program Files\Java\jdk-17.0.11\bin\java.exe`。
3. MongoDB 中两个源库可访问。

## 构建与运行

```bash
cd scripts/mongo-to-mysql
mvn clean package
java -jar target/mongo-to-mysql.jar --mysql-password=your_password
```

或使用环境变量（推荐，避免口令出现在命令行历史）：

```powershell
$env:MYSQL_PASSWORD = 'jwmhw5154'
java -jar target/mongo-to-mysql.jar
```

所有可选项：

```
--mongo-uri URI       默认 mongodb://localhost:27017（env MONGO_URI）
--admin-db NAME       默认 admin_service（env ADMIN_MONGO_DB）
--auth-db NAME        默认 auth_service（env AUTH_MONGO_DB）
--mysql-url URL       默认 jdbc:mysql://localhost:3306/iot-platform?...（env MYSQL_URL）
--mysql-user USER     默认 root（env MYSQL_USER）
--mysql-password PWD  默认空（env MYSQL_PASSWORD）
--batch-size N        默认 200
```

## 幂等与保真

- 主键保留原始 `_id`：ObjectId 转 24 位十六进制字符串；字符串 id 原样保留，保证 `org_id`、`menu_ids` 引用不失效。
- BCrypt 密码哈希、`tokenVersion` 原样迁移。
- `roles`/`menuIds`/`dataScopes` 序列化为 MySQL JSON 列。
- `INSERT ... ON DUPLICATE KEY UPDATE`：重复执行只更新、不新增；每批提交，失败时输出失败记录 id 并回滚该批。
- 日志只输出迁移计数，不打印密码哈希或连接口令。

## 验证

```sql
SELECT 'sys_org' t, COUNT(*) n FROM `iot-platform`.sys_org UNION ALL
SELECT 'sys_role', COUNT(*) FROM `iot-platform`.sys_role UNION ALL
SELECT 'sys_menu', COUNT(*) FROM `iot-platform`.sys_menu UNION ALL
SELECT 'sys_user', COUNT(*) FROM `iot-platform`.sys_user;
```

再执行一次迁移工具，记录数应保持不变。随后启动 auth-service 与 admin-service，用原管理员账号登录并确认菜单/机构可见。
