package com.iot.dtm.mq;

import com.iot.dtm.config.MqConstants;
import com.iot.dtm.config.WorkerProperties;
import com.iot.dtm.service.UplinkService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 上行 MQ 入口：proxy.uplink -> 线程池 -> UplinkService -> platform.uplink。 */
@Component
public class UplinkConsumer extends AbstractAckConsumer {

    private final UplinkService uplinkService;

    @Autowired
    public UplinkConsumer(UplinkService uplinkService,
                          @Qualifier("dtmWorkerExecutor") ThreadPoolTaskExecutor executor,
                          WorkerProperties properties) {
        super(executor, properties);
        this.uplinkService = uplinkService;
    }

    @RabbitListener(queues = MqConstants.PROXY_UPLINK)
    public void onUplink(Message message, Channel channel) throws IOException {
        execute(message, channel, () -> uplinkService.process(message), "uplink");
    }
}
