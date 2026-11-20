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
import com.proxy.handler.UplinkHandler;
import com.proxy.model.Message;

@Service("UplinkHandler")
public class UplinkHandler{

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private UplinkProcessor uplinkProcessor;

	@Autowired
	private ThreadPoolExecutors threadPoolExecutors;

	private ThreadPoolExecutor uplinkExecutor;

	@PostConstruct
	private void init() {
		uplinkExecutor = threadPoolExecutors.getUplinkThreadPoolExecutor();
	}

	public void process(Message uplinkMessage) throws Exception{
		uplinkExecutor.submit(new UplinkMessageProcessorTask(uplinkMessage));
		logger.debug("core pool size = " + uplinkExecutor.getCorePoolSize() + ", PoolSize = "
					+ uplinkExecutor.getPoolSize() + ", " + uplinkExecutor.getActiveCount() + ", "
					+ uplinkExecutor.getTaskCount());
	}

	private class UplinkMessageProcessorTask implements Runnable {

		private final Message uplinkMessage;
		// private final Map<String, String> mdcContextMap;

		private UplinkMessageProcessorTask(final Message uplinkMessage) {
			this.uplinkMessage = uplinkMessage;
			// mdcContextMap = MDC.getCopyOfContextMap();
		}

		public void run() {
			try {
				// MDC.setContextMap(mdcContextMap);
//				logger.info("start to send to uplinkProcessor");

				uplinkProcessor.process(uplinkMessage);

			} catch (final Throwable e) {
				e.printStackTrace();
			} finally {
				// MDC.clear();
			}
		}
	}
}
