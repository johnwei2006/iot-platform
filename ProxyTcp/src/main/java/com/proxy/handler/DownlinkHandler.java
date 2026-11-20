package com.proxy.handler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proxy.concurrent.ThreadPoolExecutors;

@Service("TransportDownlinkHandler")
public class DownlinkHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private DownlinkProcessor downlinkProcessor;

	@Autowired
	private ThreadPoolExecutors threadPoolExecutors;

	private ThreadPoolExecutor downlinkExecutor;

//	@PostConstruct
//	private void init() {
//		downlinkExecutor = threadPoolExecutors.getDownlinkScheduledThreadPoolExecutor();
//	}

	public void process() {
		try {
			downlinkExecutor.submit(new DownlinkMessageProcessorTask());
			logger.info("core pool size = " + downlinkExecutor.getCorePoolSize() + ", PoolSize = "
					+ downlinkExecutor.getPoolSize() + ", " + downlinkExecutor.getActiveCount() + ", "
					+ downlinkExecutor.getTaskCount());
		} catch (Exception e) {
			// Send error response to vehicle
			// sendErrorResponse(uplinkMessage);
			e.printStackTrace();
		}
	}

	private class DownlinkMessageProcessorTask implements Runnable {
		private DownlinkMessageProcessorTask() {
			// mdcContextMap = MDC.getCopyOfContextMap();
		}

		public void run() {
			try {
				// MDC.setContextMap(mdcContextMap);

			} catch (final Throwable e) {
				e.printStackTrace();
			} finally {
				// MDC.clear();
			}
		}
	}
}
