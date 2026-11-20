package com.iot.platform.service;

import com.iot.platform.mq.DownlinkMessage;
import com.iot.platform.service.downlink.DownlinkHandler;
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
 * 下行处理编排：按 @Order 顺序执行所有 {@link DownlinkHandler}，统一记录步骤、耗时与失败。
 *
 * <p>下行是同步发送链路，没有旁路：任一步骤失败都会抛异常，由 REST 层转换为 5xx。
 * 新增下行处理步骤只需新增一个 DownlinkHandler Bean，不需要改动此类。
 */
@Service
public class DownlinkService {

    private static final Logger log = LoggerFactory.getLogger(DownlinkService.class);

    private final List<DownlinkHandler> handlers;

    @Autowired
    public DownlinkService(ObjectProvider<DownlinkHandler> provider) {
        this(provider == null ? Collections.<DownlinkHandler>emptyList()
                : provider.orderedStream().collect(Collectors.<DownlinkHandler>toList()));
    }

    public DownlinkService(List<DownlinkHandler> handlers) {
        this.handlers = handlers == null
                ? Collections.<DownlinkHandler>emptyList()
                : new ArrayList<DownlinkHandler>(handlers);
    }

    public void send(DownlinkMessage message) {
        for (DownlinkHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            long start = System.currentTimeMillis();
            try {
                handler.handle(message);
                log.debug("downlink step ok: step={} deviceId={} serviceType={} costMs={}",
                        handler.name(), message == null ? "-" : message.getDeviceId(),
                        message == null ? -1 : message.getServiceType(),
                        System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.error("downlink step failed: step={} deviceId={} serviceType={} costMs={} reason={}",
                        handler.name(), message == null ? "-" : message.getDeviceId(),
                        message == null ? -1 : message.getServiceType(),
                        System.currentTimeMillis() - start, e.getMessage(), e);
                throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
        }
    }

    public List<DownlinkHandler> handlers() {
        return Collections.unmodifiableList(handlers);
    }
}
