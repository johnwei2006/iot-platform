package com.iot.platform.mq;

import com.iot.platform.service.uplink.UplinkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 上行队列处理层：按 @Order 顺序执行所有 {@link UplinkHandler}，统一记录步骤、耗时与失败。
 *
 * <p>与 dtm 的 mq 包职责对齐：队列层负责消费边界与编排，业务处理由 service/uplink/ 的处理器完成。
 * 主链路步骤（critical）失败会抛异常触发 RabbitMQ 重试 / 死信；旁路步骤（non-critical）
 * 失败只告警降级，保证 ACK 成功。新增上行处理步骤只需新增一个 UplinkHandler Bean。
 */
@Service
public class UplinkProcessingService {

    private static final Logger log = LoggerFactory.getLogger(UplinkProcessingService.class);

    private final List<UplinkHandler> handlers;

    @Autowired
    public UplinkProcessingService(ObjectProvider<UplinkHandler> provider) {
        this(provider == null ? Collections.<UplinkHandler>emptyList()
                : provider.orderedStream().collect(Collectors.<UplinkHandler>toList()));
    }

    public UplinkProcessingService(List<UplinkHandler> handlers) {
        this.handlers = handlers == null
                ? Collections.<UplinkHandler>emptyList()
                : new ArrayList<UplinkHandler>(handlers);
    }

    public void process(UplinkMessage message) {
        for (UplinkHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            long start = System.currentTimeMillis();
            try {
                handler.handle(message);
                log.debug("uplink step ok: step={} deviceId={} serviceType={} costMs={}",
                        handler.name(), message == null ? "-" : message.getDeviceId(),
                        message == null ? -1 : message.getServiceType(),
                        System.currentTimeMillis() - start);
            } catch (Exception e) {
                long cost = System.currentTimeMillis() - start;
                if (handler.critical()) {
                    log.error("uplink critical step failed: step={} deviceId={} serviceType={} costMs={} reason={}",
                            handler.name(), message == null ? "-" : message.getDeviceId(),
                            message == null ? -1 : message.getServiceType(), cost, e.getMessage(), e);
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                }
                log.warn("uplink side step failed (degraded, uplink unaffected): step={} deviceId={} "
                                + "serviceType={} costMs={} reason={}",
                        handler.name(), message == null ? "-" : message.getDeviceId(),
                        message == null ? -1 : message.getServiceType(), cost, e.getMessage());
            }
        }
    }

    public List<UplinkHandler> handlers() {
        return Collections.unmodifiableList(handlers);
    }
}
