package com.iot.dtm.mq;

import com.iot.dtm.config.MqConstants;
import com.iot.dtm.config.WorkerProperties;
import com.iot.dtm.service.DownlinkService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 下行 MQ 入口：platform.downlink -> 线程池 -> DownlinkService -> proxy.downlink。 */
@Component
public class DownlinkConsumer extends AbstractAckConsumer {

    private final DownlinkService downlinkService;

    @Autowired
    public DownlinkConsumer(DownlinkService downlinkService,
                            @Qualifier("dtmWorkerExecutor") ThreadPoolTaskExecutor executor,
                            WorkerProperties properties) {
        super(executor, properties);
        this.downlinkService = downlinkService;
    }

    @RabbitListener(queues = MqConstants.PLATFORM_DOWNLINK)
    public void onDownlink(Message message, Channel channel) throws IOException {
        execute(message, channel, () -> downlinkService.process(message), "downlink");
    }
}
