# Mongo → MySQL 数据迁移工具

一次性把 MongoDB 中的系统管理数据与 platform_java 业务数据迁移到 MySQL `iot-platform` 库：

| 源（MongoDB） | 目标（MySQL） |
|---|---|
| `admin_service.orgs` | `sys_org` |
| `admin_service.roles` | `sys_role` |
| `admin_service.menus` | `sys_menu` |
| `auth_service.users` | `sys_user` |
| `iot_platform.devices` | `iot_device` |
| `iot_platform.data_records` | `iot_data_record` |
| `iot_platform.alarms` | `iot_alarm` |

传感器时序读数不迁移，仍在 TDengine。

## 前置条件

1. 已执行 `sql/sys-management-mysql.sql` 与 `sql/platform-mysql.sql`（库表已存在）。
2. JDK 17+、Maven 3.9.9（本机：`C:\Programs\apache-maven-3.9.9\bin\mvn.cmd`）。注意 PATH 中的 `java` 可能是 JDK 8，运行时使用 `C:\Program Files\Java\jdk-17.0.11\bin\java.exe`。
3. MongoDB 中三个源库可访问。

## 构建与运行

```bash
cd scripts/mongo-to-mysql
mvn clean package
java -jar target/mongo-to-mysql.jar --mysql-password=your_password
```

或用环境变量（推荐）：

```powershell
$env:MYSQL_PASSWORD = 'jwmhw5154'
java -jar target/mongo-to-mysql.jar
```

可选项：

```
--mongo-uri URI       默认 mongodb://localhost:27017（env MONGO_URI）
--admin-db NAME       机构/角色/菜单源库（env ADMIN_MONGO_DB, default admin_service）
--auth-db NAME        用户源库（env AUTH_MONGO_DB, default auth_service）
--platform-db NAME    设备/数据记录/告警源库（env PLATFORM_MONGO_DB, default iot_platform）
--mysql-url URL       默认 jdbc:mysql://localhost:3306/iot-platform?...（env MYSQL_URL）
--mysql-user USER     默认 root（env MYSQL_USER）
--mysql-password PWD  默认空（env MYSQL_PASSWORD）
--batch-size N        默认 200
```

## 幂等与保真

- 主键保留原始 `_id`：ObjectId 转 24 位十六进制字符串，保证 org_id、menu_ids、device_id 引用继续有效。
- BCrypt 哈希、`tokenVersion`、原始 id 与时间戳原样迁移；`serviceDataDecoded` 等集合字段序列化为 MySQL JSON 列。
- `INSERT ... ON DUPLICATE KEY UPDATE`：重复执行只更新、不新增；每批提交，失败时回滚并输出失败记录 id。
- 日志只输出迁移计数，不打印密码哈希或连接口令。

## 验证

```sql
SELECT 'sys_org' t, COUNT(*) n FROM `iot-platform`.sys_org UNION ALL
SELECT 'sys_role', COUNT(*) FROM `iot-platform`.sys_role UNION ALL
SELECT 'sys_menu', COUNT(*) FROM `iot-platform`.sys_menu UNION ALL
SELECT 'sys_user', COUNT(*) FROM `iot-platform`.sys_user UNION ALL
SELECT 'iot_device', COUNT(*) FROM `iot-platform`.iot_device UNION ALL
SELECT 'iot_data_record', COUNT(*) FROM `iot-platform`.iot_data_record UNION ALL
SELECT 'iot_alarm', COUNT(*) FROM `iot-platform`.iot_alarm;
```

重复执行工具记录数应保持不变。随后启动各服务，确认登录、菜单、设备与告警接口正常。
