package com.proxy.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Same functionality as in
 * {@link java.util.concurrent.Executors#defaultThreadFactory()} but with the
 * ability to set name of thread pool.
 */
public class CustomDefaultThreadFactory implements ThreadFactory {

	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;

	public CustomDefaultThreadFactory(final String threadPoolName) {
		final SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		namePrefix = threadPoolName + "-thread-";
	}

	public Thread newThread(final Runnable r) {
		final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
		if (t.isDaemon())
			t.setDaemon(false);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		System.out.println("new thread : " + t.getName());
		return t;
	}
}
