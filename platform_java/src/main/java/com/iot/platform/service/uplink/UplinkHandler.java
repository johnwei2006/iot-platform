package com.iot.platform.service.uplink;

import com.iot.platform.mq.UplinkMessage;

/**
 * 上行处理管线中的一个步骤。
 *
 * <p>每个实现只做一件事，由 {@code UplinkProcessingService} 按 @Order 顺序执行。
 * {@link #critical()} 决定失败语义：
 * <ul>
 *   <li>true：主链路步骤，异常会向上传播，触发 RabbitMQ 重试 / 死信；</li>
 *   <li>false：旁路步骤，异常只告警降级，不影响消息 ACK。</li>
 * </ul>
 */
public interface UplinkHandler {

    /** 步骤名称，用于日志与问题定位。 */
    String name();

    /** 是否为必须成功的主链路步骤。 */
    boolean critical();

    /** 执行该步骤；实现内部不应吞掉自身异常（由编排层统一处理）。 */
    void handle(UplinkMessage message);
}
