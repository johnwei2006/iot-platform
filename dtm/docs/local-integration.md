# dtm 三端本机端到端联调

本手册描述 `ProxyTcp → dtm → platform_java` 上行、`platform_java → dtm → ProxyTcp` 下行在本机跑通的步骤。
协议字段与端序依据见 [protocol-mapping.md](protocol-mapping.md)。

## 1. 前置条件

| 组件 | 本机配置 | 说明 |
|---|---|---|
| JDK | `C:\Program Files\Java\jdk1.8.0_202` | 三个工程均为 Java 8 / Spring Boot 2.1.6 |
| Maven | 3.6+ | `mvn -q -DskipTests package` |
| RabbitMQ | `localhost:5672`，管理页 `http://localhost:15672/`，`guest/guest` | 需启用 `rabbitmq_management` |
| MongoDB | `127.0.0.1:27017`，无认证 | ProxyTcp 用 `proxy` 库，platform_java 用 `iot_platform` 库 |

启动 Mongo（非管理员时可绕过服务，直接后台运行）：

```powershell
Start-Process -FilePath "C:\Program Files\MongoDB\Server\8.0\bin\mongod.exe" `
  -ArgumentList '--config','"C:\Program Files\MongoDB\Server\8.0\bin\mongod.cfg"' -WindowStyle Hidden
```

## 2. 统一 broker 与 vhost（关键）

三端必须连**同一个 RabbitMQ 实例 + 同一个 vhost**，否则 `iot.exchange` 与 4 个队列互不可见。

| 应用 | 配置文件 | vhost 默认值 | 覆盖方式 |
|---|---|---|---|
| ProxyTcp | `ProxyTcp/src/main/resources/application.yml` | `${MQ_VHOST:/caipos_mqtt}` | `--mq_server_vhost=/` 或环境变量 `MQ_VHOST` |
| dtm | `dtm/src/main/resources/application.yml` | `${RABBITMQ_VHOST:/}` | `--spring.rabbitmq.virtual-host=/caipos_mqtt` 或 `RABBITMQ_VHOST` |
| platform_java | `platform_java/src/main/resources/application.yml` | `${RABBITMQ_VHOST:/}` | 同上 |

dtm / platform_java 默认就是 `/`，只需把 ProxyTcp 覆盖成 `/`（启动参数 `--mq_server_vhost=/` 或环境变量 `MQ_VHOST=/`），这是本机最省事的方案（guest 在 `/` 与 `/caipos_mqtt` 上均有全权限）。若 ProxyTcp 保持默认 `/caipos_mqtt`，则给 dtm / platform_java 设置 `RABBITMQ_VHOST=/caipos_mqtt`。

## 3. 构建与启动

> 如果本机已有旧 jar 在运行，先停掉再打包（Windows 会锁定 jar 文件）。
> 旧 jar 中 `TransportUplinkProcessor.callMQService` 的 `convertAndSend` 曾被注释，只打「Published」假日志，必须重打包。

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_202'
$root = 'c:\KeBai\5_Codes_Github\2_codes\dtm_platform'

Get-Process java -ErrorAction SilentlyContinue |
  Where-Object { $_.Path -like '*jdk1.8*' } | Out-Null   # 按需手动确认旧进程

cd "$root\ProxyTcp";    mvn -q clean package -DskipTests
cd "$root\dtm";         mvn -q clean package -DskipTests   # 必须 clean：已删除 relay/ServiceDataCodec
cd "$root\platform_java"; mvn -q clean package -DskipTests
```

依次后台启动（日志重定向到 `%TEMP%` 便于排查）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_202'
$root = 'c:\KeBai\5_Codes_Github\2_codes\dtm_platform'
$java = "$env:JAVA_HOME\bin\java.exe"

Start-Process -FilePath $java -ArgumentList '-jar', "$root\dtm\target\dtm-0.0.1-SNAPSHOT.jar" `
  -RedirectStandardOutput "$env:TEMP\dtm.log" -RedirectStandardError "$env:TEMP\dtm.err.log" -WindowStyle Hidden

Start-Process -FilePath $java -ArgumentList '-jar', "$root\platform_java\target\platform-java-0.0.1-SNAPSHOT.jar" `
  -RedirectStandardOutput "$env:TEMP\platform.log" -RedirectStandardError "$env:TEMP\platform.err.log" -WindowStyle Hidden

Start-Process -FilePath $java -ArgumentList '-Xms512M','-Xmx2048M','-jar', `
  "$root\ProxyTcp\target\proxytcp-0.0.1-SNAPSHOT.jar", '--mq_server_vhost=/' `
  -RedirectStandardOutput "$env:TEMP\proxy.log" -RedirectStandardError "$env:TEMP\proxy.err.log" -WindowStyle Hidden
```

端口自检：

```powershell
Get-NetTCPConnection -State Listen -LocalPort 50001,8010,8081,8082,5672,15672,27017 |
  Select-Object LocalPort,OwningProcess | Sort-Object LocalPort
```

## 4. 验证上行（ProxyTcp → dtm → platform_java）

用管理页 `Exchanges → iot.exchange → Publish message`（routing key `proxy.uplink`），或 HTTP API：

```powershell
$sec  = ConvertTo-SecureString 'guest' -AsPlainText -Force
$cred = New-Object System.Management.Automation.PSCredential('guest', $sec)
$body = @{
  routing_key = 'proxy.uplink'
  payload = (@{
      deviceId = '30410007102'; nodeId = ''; eventId = 2774
      serviceType = 101; serviceVersion = 1
      serviceDataEncoding = 2; serviceDataProtocolVersion = 1; serviceEncryption = 0
      serviceData = 'AQI='        # 0x01 0x09
    } | ConvertTo-Json -Compress)
  payload_encoding = 'string'
  properties = @{ content_type = 'application/json' }
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri 'http://localhost:15672/api/exchanges/%2F/iot.exchange/publish' `
  -Credential $cred -ContentType 'application/json' -Body $body
```

预期：

- dtm 日志 `dtm uplink decode: ... serviceType=101 protocol=GATEWAY_REBOOT status=OK bytes=2`；
- `platform.uplink` 消息体在原有字段上追加：

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

- platform_java 日志 `ingested uplink ... protocol=GATEWAY_REBOOT decodeStatus=OK ...`，并把结构化字段写入 `data_records`。

> 想直接看到 `platform.uplink` 里的报文体：先停 platform_java，再发布，用管理页 `Queues → platform.uplink → Get messages` 查看。

## 5. 验证下行（platform_java → dtm → ProxyTcp）

向 `platform.downlink` 发布**结构化**下行：

```json
{
  "deviceId": "30410007102",
  "serviceType": 101,
  "serviceVersion": 1,
  "serviceDataEncoding": 2,
  "serviceDataProtocolVersion": 1,
  "serviceEncryption": 0,
  "serviceDataDecoded": { "appType": 1, "action": 9 }
}
```

预期 dtm 输出到 `proxy.downlink` 的**标准 ServiceRequest**（不含任何附加字段）：

```json
{
  "deviceId": "30410007102",
  "serviceType": 101,
  "serviceVersion": 1,
  "serviceDataEncoding": 2,
  "serviceDataProtocolVersion": 1,
  "serviceEncryption": 0,
  "serviceData": "AQk="
}
```

ProxyTcp 的 `PlatformDownlinkReceiver` 用 `JsonUtils.gson()` 把 `AQk=` 还原成 `byte[]{1,9}` 并交给 `TransportDownlinkProcessor` 编码设备帧。
设备不在线时 Netty 侧会记录「无可用 Channel」，属预期（可用 `DeviceMockTCP` 连 50001 后重测）。

## 6. 手动 ACK / publisher confirm / DLQ 验证

dtm 的处理顺序是：

```text
proxy.uplink 消息 -> UplinkConsumer 提交 dtmWorkerExecutor
  -> UplinkService 解码并发布 platform.uplink
  -> MessagePublisher 等待 broker confirm ACK
  -> confirm 成功 -> listener 线程 basicAck 上游
  -> confirm 失败/超时 -> listener 线程 basicNack
```

验证点：

| 场景 | 操作 | 预期 |
|---|---|---|
| 正常 ACK | 发布一条合法上行 | 管理页 `proxy.uplink` 的 ack 计数 +1；`platform.uplink` deliver +1；dtm 日志 `dtm uplink decode ... status=OK` |
| confirm 失败 | 停掉 RabbitMQ 或阻断 5672 后发布 | dtm 日志 `publisher confirm timeout`；`proxy.uplink` 消息未被 ack，恢复后重新投递（至少一次） |
| worker 失败进 DLQ | `--dtm.protocol.fail-open=false` 并发布非法 JSON | `proxy.uplink` 消息被 nack 且不 requeue，进入 `iot.dlq` |
| 超时 requeue | 调小 `dtm.worker.await-timeout-ms` 并制造慢处理 | 日志 `worker timeout ...; requeue`，消息重新投递，可能重复处理 |
| 结构化下行 | 向 `platform.downlink` 发布带 `serviceDataDecoded` 的报文 | `proxy.downlink` 输出标准 ServiceRequest，`serviceData` 为编码后的 base64，扩展字段全部消失 |

> 管理页 `Ready` 常为 0 是正常的：消费者会立即取走。看 `message_stats.ack` / `deliver_get` 与 `iot.dlq` 的 `messages` 更准确。

## 7. 可观测点

| 检查项 | 位置 |
|---|---|
| 4 个队列声明 | 管理页 `Queues`：`proxy.uplink` / `platform.uplink` / `platform.downlink` / `proxy.downlink`（durable，带 DLX `iot.dlx`） |
| 交换器 | `iot.exchange`（topic）、`iot.dlx`（topic）、`iot.dlq` |
| 发布/消费速率 | `Exchanges → iot.exchange` 的 Publish / Deliver rate；队列 `Ready` 常为 0 是因为消费者立即取走 |
| 解码状态 | 上行消息体的 `decodeStatus`：`OK` / `EMPTY` / `UNSUPPORTED` / `FAILED` |
| 处理失败 | AMQP header `decodeStatus=FAILED` / `encodeStatus=FAILED` + `decodeError` / `encodeError`，同时保留原报文 |
| 手动 ACK | `proxy.uplink` / `platform.downlink` 的 `message_stats.ack` 随成功处理增长；未 ack 的消息在消费者断开后重新投递 |
| 发布确认 | dtm DEBUG 日志 `published ... (confirm=...)`；失败时 `publisher confirm nack/timeout` |
| 线程池 | 日志线程名前缀 `dtm-worker-`；队列满时 `CallerRunsPolicy` 在消费者线程执行，表现为消费速率下降而非丢消息 |
| 死信 | worker 异常 / `fail-open=false` 主动 reject 进 `iot.dlq`；超时消息 requeue 而非进 DLQ |

## 8. 开关与回退

`dtm/src/main/resources/application.yml`：

```yaml
dtm:
  protocol:
    enabled: ${DTM_PROTOCOL_ENABLED:true}       # false = 回退为纯字节透传
    fail-open: ${DTM_PROTOCOL_FAIL_OPEN:true}   # false = 失败时 reject 进死信
    max-payload-bytes: 8192
    max-repeat-items: 256
  worker:
    core-size: ${DTM_WORKER_CORE:8}
    max-size: ${DTM_WORKER_MAX:32}
    queue-capacity: ${DTM_WORKER_QUEUE:1024}
    keep-alive-seconds: 60
    await-timeout-ms: ${DTM_WORKER_AWAIT_TIMEOUT_MS:60000}
    confirm-timeout-ms: ${DTM_CONFIRM_TIMEOUT_MS:10000}
    profile-cache-max: ${DTM_PROFILE_CACHE_MAX:10000}
spring:
  rabbitmq:
    publisher-confirms: true
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        retry:
          enabled: false
```

启动时覆盖：`--dtm.protocol.enabled=false`、`--dtm.worker.core-size=16`。

## 9. 常见故障

| 现象 | 原因 |
|---|---|
| 管理页看不到任何队列 | 三端未启动，或连的不是同一 broker/vhost |
| dtm 日志 `unsupported: serviceType=0x2` | 0x02 是 ProxyTcp 内部在线检测，不在 37 条协议里，按设计原样透传 |
| `decodeStatus=FAILED` + `truncated field ...` | 报文字节数少于 Excel 字段定义（多为设备固件实现差异），检查 `protocol-mapping.md` 第 4 节 |
| 上行有日志但平台没数据 | ProxyTcp 仍是旧 jar（发送语句被注释），必须重新打包 |
| 下行 ProxyTcp 报 JSON 解析错 | dtm 输出带了非标准字段或 `serviceData` 为 JSON null；dtm 会剥离附加字段并保证 `serviceData` 是 base64 字符串 |
| 下行消息一直进 `iot.dlq` | dtm 对下行编码/信封失败始终 NACK（不 fail-open），检查 `serviceDataDecoded` 字段是否与协议 schema 匹配、`serviceEncryption` 是否为 0、`serviceDataEncoding` 是否为 1/2 |
| ProxyTcp 日志反复 `PRECONDITION_FAILED - inequivalent arg 'x-dead-letter-exchange' for queue 'proxy.downlink'` | dtm / platform_java 的 4 个队列都带 `x-dead-letter-exchange=iot.dlx`、`x-dead-letter-routing-key=iot.dlq`，ProxyTcp 的 `@QueueBinding` 必须声明同样的 arguments，否则同一队列重复声明参数不一致。已在 `PlatformDownlinkReceiver` 对齐 |

## 10. DeviceMockTCP 模拟设备（全业务类型）

`ProxyTcp/src/test/java/com/proxy/test/DeviceMockTCP.java` 已升级为可收发 37 类平台-网关协议的模拟设备：

- 发送：按 `ProxyTcp/src/test/resources/protocol-schemas.json` 编码 `u8/u16/u32/string/hex/time6/time3/repeat`，自动组 `FF EE + 小端长度 + payload` 帧；
- 接收：独立线程拆包/粘包、校验 XOR CRC、解析 deviceId/nodeId/messageId/eventId/serviceType，并按 schema 解码业务字段、缩进打印；
- `0x60 GATEWAY_GNSS` 没有字段定义，走原始字节路径，打印 raw hex；
- `0x00 SERVICE_ACK` 是 ProxyTcp 内部确认，按空载荷识别并打印 `EMPTY`，不会显示成 UNKNOWN；
- 测试 API：`sendService`、`sendRawService`、`sendJsonService`、`sendAllServices`、`awaitMessage`、`drainMessages`。

### 10.1 编译测试类

```powershell
cd c:\KeBai\5_Codes_Github\2_codes\dtm_platform\ProxyTcp
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_202'
mvn -q -DskipTests test-compile
```

### 10.2 生成测试 classpath

```powershell
mvn -q dependency:build-classpath '-Dmdep.outputFile=target/test-classpath.txt' '-Dmdep.includeScope=test'
$cp = "target\test-classes;target\classes;" + ((Get-Content target\test-classpath.txt -Raw).Trim())
$java = "$env:JAVA_HOME\bin\java.exe"
```

### 10.3 无参数启动：完整测试（推荐）

```powershell
& $java -cp $cp com.proxy.test.DeviceMockTCP
# 等价于
& $java -cp $cp com.proxy.test.DeviceMockTCP --test
```

无参数默认执行内置完整测试套件，**不依赖外部 ProxyTcp / RabbitMQ / Mongo**：

| 测试类 | 覆盖 |
|---|---|
| `MockProtocolCodecTest` | 37 协议 encode/decode round-trip、空载荷、unsupported、JSON、ACK、CRC/截断 |
| `ProtocolSchemaSyncTest` | 测试 schema 与 dtm 生产 schema 字节级一致 |
| `DeviceMockTcpNettyE2ETest` | 内嵌真实 Netty 接入层（临时端口），发送 37 类上行、回写 37 类下行、设备端解码业务字段 |

输出包含每个用例的 `[TEST PASS]`，最后打印汇总，失败时退出码为 1：

```text
[DeviceMockTCP] no live args: running built-in full test suite (3 test classes)
[TEST PASS] com.proxy.test.protocol.MockProtocolCodecTest.loadsAll37Protocols
...
[DeviceMockTCP] built-in tests: run=12 failures=0 ignored=0 time=2500ms => PASS
```

> 在 IDE 里直接 Run `DeviceMockTCP.main` 即可完整执行；不要只发一条 `0x65` 后等待——那是旧的默认行为。

### 10.4 真实链路 CLI（`--live`）

```powershell
# 连本机 50001，发送全部 37 类业务并等待下行（默认 20 秒）
& $java -cp $cp com.proxy.test.DeviceMockTCP --live --listen=30

# 等价显式写法
& $java -cp $cp com.proxy.test.DeviceMockTCP 127.0.0.1 50001 --all --listen=30

# 指定业务类型 / 自定义字段
& $java -cp $cp com.proxy.test.DeviceMockTCP 127.0.0.1 50001 --type=101 --listen=15
& $java -cp $cp com.proxy.test.DeviceMockTCP 127.0.0.1 50001 --type=101 --json='{"appType":1,"action":9}' --listen=15
```

`--type` 支持十进制或 `0x` 十六进制、可重复；`--device-id` 可覆盖默认 `30410007102`。

> **真实链路只会收到 `0x00 SERVICE_ACK`**（ProxyTcp 对每条上行的确认，解码为 `EMPTY`）。要收到业务下行需按第 5 节向 `platform.downlink` 发布消息；若想自动收齐 37 类业务下行，请改用无参数内置测试（10.3）。
>
> 若发布后仍只有 ACK：检查 dtm(8081)/platform_java(8082) 是否在运行，以及 RabbitMQ 管理页 `proxy.downlink` 的 consumers 是否大于 0；`platform.downlink` 堆积且 consumers=0 表示 dtm 消费者未启动。内置测试（10.3）不依赖这些外部服务。

结束时会打印汇总，便于判断测试是否跑完整：

```text
[DeviceMockTCP] SUMMARY sent=37 received=38 ack=37 businessOK=1 businessEMPTY=37 businessUNSUPPORTED=0 businessFAILED=0
[DeviceMockTCP] downlink by serviceType: 0x00 x37, 0x65 x1
```

收到业务下行时打印：

```text
[DOWNLINK] 0x65 GATEWAY_REBOOT direction=downlink
  deviceId=30410007102, nodeId=, messageId=8037, eventId=7037
  time=2026-11-10 16:11:54, serviceVersion=1, transportEncoding=2, transportVersion=1, frameLength=48
  serviceDataEncoding=2, protocolVersion=1, encryption=0, serviceDataLength=2
  serviceData(hex)=0109
  business=OK
{ "appType": 1, "action": 9 }
```

### 10.5 单独运行自动化测试

```powershell
cd c:\KeBai\5_Codes_Github\2_codes\dtm_platform\ProxyTcp

mvn -q '-Dtest=MockProtocolCodecTest' test
mvn -q '-Dtest=DeviceMockTcpNettyE2ETest' test
mvn -q '-Dtest=ProtocolSchemaSyncTest' test
mvn -q test
```

> `ProxyTcp/src/test/resources/protocol-schemas.json` 是 `dtm/src/main/resources/protocol-schemas.json` 的测试副本；协议变更时两处必须同步，`ProtocolSchemaSyncTest` 会做字节级比较并失败提醒。
