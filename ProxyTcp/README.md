# ProxyTcp

物联网设备 **TCP 网关接入层**：负责设备 TCP 接入、上下行协议编解码、与消息中间件对接，将设备数据上送后台（platform_java，经 dtm），并把后台下发的指令写回设备。

> 当前版本已**完全重构为 Netty 事件循环**（boss+worker），作为**唯一 TCP 入口**；原经典阻塞 I/O（`TcpServer`/`ServerThread`/`*SocketCache`）已移除。下行与重发全部经由 Netty Channel 完成。

---

## 1. 技术栈

- Java 17 + Spring Boot 3.3.5（`@SpringBootApplication`，`@ComponentScan("com.proxy")`；2026-11 由 Java 8 / Boot 2.1.6 迁移，JPA 实体已迁 `jakarta.persistence`，Mongo 驱动 4.x API）
- **Netty**：TCP 接入、拆帧、下行写回（事件循环，支撑高并发）
- **Spring AMQP (RabbitMQ)**：上行发布（`iot.exchange` / `proxy.uplink`）+ 下行订阅（`iot.exchange` / `proxy.downlink`）
- **MongoDB / H2**：下行消息状态、失败记录、设备档案持久化
- **Gson**：上下行 `ServiceRequest` / `DownlinkMessage` 的 JSON 序列化（`com.proxy.utils.JsonUtils`，`byte[]` 以 base64 字符串读写，与 platform_java 的 `String`(base64) 对齐）
- **JUnit 5**：单元/冒烟测试；`DeviceMockTCP` 内置套件基于 JUnit Platform Launcher 运行

---

## 2. 整体架构

```
                         ┌─────────────────────────── ProxyTcp ───────────────────────────┐
设备(TCP)                │                                                                   │
   │   TCP 连接           │   NettyTcpServer (boss+worker)                                  │
   │ ───────────────────► │      │                                                           │
   │  帧: FF EE|len|payload│   UplinkChannelHandler                                          │
   │                      │      ├─ 连接建立: 按 ip:port 注册 addressChannels                │
   │                      │      └─ UplinkFrameDecoder 拆帧                                 │
   │                      │            └─ 完整帧 → Message(payload=设备协议字节,ip,port)      │
   │                      │                  └─► receiveQueue                              │
   │                      │                         │                                      │
   │                      │   TransportUplinkProcessor (解码 transport 层, 取 deviceId...)   │
   │                      │      ├─ channelRegistry.bindDevice(deviceId, ip, port)          │
   │                      │      └─ ServiceRequest(JSON, JsonUtils)                         │
   │                      │            └─► rabbitTemplate ─► iot.exchange / proxy.uplink ──┐│
   │                      │                                                  (上行发布)     ││
   │                      │   ChannelRegistry: addressChannels(ip:port→Ch),                 ││
   │                      │                    deviceChannels(deviceId→Ch),                ││
   │                      │                    getOnlineDeviceIds()                         ││
   │ ◄─────────────────── │                                                                  ││
   │  帧: FF EE|len|payload│   NettyDownlinkSender (sendQueue 唯一消费者)                      ││
   │                      │      └─ 按 deviceId 取 Channel ── 写 FF EE|len|payload ─────────┘│
   │                      │            ▲                                                    │
   │                      │   DownlinkProcessor.send() ──► sendQueue                        │
   │                      │      ▲ (offer)                                                 │
   │                      │   TransportDownlinkProcessor / ResendDownlinkProcessor          │
   │                      │      (编码完整设备协议帧 → Message(payload) → send)              │
   └──────────────────────┘                                                                 │
                                                                                             │
   后台 platform_java ──► platform.downlink ──► dtm ──► proxy.downlink ──► PlatformDownlinkReceiver (@RabbitListener) ┘
                                                                 (Gson→ServiceRequest→DownlinkTransportMessage→process)
```

---

## 3. 包结构

```
com.proxy
├── ProxyApplication            启动入口：装配 receiveQueue/sendQueue，启动上行处理线程 + Netty 服务
├── netty                      接入层
│   ├── NettyTcpServer         唯一 TCP 入口（boss+worker，bind 后阻塞于 closeFuture）
│   ├── UplinkChannelHandler   每连接处理器：注册会话、拆帧、投 receiveQueue
│   ├── UplinkFrameDecoder     基于 ByteToMessageDecoder 的帧解析（半包/粘包/重同步）
│   ├── NettyDownlinkSender    sendQueue 唯一消费者，按 deviceId 写回设备
│   └── ChannelRegistry        会话注册表（addressChannels / deviceChannels）
├── mq                        下行 MQ 消费
│   └── PlatformDownlinkReceiver  @RabbitListener 消费 proxy.downlink，解析后转内部下行链路
├── transport                  业务处理
│   ├── TransportUplinkProcessor   上行：receiveQueue → 解码 → 绑定 → 发布 proxy.uplink
│   ├── TransportDownlinkProcessor 下行：设备协议帧编码 → sendQueue
│   └── ResendDownlinkProcessor    重发：缓存下行消息经 sendQueue 重投
├── handler
│   └── DownlinkProcessor      抽象基类，持有 sendQueue，send() → offer
├── model                      Message / ServiceRequest / DownlinkTransportMessage /
│                               UplinkTransportMessage / MessageHeader / ServiceResponse ...
├── persistence                DeviceInfoService / DownlinkMessageService / SendFailureMessageService
├── concurrent                 业务线程池
├── codec                      MessageCodeHandler（设备协议帧编解码）
└── utils                      JsonUtils（base64 感知 Gson，统一上下行 serviceData 编码）
```

---

## 4. 业务流程

### 4.1 上行（设备 → 后台）

1. 设备与 `NettyTcpServer` 建立 TCP 连接；`UplinkChannelHandler` 在 `channelActive` 时按 `ip:port` 注册到 `ChannelRegistry.addressChannels`。
2. 设备发送 TCP 帧：`0xFF 0xEE | 长度(2字节,小端) | payload`。
3. `UplinkFrameDecoder` 按帧格式拆包（支持半包续传、粘包拆分、非法帧头重同步、声明长度大于可用字节时等待补全），得到 `payload`（设备协议字节）。
4. `UplinkChannelHandler` 组装 `Message`（payload=设备协议字节, ip, port）并 `offer` 到 `receiveQueue`。
5. `TransportUplinkProcessor` 工作线程从 `receiveQueue.take()`，调用 `MessageCodeHandler` 解码 transport 层，得到 `deviceId/nodeId/serviceType/serviceData/...`。
6. 调用 `channelRegistry.bindDevice(deviceId, ip, port)`，将 `deviceId → Channel` 写入 `deviceChannels`，供下行按 deviceId 取通道。
7. 构造 `ServiceRequest`（`JsonUtils` 序列化，`serviceData` 以 base64 字符串写出，与 platform_java 对齐）经 `rabbitTemplate.convertAndSend("iot.exchange", "proxy.uplink", json)` 发布。
8. dtm 透传 → platform_java 的 `UplinkMessage`（JSON 信封）被消费、入库、刷新在线状态、并按规则产生告警。

### 4.2 下行（后台 → 设备）

**内部下发链路（已打通，含重发）**：

1. `TransportDownlinkProcessor.process(DownlinkTransportMessage)`：查 `DeviceInfo`，用 `MessageCodeHandler` 编码**完整设备协议帧**（transport header + transport part + serviceData + CRC），装入 `Message.payload`。
2. `DownlinkProcessor.send()` 将 `Message` `offer` 到 `sendQueue`。
3. `NettyDownlinkSender`（sendQueue 唯一消费者）按 `message.getDeviceId()` 从 `ChannelRegistry.deviceChannels` 取 Channel，写出 `0xFF 0xEE | 长度(小端) | payload`，设备即收到原始设备协议帧。
4. 发送失败或无在线通道：记录/丢弃；由 `ScheduledTask` 定时调度 `ResendDownlinkProcessor` 对**在线设备**（`getOnlineDeviceIds()` 过滤）重发缓存的下行消息（最多 `maxResendTimes=8`，超次标记 `FAILED` 并存 `SendFailureMessage`）。

**平台来源链路（已打通）**：platform_java 下发 → `platform.downlink` → dtm → `proxy.downlink` → `PlatformDownlinkReceiver`（`@RabbitListener`，声明式 `@QueueBinding` 幂等创建队列/交换器/绑定）用 Gson 把 `DownlinkMessage`/`ServiceRequest` JSON 解析为 `DownlinkTransportMessage`，调用 `TransportDownlinkProcessor.process(...)` 走上述内部链路。

### 4.3 TCP 帧格式

| 字段 | 长度 | 说明 |
|---|---|---|
| 魔数 | 2 字节 | `0xFF 0xEE`（固定） |
| 长度 | 2 字节 | **小端**，表示后续 payload 字节数（不含头） |
| payload | N 字节 | 设备协议字节（transport header + transport part + serviceData + CRC） |

上行与下行使用**相同**帧格式（下行由 `NettyDownlinkSender` 加头，上行由 `UplinkFrameDecoder` 去头）。

---

## 5. MQ 消息契约

| 方向 | exchange | routing / queue | body | 状态 |
|---|---|---|---|---|
| 上行发布 | `iot.exchange` (topic) | `proxy.uplink` | `ServiceRequest` JSON（JsonUtils） | **活跃**（Spring AMQP） |
| 下行订阅 | `iot.exchange` (topic) | `proxy.downlink` | `DownlinkMessage` JSON（platform_java 发出，字段对齐 `ServiceRequest`） | **活跃**（Spring AMQP `@RabbitListener`） |
| 遗留直连 | `/kebai/link/v5` (DIRECT) | routing `up`/`down` | 原始字节 | **已删除**（`amq` 包已清理，原遗留直连交换器不再使用） |

`ServiceRequest` 字段：`deviceId`、`nodeId`、`eventId`、`eventCreationTime`、`serviceType`、`serviceVersion`、`serviceDataEncoding`、`serviceDataProtocolVersion`、`serviceEncryption`、`serviceData`（原始设备协议字节，经 `JsonUtils` 以 **base64 字符串** 读写，与 platform_java 的 `UplinkMessage`/`DownlinkMessage`(`String`) 一致）。

---

## 6. 配置（`application.yml` 关键项）

所有配置已统一到 `src/main/resources/application.yml`（标准 Spring Boot 位置，打包进 jar；也可用同目录外部文件覆盖）。

- `tcpport`：TCP 监听端口，**默认 50001**；设为 `0` 使用临时端口（测试/避免冲突时可用）。
- `spring.rabbitmq.*`（`host`/`port`/`username`/`password`/`virtual-host`，经 `${mq_server_*}` 引用）：**必须与 dtm、platform_java 指向同一 RabbitMQ 实例**，否则上行发布/下行订阅不可达。
- `spring.data.mongodb.uri`（`dbtype=mongo` 生效）：下行消息状态、失败记录、设备档案持久化（备选 uri 在 `application.yml` 中以注释保留，按需切换）。

---

## 7. 已知问题与待办（重要）

1. **broker 一致性**：ProxyTcp / dtm / platform_java 三者须连**同一** RabbitMQ 实例（同一 `iot.exchange`）。
2. **设备档案依赖**：`TransportDownlinkProcessor` 编码时依赖 `DeviceInfoService` 中的设备协议版本等信息；Netty 下行按 `deviceId` 取 Channel，`Message` 中的 `ip/port` 字段仅为兼容保留（经典链路遗留），不再用于路由。
3. **无回退**：经典阻塞 I/O 已完全删除，预发环境请先验证 Netty 下行（ACK/重发）再上生产。

---

## 8. 运行与冒烟测试

- **启动**：`ProxyApplication.main` 启动 Spring Boot；先 `TransportUplinkProcessor.startup()`，再 `NettyTcpServer.start()`（阻塞于 `closeFuture`，保持进程存活）。
- **设备模拟**：`src/test/java/com/proxy/test/DeviceMockTCP.java` —— 支持 37 类业务报文的 schema 编码/解码、TCP 拆包/CRC 校验和业务字段打印。**无参数运行 main 即执行内置完整测试套件**（`MockProtocolCodecTest` + `ProtocolSchemaSyncTest` + `DeviceMockTcpNettyE2ETest`，自包含、不依赖外部服务）；`--live` 连接真实 `127.0.0.1:50001` 发送全部 37 类并等待下行；另有 `--test` / `--all` / `--type` / `--json` / `--listen`。完整用法见 `dtm/docs/local-integration.md` 第 10 节。
- **自包含闭环冒烟**：`NettyClosedLoopSmokeTest`（JUnit，类名以 `Test` 结尾，已被 `mvn test` 默认执行）——最小上下文（仅 `com.proxy.netty` + 两个队列，**不依赖 broker/DB**），验证「上行解码 → 绑定 → 下行经 sendQueue 写回」闭环。也可显式 `mvn test '-Dtest=NettyClosedLoopSmokeTest'`。
- **帧解析单测**：`UplinkFrameDecoderTest`（基于 `EmbeddedChannel`）——覆盖整帧/半包/粘包/重同步/长度等待。
- **全业务类型端到端**：`DeviceMockTcpNettyE2ETest` —— 启动真实 Netty 接入层，DeviceMockTCP 发送 37 类上行并接收同类型下行；`MockProtocolCodecTest` 覆盖 37 协议 encode/decode round-trip、空载荷、unsupported、JSON、CRC/截断。
- **schema 同步**：`ProtocolSchemaSyncTest` 校验 `ProxyTcp/src/test/resources/protocol-schemas.json` 与 dtm 生产 schema 字节级一致。
- **下行消费者单测**：`PlatformDownlinkReceiverTest`（无 broker）——验证 `DownlinkMessage`/`ServiceRequest` JSON 解析、字段映射与容错（非法 JSON / 缺 deviceId）。
- **构建/测试**：`mvn test`（含上述用例）。

## 10. 下行重试与可靠性

- **重试触发**：`@Scheduled(cron="${downlinkResendCron}")` 每分钟触发 `ScheduledTask.resendMessage2Device`；单条消息的「重发 / 判失败」决策集中在 `DownlinkRetryPolicy.decide`（纯函数，便于单测）。
- **终止双保险**：`maxResendTimes`（次数上限，取自配置 `maxResendTimes`，缺失回退 8）与 `judgeSendFailedAferMinutes`（时间上限，默认 10 分钟）。任一达到即判 `FAILED` 并写 `SendFailureMessage` 通知平台，避免无限重发（替代原逻辑中 `>100` 的死分支）。
- **离线不再永久堆积**：所有非 `FAILED` 且超过时间上限的下行（**在线/离线一致**）统一在调度中批量判 `FAILED`；旧逻辑对离线设备只入队丢弃、永不清理，会无限堆积。
- **最小重发间隔**：`resendIntervalSeconds`（默认 60s）避免升级包等大下行在设备 `ACK` 前被重复洪泛；设备回 `ACK` 会经 `TransportUplinkProcessor.dealAckMessage` 删除缓存，停止重发。
- **状态语义**：`Status.SENT` 表示「已提交发送队列」（Netty 下发为 fire-and-forget，不保证设备已收到），并非「已送达」；真正送达以设备 `ACK` 删除缓存为准。
- **H2 模式单实例约束**：下行重发的分布式锁（`ProxyLock`）仅在 `dbtype=mongo` 时生效；**使用 H2 时务必单实例部署**，否则多实例会重复重发。

---

## 9. 相关仓库

- `dtm`：上行/下行消息透传（`iot.exchange` / `proxy.uplink` / `platform.uplink` / `platform.downlink` / `proxy.downlink`），字节透传、载荷为 JSON 信封。
- `platform_java`：后台业务平台，消费 `platform.uplink`（`UplinkMessage` = 网关 `ServiceRequest` 信封），下发 `platform.downlink`（`DownlinkMessage`，对齐 `ServiceRequest`）。
