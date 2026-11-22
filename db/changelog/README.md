# Liquibase changelog（共享）

`admin-service`、`auth-service`、`platform_java` 共用 MySQL 库 `iot-platform` 与同一份 changelog：

```
db/changelog/db.changelog-master.yaml
```

三个服务通过 Maven resources 配置将本目录复制到各自 classpath 的 `db/changelog`，因此 `DATABASECHANGELOG` 中的 filename / id / author 完全一致；先启动的服务执行变更，其余服务自动跳过。并发启动由 `DATABASECHANGELOGLOCK` 串行化。

## 规范

- 基线 changeset 使用 `CREATE TABLE IF NOT EXISTS`，对已有表和数据无破坏性。
- **禁止修改已执行的 changeset**；结构变更一律追加新 changeset，并更新 master changelog 的 include。
- SQL 中不写 `CREATE DATABASE` / `USE`，连接哪个库由服务 `spring.datasource` 决定（库名 `iot-platform` 含连字符，SQL 中用反引号）。
- author 固定 `iot-platform`，id 采用 `YYYYMMDD-NN-描述` 格式。
- 传感器时序数据（TDengine）和 Redis 实时状态不由 Liquibase 管理；ProxyTcp 的 H2 也未接入。

## 运行方式

服务启动时由 Spring Boot 自动执行，配置项：

```yaml
spring:
  liquibase:
    enabled: ${LIQUIBASE_ENABLED:true}
    change-log: classpath:db/changelog/db.changelog-master.yaml
    drop-first: false
```

本地可用 `LIQUIBASE_ENABLED=false` 临时关闭。
