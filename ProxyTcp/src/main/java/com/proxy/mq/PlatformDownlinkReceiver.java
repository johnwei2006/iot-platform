package com.proxy.mq;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.ServiceRequest;
import com.proxy.transport.TransportDownlinkProcessor;
import com.proxy.utils.ShowFlakeIdGanerator;

/**
 * 平台来源下行 MQ 消费者（ProxyTcp 唯一 TCP 接入层的下行入口之一）。
 *
 * <p>消费 {@code iot.exchange / proxy.downlink}（由 platform_java 经 dtm 透传下发，
 * body 为与 {@link ServiceRequest} 字段对齐的 DownlinkMessage JSON）。解析后映射为
 * {@link DownlinkTransportMessage}，交给已验证可用的内部下行链路
 * （{@code TransportDownlinkProcessor -> sendQueue -> NettyDownlinkSender -> 设备}）。
 *
 * <p>使用声明式 {@link QueueBinding} 幂等创建队列/交换器/绑定，与 dtm、platform_java
 * 连接同一 broker 即生效，且与启动顺序无关。
 */
@Component
public class PlatformDownlinkReceiver {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TransportDownlinkProcessor transportDownlinkProcessor;

    private final Gson gson = com.proxy.utils.JsonUtils.gson();

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "proxy.downlink", durable = "true", arguments = {
                    @Argument(name = "x-dead-letter-exchange", value = "iot.dlx"),
                    @Argument(name = "x-dead-letter-routing-key", value = "iot.dlq")}),
            exchange = @Exchange(value = "iot.exchange", type = ExchangeTypes.TOPIC),
            key = "proxy.downlink"))
    public void onDownlink(Message message) {
        handleDownlink(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    /**
     * 解析 + 映射 + 派发，无 MQ 依赖，便于单元测试。
     */
    public void handleDownlink(String json) {
        if (json == null || json.trim().isEmpty()) {
            logger.warn("Empty downlink payload, ignore");
            return;
        }
        ServiceRequest req;
        try {
            req = gson.fromJson(json, ServiceRequest.class);
        } catch (Exception e) {
            logger.error("Downlink JSON parse error: " + e.getMessage());
            return;
        }
        if (req == null || req.getDeviceId() == null) {
            logger.error("Downlink missing deviceId, ignore");
            return;
        }
        try {
            DownlinkTransportMessage transportMessage = new DownlinkTransportMessage();
            transportMessage.setEventId(req.getEventId());

            ShowFlakeIdGanerator snowFlake = new ShowFlakeIdGanerator(2, 3);
            transportMessage.setMessageId(snowFlake.nextRandId(4));
            transportMessage.setMessageCreationTime(new Date());
            transportMessage.setServiceData(req.getServiceData());
            transportMessage.setServiceDataEncoding(req.getServiceDataEncoding());
            transportMessage.setServiceDataProtocolVersion(req.getServiceDataProtocolVersion());
            transportMessage.setServiceDataEncryption(req.getServiceEncryption());
            transportMessage.setServiceType(req.getServiceType());
            transportMessage.setServiceVersion(req.getServiceVersion());
            transportMessage.setDeviceId(req.getDeviceId());
            transportMessage.setNodeId(req.getNodeId());

            transportDownlinkProcessor.process(transportMessage);
        } catch (Exception e) {
            logger.error("Downlink process error for device " + req.getDeviceId() + ": " + e.getMessage());
        }
    }
}
