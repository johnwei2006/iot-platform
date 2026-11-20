# dtm — IoT 数据传输服务

数据传输服务（Data Transfer Module），位于 `ProxyTcp` 与 `platform_java` 之间，负责**业务层协议编解码**与上下行消息的可靠转发、削峰、解耦。

`ProxyTcp` 处理传输层（TCP 帧拆包），dtm 处理业务层（37 个 serviceType 的二进制 `serviceData` ↔ 结构化字段），`platform_java` 做业务判定。

> 设计基线见文档仓 [`12_design/dtm/design.md`](https://github.com/johnwei2006/iot-platform-docs/blob/main/12_design/dtm/design.md) 与 [`12_design/api-design.md`](https://github.com/johnwei2006/iot-platform-docs/blob/main/12_design/api-design.md)。

## 职责

- **上行解码转发**：消费 `proxy.uplink` → 按 serviceType 把 base64 `serviceData` 解成结构化字段 → 追加字段后发 `platform.uplink`。
- **下行编码转发**：消费 `platform.downlink` → 把结构化字段编码回二进制 → 回填 base64 `serviceData` 后发 `proxy.downlink`。
- **协议清单**：`src/main/resources/protocol-schemas.json` 声明 37 条平台-网关协议；启动时由 `ProtocolRegistry` 与 `ServiceType` 枚举一一校验，`TransportEncoderManager` 强制 37 个 Encoder 全覆盖。
- **可靠性**：手动 ACK + publisher confirm。worker 处理并发布成功、broker confirm ACK 后才 ACK 上游；失败 NACK 进死信 `iot.dlq`，超时 requeue，至少一次投递。
- **解耦**：接入层与业务层故障互不致命；`dtm.protocol.enabled=false` 可一键回退为纯字节透传。

## 技术栈

- Spring Boot 2.1.6（与 `ProxyTcp` 对齐，Java 8）
- Spring AMQP（RabbitMQ）
- Actuator（健康检查）

## 处理链路（按架构图）

```text
上行：proxy.uplink -> UplinkConsumer -> dtmWorkerExecutor -> UplinkService
      -> DeviceProfileCache + TransportEncoderManager(37 个 XxxEncoder) -> platform.uplink

下行：platform.downlink -> DownlinkConsumer -> dtmWorkerExecutor -> DownlinkService
      -> DeviceProfileCache + TransportEncoderManager(37 个 XxxEncoder) -> proxy.downlink
```

| 组件 | 职责 |
| --- | --- |
| `mq/UplinkConsumer` / `mq/DownlinkConsumer` | 监听、提交线程池、`Future.get()` 等待、手动 ACK/NACK |
| `service/UplinkService` / `service/DownlinkService` | DTO 解析、DeviceProfile 合并、调用编码器、发布并等待 confirm |
| `protocol/encoder/TransportEncoderManager` | 按 serviceType 选 Encoder，校验 encoding/协议版本/加密 |
| `protocol/encoder/impl/*Encoder` | 37 个 serviceType 各一个薄封装，字段定义统一来自 schema |
| `profile/DeviceProfileCache` | 按 deviceId 缓存最近一次 Profile，缺省字段复用，容量上限 |
| `mq/MessagePublisher` | `CorrelationData` + publisher confirm，confirm 成功才返回 |

线程池每个报文独立 worker，**不保证同设备顺序**；有界队列 + `CallerRunsPolicy` 提供背压。消费者线程会等待 worker 完成后再 ACK，因此实际并发 ≈ `spring.rabbitmq.listener.simple.concurrency`（默认 8），可通过 `DTM_CONCURRENCY` 调大；worker 池上限由 `dtm.worker.max-size` 控制。

## 消息契约（RabbitMQ）

| 队列 | 方向 | 消息体 |
| --- | --- | --- |
| `proxy.uplink` | 网关 → dtm | 标准 `ServiceRequest` JSON，`serviceData` 为设备二进制的 base64 |
| `platform.uplink` | dtm → 后台 | `ServiceRequest` 原字段 **+** `serviceDataDecoded` / `protocolName` / `transactionType` / `decodeStatus`（失败时含 `decodeError`） |
| `platform.downlink` | 后台 → dtm | `DownlinkMessage` JSON；可带 `serviceDataDecoded`（结构化）或仅 `serviceData`（旧 base64 通道） |
| `proxy.downlink` | dtm → 网关 | **标准 `ServiceRequest`**，附加字段全部剥离，`serviceData` 为编码后的 base64 |
| `iot.dlq` | 死信 | 下行编码/信封失败、`fail-open=false` 的上行失败、worker 异常；超时 requeue |

交换器：`iot.exchange`（topic）；死信交换器：`iot.dlx`。

上行结构化字段示例（原 `serviceData` 保持不变，后台可继续按旧 DTO 消费）：

```json
{
  "deviceId": "30410007102",
  "serviceType": 101,
  "serviceData": "AQI=",
  "serviceDataDecoded": { "appType": 1, "action": 9 },
  "protocolName": "GATEWAY_REBOOT",
  "transactionType": "downlink",
  "decodeStatus": "OK"
}
```

`decodeStatus` 语义：`OK`（解码成功）、`EMPTY`（协议本身空载荷）、`UNSUPPORTED`（未知类型 / 0x60 无 schema / 加密 / 版本或编码不支持）、`FAILED`（截断、非法 base64、超长、字段缺失）。
上行后两者 `serviceDataDecoded` 为 `null`，原报文仍转发（`fail-open=true`），并在 AMQP header 上打 `decodeStatus=FAILED` 与错误原因；`fail-open=false` 时直接 NACK 进 DLQ。
下行编码/信封失败**始终 NACK 进 DLQ**，避免把无效指令发给网关。

回调事务（上行回调 / 下行回调）的应答由 `platform_java` 生成，dtm 只输出 `transactionType` 元信息，不自动回包。

协议字段与端序核对：[docs/protocol-mapping.md](docs/protocol-mapping.md)；三端联调：[docs/local-integration.md](docs/local-integration.md)。

## 构建与运行

```bash
mvn clean package
java -jar target/dtm-0.0.1-SNAPSHOT.jar
# 或开发运行
mvn spring-boot:run
```

- 端口：`8082`，健康检查：`GET /health`、`/actuator/health`。
- RabbitMQ 连接可通过环境变量覆盖：`RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST`（默认 `/`）。
- 编解码开关：`dtm.protocol.enabled` / `dtm.protocol.fail-open` / `dtm.protocol.max-payload-bytes` / `dtm.protocol.max-repeat-items`，均可用 `DTM_PROTOCOL_*` 环境变量或启动参数覆盖。
- 线程池与确认：`dtm.worker.core-size/max-size/queue-capacity/keep-alive-seconds/await-timeout-ms/confirm-timeout-ms/profile-cache-max`，可用 `DTM_WORKER_*` / `DTM_CONFIRM_TIMEOUT_MS` / `DTM_PROFILE_CACHE_MAX` 覆盖。
- 监听器：`acknowledge-mode=manual`、`prefetch`/`concurrency`/`max-concurrency` 可配，`retry.enabled=false`（失败由 Consumer 显式 nack）。

## 可观测

- 日志：`com.iot.dtm` 每条上行打印 `deviceId / serviceType / protocolName / decodeStatus / 字节数`；结构化内容走 DEBUG；未知 serviceType 的 WARN 按类型抑制，避免刷屏。
- 线程与 ACK：消费者记录 worker 失败原因、超时与 nack 决策；`MessagePublisher` DEBUG 记录 confirm 延迟与字节数。
- 状态标记：见消息契约一节的 `decodeStatus` / `encodeStatus`。
- 死信：监控 `iot.dlq` 积压以发现基础设施异常；超时消息会 requeue，注意重复投递需业务幂等。
