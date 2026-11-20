# 协议 5.0「平台-网关协议」映射与字节级核对

> **事实源**：`Protocal5.0.xlsx` 的「平台-网关协议」与「平台网关协议明细」。
> **交叉校核**：`Gateway4GCat1/transport_udp/transport_udp_message.h`（命令码）、
> `transport_udp/transport_udp_service_message.h`（结构体）、
> `transport_udp_encoder/transport_udp_service_encoder.c`（下/上行编解码，下文简称 ENC）。
> **代码落地**：`dtm/src/main/resources/protocol-schemas.json`（声明式 schema）、
> `com.iot.dtm.protocol.ServiceType`（37 个枚举）、`ProtocolRegistry`（启动校验）。

## 1. 范围

覆盖「平台-网关协议」37 个 serviceType（0x01~0x65）。**不包含**：

- `0x02`（ProxyTcp 内部在线检测，日志中常见但不在协议清单）、`0x00 ACK`、`0xC6` 节点入网；
- 「网关-节点协议(Lora)」0x67 起的节点侧协议；
- `19_gateway_device_control`、`17_gateway_sensor_query`、`91_ota`、`6_node_device_control` 四张专项表。

未知类型（含 0x02）按 `decodeStatus=UNSUPPORTED` 原样透传，不报错、不进死信。

## 2. 全局字节规则

| 主题 | 结论 | 依据 |
|---|---|---|
| serviceData | `ServiceRequest.serviceData` 的 base64 解码结果；`serviceDataEncoding=2` 走 schema，`=1` 直接当 JSON 文本 | `ProxyTcp/JsonUtils.java`、`transport_udp_message.h:52-53` |
| 6 字节时间 | **普通二进制字节** `(year%100, month, date, hour, minute, second)`，**不是 BCD** | `gateway_manager.c:34-39`、`service_gateway_time_sync_response.c:49-54`、`ENC:28-33` |
| 0x18 collectTime | 例外：C 侧 `get_current_time_hex()` 写的是 6 个 ASCII 十六进制字符（"%06X" HHMMSS），dtm 统一按 `time6` 解码，后台需按业务区分 | `utils/time_utils.c:18-51` |
| 定长字符串 | 按 `length` 读，去尾部 NUL；写出时补零到定长 | `ENC` 中 `rt_memcpy(dst, src, FIXED_LEN)` |
| 变长 message | 响应类按剩余字节读取（写出不补零） | `ENC:520` 写 `1 + strlen(message)`，`MAX_RESPONSE_MESSAGE_LENGTH=30` |
| 端序 | **逐字段声明**：0x06、0x0C、0x5C.packageSize 为小端；0x08/0x09/0x0D/0x10/0x11/0x14/0x15/0x22/0x23/0x25 的多字节数值为大端；其余默认大端 | `ENC:307-344`(LE)、`ENC:376-381`(BE)、`ENC:199`(LE) |
| 与传输层区分 | TCP 帧长度 `FF EE|len(2,小端)|payload` 的小端属于 ProxyTcp 传输层，**不能套用到业务字段** | `ProxyTcp/UplinkFrameDecoder` |
| seq 跳号 | 明细表 seq 存在重复/跳号（0x08 出现两个 9，0x23/0x25 编号不连续），schema 以「字段顺序 + 长度」为准 | Excel 明细表 |
| 载荷上限 | C 侧 `business_data_payload_length` 为 uint8_t（255B），而 0x5C=391B、0x10/0x14 可更长；dtm 默认上限 8192B 并按 Excel 实现，后台需注意固件侧截断 | `transport_udp_message.h:99-104`、`ENC:43-50` |

## 3. 37 条协议映射表

| serviceType | 名称 | 事务类型 | Excel 行 | 顶层字段数 | 端序 | C 侧参考 | 备注 / 取舍 |
|---|---|---|---|---|---|---|---|
| `0X01` | GATEWAY_HEART_BEAT | uplink | 2 | 0 | - | ENC:16-20（空载荷显式处理） | 空载荷；dtm 返回 decodeStatus=EMPTY |
| `0X03` | GATEWAY_TIME_SYNC_RESPONSE | downlink | 6 | 6 | - | ENC:21-36 / SVCMSG:10-17 | year=年%100，普通二进制 6 字节，非 BCD |
| `0X04` | GATEWAY_TIME_SYNC_REQUEST | uplink_callback | 6 | 0 | - | ENC default:269-271 | 上行回调，空载荷；应答由 platform_java 生成（0x03） |
| `0X05` | GATEWAY_INFO_QUERY | downlink_callback | 15 | 0 | - | ENC default:269-271 | 下行回调，空载荷 |
| `0X06` | GATEWAY_INFO_REPORT | uplink | 15 | 21 | 小端 | ENC:284-353 / GWINF:9-33 | 全部 uint16 小端；battery(4) 为原始字节（C 侧 float memcpy，端序无法推断）→ hex |
| `0X07` | GATEWAY_CONFIG_QUERY | downlink_callback | 39 | 0 | - | ENC default:269-271 | 下行回调，空载荷 |
| `0X08` | GATEWAY_CONFIG_REPORT | uplink | 39 | 13 | 大端 | ENC:354-384 / CFG:162-175 | uint16 大端；Excel 额外列 loraSf/loraBw/loraCr/loraTxPower（C 侧无，共 80B，C 为 76B）→ 以 Excel 为准 |
| `0X09` | GATEWAY_CONFIG | downlink_callback | 55 | 9 | 大端 | ENC:80-98 | uint16 大端；Excel 含 businessServerUrl(30)（C 侧最小 70B 无此字段）→ 以 Excel 为准 |
| `0X0A` | GATEWAY_CONFIG_RESPONSE | uplink | 55 | 2 | - | ENC:508-532（通用响应） | result(1)+message(变长)；Excel 写 30B，C 侧写 1+strlen → dtm 用 variable string |
| `0X0B` | GATEWAY_CONFIG_NODE_QUERY | downlink_callback | 68 | 0 | - | ENC default:269-271 | 下行回调，空载荷 |
| `0X0C` | GATEWAY_CONFIG_NODE_REPORT | uplink | 68 | 3 | 小端 | ENC:385-411 / CFG:142-146 | repeat nodes 16B/条，reportInterval 小端；尾部 configTime(6) |
| `0X0D` | GATEWAY_CONFIG_NODE | downlink_callback | 79 | 2 | 大端 | ENC:99-117 / SVCMSG:88-92 | repeat nodes 15B/条，reportInterval 大端，含 loraFreqGroup，无尾部时间 |
| `0X0E` | GATEWAY_CONFIG_NODE_RESPONSE | uplink | 79 | 2 | - | ENC:508-532 | result+message |
| `0X0F` | GATEWAY_CONFIG_SENSOR_QUERY | downlink_callback | 89 | 1 | - | ENC:161-172 | C 侧要求 11B nodeId（不是空载荷） |
| `0X10` | GATEWAY_CONFIG_SENSOR_REPORT | uplink | 89 | 4 | 大端 | ENC:449-475 / CFG:93-99 | repeat sensors 42B/条（Excel readHex=20，C 侧 26）→ 以 Excel 为准；尾部 configTime(6) |
| `0X11` | GATEWAY_CONFIG_SENSOR | downlink_callback | 100 | 3 | 大端 | ENC:118-137 | repeat sensors，readHex=26，无尾部时间 |
| `0X12` | GATEWAY_CONFIG_SENSOR_RESPONSE | uplink | 100 | 2 | - | ENC:508-532 | result+message |
| `0X13` | GATEWAY_CONFIG_DEVICE_QUERY | downlink_callback | 111 | 1 | - | ENC:161-172 | 11B nodeId |
| `0X14` | GATEWAY_CONFIG_DEVICE_REPORT | uplink | 111 | 5 | 大端 | ENC:412-448 | repeat devices 95B/条 + configTime(6)；C 侧每条目按 101B 且 configTime 越界 → 以 Excel 为准 |
| `0X15` | GATEWAY_CONFIG_DEVICE | downlink_callback | 126 | 3 | 大端 | ENC:138-160 | repeat devices 95B/条，无尾部时间 |
| `0X16` | GATEWAY_CONFIG_DEVICE_RESPONSE | uplink | 126 | 2 | - | ENC:508-532 | result+message（Excel seq 从 2 开始） |
| `0X17` | GATEWAY_SENSOR_DATA_UPLOAD_NOTICE | downlink_callback | 140 | 1 | - | ENC:161-172 | 11B nodeId |
| `0X18` | GATEWAY_SENSOR_DATA_REPORT | uplink | 140 | 5 | 大端 | ENC:476-507 / SVCMSG:41-48 | repeat sensors 6B/条；collectTime 在 C 侧为 6 个 ASCII 十六进制字符（TIMEU:49），dtm 统一按 time6 解码 |
| `0X19` | GATEWAY_DEVICE_CONTROL | downlink_callback | 152 | 3 | 大端 | ENC:37-79 / SVCMSG:26-31 | C 侧按剩余长度推导条目数但循环步长 4（BUG）；dtm 用 Excel 的 deviceSize 计数 |
| `0X1A` | GATEWAY_DEVICE_CONTROL_RESPONSE | uplink | 152 | 4 | - | ENC:533-559 | C 侧 [11-16] 有 6B 隐含空洞；dtm 按 Excel 用 controlResponse+deviceSize |
| `0X1B` | GATEWAY_DEVICE_CONTROL_BATCH | downlink_callback | 163 | 2 | 大端 | C 侧无实现（仅 MSG:32 宏） | 嵌套 repeat：nodeSize → nodeId/deviceSize → devices；C 侧无实现，仅按 Excel |
| `0X1C` | GATEWAY_DEVICE_CONTROL_BATCH_RESPONSE | uplink | 163 | 2 | - | C 侧无实现（仅 MSG:33 宏） | 嵌套 repeat，result 长度 N=deviceSize → 拆为 N 个 u8；C 侧无实现 |
| `0X21` | GATEWAY_CONFIG_TASK_QUERY | downlink_callback | 175 | 1 | - | ENC default:268 | Excel 定义 result(1)，C 侧为空载荷 → 以 Excel 为准 |
| `0X22` | GATEWAY_CONFIG_TASK_REPORT | uplink | 175 | 2 | 大端 | ENC:575-616 / CFG:192-207 | taskCount + repeat tasks（内嵌 triggerSize/actionSize 两层 repeat）；C 侧偏移算法损坏，以 Excel 为准 |
| `0X23` | GATEWAY_CONFIG_TASK | downlink_callback | 194 | 8 | 大端 | ENC:203-232 | 单任务，内嵌两层 repeat；C 侧偏移算法损坏 |
| `0X24` | GATEWAY_CONFIG_TASK_RESPONSE | uplink | 194 | 2 | - | ENC:508-532 | result+message |
| `0X25` | GATEWAY_CONFIG_TASK_LIST | downlink_callback | 212 | 2 | 大端 | ENC:233-264 | taskCount + repeat tasks；C 侧内层偏移不递增，以 Excel 为准 |
| `0X26` | GATEWAY_CONFIG_TASK_LIST_RESPONSE | uplink | 212 | 3 | - | ENC:508-532 | Excel 为 taskCount + (taskId,result) 列表 + message；C 侧仅通用 result+message → 以 Excel 为准 |
| `0X5C` | GATEWAY_UPGRADE | downlink_callback | 232 | 7 | 小端 | ENC:185-202 / SVCMSG:51-61 | packageSize 小端；C 解码 version 从 [65] 与 upgradeType 重叠（off-by-one），dtm 按 Excel 顺序 |
| `0X5D` | GATEWAY_UPGRADE_STATUS | uplink | 232 | 5 | - | ENC:560-574 / SVCMSG:63-71 | 定长 78B；C 侧有 error 字段但不编码 |
| `0X60` | GATEWAY_GNSS | uplink | - | 0 | - | C 侧无实现（仅 MSG:42 宏） | Excel 明细表无字段段落、C 侧无实现 → 已注册但 unsupported，原样透传 |
| `0X65` | GATEWAY_REBOOT | downlink | 242 | 2 | - | ENC:173-184 / SVCMSG:73-77 | 2 字节：appType + action |

## 4. Excel 与 C 实现的主要差异（dtm 的取舍）

1. **0x06 / 0x08 / 0x09 同为网关配置但端序互相矛盾**：0x06 全部 uint16 小端（ENC:307-344），0x08/0x09 大端（ENC:376-381、:92）；0x08 比 0x09 多 `configTime(6)`。dtm 按 Excel 字段顺序、按 C 证据逐协议声明端序。
2. **0x0C 与 0x0D 条目不一致**：0x0C 16B/条（小端、sensorCount/deviceCount、尾部时间），0x0D 15B/条（大端、loraFreqGroup、无时间）。
3. **0x10 与 0x11 的 readHex 长度不一致**：Excel 20 vs 26，C 侧统一 26；dtm 按 Excel 逐协议定义。
4. **0x14 C 侧越界**：每条目按 101B 分配但只写 95B，`configTime` 写在越界偏移；dtm 按 Excel 的 95B + 尾部 6B。
5. **0x19 剩余长度推导与循环步长矛盾**：`count=(len-11-1)/6` 但循环用 `i*4`；dtm 用 Excel 的 `deviceSize` 显式计数。
6. **0x1A 6 字节空洞**：C 侧 `[11-16]` 未写（复用下行缓冲区残留）；dtm 按 Excel 的 `controlResponse`。
7. **0x1B / 0x1C / 0x60 C 侧无实现**：仅按 Excel 定义；0x60 在 Excel 明细表也没有字段段落，故注册为 `unsupported`。
8. **任务配置 0x22 / 0x23 / 0x25 C 侧偏移算法损坏**（`+i`/`+j` 而非 `+i*entry`，0x25 内层偏移不随 j 变化），dtm 严格按 Excel 的嵌套 repeat 实现。
9. **0x26 / 0x24 C 侧只写通用 result+message**，Excel 的 0x26 还定义了任务结果列表；dtm 按 Excel。
10. **0x21 Excel 有 result(1)，C 侧为空载荷**；dtm 按 Excel。
11. **0x5C C 解码 off-by-one**：`version` 从 `[65]` 起与 `upgradeType[65]` 重叠，结构体 391B vs 解码 390B；dtm 按 Excel 顺序（391B）。
12. **响应 message 长度**：Excel 写 30B 定长，C 侧写 `1+strlen`；dtm 用 `variable string`（读剩余、写原长），兼容两种报文。
13. **十进制序号错位**：C 宏注释 `0x5C //91`（实际 92）、`0x5D //92`（实际 93）、`0x65 //99`（实际 101）；代码一律以十六进制 serviceType 为准，`platform_java` 中 `serviceType == 99` 的告警规则是历史逻辑，与本协议无关。

## 5. 黄金向量（已落地为 `CodecGoldenVectorTest`）

| 协议 | 字节 | 断言 |
|---|---|---|
| 0x03 | `1A 01 0F 0A 1E 2D` | year=26/month=1/date=15/hour=10/minute=30/second=45，普通二进制 |
| 0x65 | `01 02` | appType=1, action=2 |
| 0x0A | `00` + `update gateway config success` | result=0，message 原样 |
| 0x06 | 合成 117B | `[23..24]` heartbeatInterval 小端 `34 12` |
| 0x08 | 合成 80B | `[63..64]` systemServerPort 大端 `12 34` |
| 0x5C | 合成 391B | `[354..357]` packageSize 小端 `04 03 02 01` |
| 0x5D | 合成 78B | `[64]=appType, [65]=status, [66]=percent` |

## 6. 待补

- **0x60 GATEWAY_GNSS**：Excel 明细表无字段段落、C 侧无实现，当前 `unsupported`，需协议补充后再加 schema。
- `0x18 collectTime` 的 ASCII-HHMMSS 与常规 time6 语义差异，需后台侧按 serviceType 区分。
- 若固件修复上述 C 侧偏移/端序缺陷，需同步更新 `protocol-schemas.json` 与本表。
