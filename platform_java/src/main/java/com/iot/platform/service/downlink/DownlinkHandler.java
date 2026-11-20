package com.iot.platform.service.downlink;

import com.iot.platform.mq.DownlinkMessage;

/**
 * 下行处理管线中的一个步骤。
 *
 * <p>每个实现只做一件事，由 {@code DownlinkService} 按 @Order 顺序执行。
 * 下行是同步发送链路，没有旁路：任何步骤失败都会抛异常并由 REST 层转换为 5xx。
 */
public interface DownlinkHandler {

    /** 步骤名称，用于日志与问题定位。 */
    String name();

    /** 执行该步骤；实现内部不吞异常，由编排层统一记录并传播。 */
    void handle(DownlinkMessage message);
}
