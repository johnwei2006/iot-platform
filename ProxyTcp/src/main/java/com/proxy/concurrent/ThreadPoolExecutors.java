package com.proxy.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for setting up all thread pool executors 
 */
@Service("threadPoolExecutors")
public class ThreadPoolExecutors {

	private static final String UPLINK_THREAD_POOL_CORE_SIZE = "uplink.threadpool.coreSize";
	private static final String DEFAULT_UPLINK_THREAD_POOL_CORE_SIZE = "30";
	private static final String UPLINK_THREAD_POOL_MAX_SIZE = "uplink.threadpool.maxSize";
	private static final String DEFAULT_UPLINK_THREAD_POOL_MAX_SIZE = "500";
	private static final String UPLINK_THREAD_POOL_TASK_QUEUE_SIZE = "uplink.threadpool.taskQueueSize";
	private static final String DEFAULT_UPLINK_THREAD_POOL_TASK_QUEUE_SIZE  = "500";

	private static final String DOWNLINK_THREAD_POOL_CORE_SIZE  = "downlink.threadpool.coreSize";
	private static final String DEFAULT_DOWNLINK_THREAD_POOL_CORE_SIZE = "3";

	private static final String RESEND_SCANNER_POOL_SIZE = "resend.downlink.scanner.pool.thread.size";
	private static final String DEFAULT_RESEND_SCANNER_POOL_SIZE = "5";

	private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired 
    Environment environment; 

	private ThreadPoolExecutor uplinkThreadPoolExecutor;
	private ScheduledThreadPoolExecutor downlinkScheduledThreadPoolExecutor;
	private ScheduledThreadPoolExecutor resendScheduledThreadPoolExecutor;

	private int uplinkMaximumTaskQueueSize;

	@PostConstruct
	public void init() {
		createUplinkThreadPoolExecutor();
		createDownlinkScheduledThreadPoolExecutor();
		createResendScheduledThreadPoolExecutor();
	}

	@PreDestroy
	public void destroy() {
		logger.info("Stopping thread pool executor");
		if (uplinkThreadPoolExecutor != null) {
			uplinkThreadPoolExecutor.shutdown();
		}
		if (downlinkScheduledThreadPoolExecutor != null) {
			downlinkScheduledThreadPoolExecutor.shutdown();
		}
		if (resendScheduledThreadPoolExecutor != null) {
			resendScheduledThreadPoolExecutor.shutdown();
		}
	}

	private void createUplinkThreadPoolExecutor() {
		String corePoolSizeStr = environment.getProperty(UPLINK_THREAD_POOL_CORE_SIZE);
		if (corePoolSizeStr == null)
			corePoolSizeStr = DEFAULT_UPLINK_THREAD_POOL_CORE_SIZE;
		int corePoolSize = Integer.parseInt(corePoolSizeStr);
		String maximumPoolSizeStr = environment.getProperty(UPLINK_THREAD_POOL_MAX_SIZE);
		if (maximumPoolSizeStr == null)
			maximumPoolSizeStr = DEFAULT_UPLINK_THREAD_POOL_MAX_SIZE;
		int maximumPoolSize = Integer.parseInt(maximumPoolSizeStr);
		String uplinkMaximumTaskQueueSizeStr= environment.getProperty(UPLINK_THREAD_POOL_TASK_QUEUE_SIZE);
		if (uplinkMaximumTaskQueueSizeStr == null)
			uplinkMaximumTaskQueueSizeStr = DEFAULT_UPLINK_THREAD_POOL_TASK_QUEUE_SIZE;
		uplinkMaximumTaskQueueSize = Integer.parseInt(uplinkMaximumTaskQueueSizeStr);

//		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue(uplinkMaximumTaskQueueSize, true);
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		final long keepAliveTimeInSeconds = 60L;
		uplinkThreadPoolExecutor = new MdcThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTimeInSeconds,
				TimeUnit.SECONDS, queue, new CustomDefaultThreadFactory("uplink-pool"));

//		logger.info("MdcThreadPoolExecutor, corePoolSize = " + corePoolSize + ", maximumPoolSize = " + maximumPoolSize);
//		logger.info("MdcThreadPoolExecutor: " + uplinkThreadPoolExecutor.toString());
		uplinkThreadPoolExecutor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
				logger.error("Uplink message queue is full. Uplink message is rejected.");
				throw new RejectedExecutionException("Uplink message queue is full. Uplink message is rejected.");
			}
		});

//		logThreadPoolExecutor("Uplink ThreadPoolExecutor", uplinkThreadPoolExecutor, uplinkMaximumTaskQueueSize);
	}

	private void createDownlinkScheduledThreadPoolExecutor() {
		String corePoolSizeStr = environment.getProperty(DOWNLINK_THREAD_POOL_CORE_SIZE);
		if (corePoolSizeStr == null)
			corePoolSizeStr = DEFAULT_DOWNLINK_THREAD_POOL_CORE_SIZE;
		final int corePoolSize = Integer.parseInt(corePoolSizeStr);

		downlinkScheduledThreadPoolExecutor = new MdcScheduledThreadPoolExecutor(corePoolSize,
				new CustomDefaultThreadFactory("downlink-pool"));
		downlinkScheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		final int maximumTaskQueueSize = Integer.MAX_VALUE;

//		logThreadPoolExecutor("Downlink ScheduledThreadPoolExecutor", downlinkScheduledThreadPoolExecutor,maximumTaskQueueSize);
	}

	private void createResendScheduledThreadPoolExecutor() {
		String poolSizeStr = environment.getProperty(RESEND_SCANNER_POOL_SIZE);
		if (poolSizeStr == null)
			poolSizeStr = DEFAULT_RESEND_SCANNER_POOL_SIZE;
		final int poolSize = Integer.parseInt(poolSizeStr);
		if (poolSize > 0) {
			resendScheduledThreadPoolExecutor = new MdcScheduledThreadPoolExecutor(poolSize,
					new CustomDefaultThreadFactory("resend-pool"));
			resendScheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		} else {
			logger.debug("Property " + RESEND_SCANNER_POOL_SIZE
					+ " is not set, will not start without any threads. Must be at least two in size to function properly");
		}
	}

	private void logThreadPoolExecutor(final String threadPoolExecutorName, final ThreadPoolExecutor threadPoolExecutor,
			final int maximumTaskQueueSize) {
		final StringBuilder msg = new StringBuilder();
		msg.append(threadPoolExecutorName).append(":\n");
		msg.append("\t").append("ThreadPoolCoreSize=").append(threadPoolExecutor.getCorePoolSize()).append("\n");
		msg.append("\t").append("ThreadPoolMaxSize=").append(getMaxPoolSize(threadPoolExecutor)).append("\n");
		msg.append("\t").append("TaskQueueSize=").append(threadPoolExecutor.getQueue().size()).append("\n");
		msg.append("\t").append("MaximumTaskQueueSize=").append(maximumTaskQueueSize).append("\n");

//		logger.info(msg.toString());
	}

	private int getMaxPoolSize(final ThreadPoolExecutor threadPoolExecutor) {
		return (threadPoolExecutor instanceof ScheduledThreadPoolExecutor) ? threadPoolExecutor.getCorePoolSize()
				: threadPoolExecutor.getMaximumPoolSize();
	}

	public ThreadPoolExecutor getUplinkThreadPoolExecutor() {
		return uplinkThreadPoolExecutor;
	}

//	public ScheduledThreadPoolExecutor getDownlinkScheduledThreadPoolExecutor() {
//		return downlinkScheduledThreadPoolExecutor;
//	}
//
//	public ScheduledThreadPoolExecutor getResendScheduledThreadPoolExecutor() {
//		return resendScheduledThreadPoolExecutor;
//	}
}
