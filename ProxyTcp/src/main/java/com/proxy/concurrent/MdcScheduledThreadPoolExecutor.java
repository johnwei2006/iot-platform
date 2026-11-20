package com.proxy.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import org.slf4j.MDC;

/**
 * A SLF4J MDC-compatible {@link ScheduledThreadPoolExecutor} that sets MDC data before each task
 * appropriately.
 */
class MdcScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

  MdcScheduledThreadPoolExecutor(final int corePoolSize, final ThreadFactory threadFactory) {
    super(corePoolSize, threadFactory);
  }

  @Override
  public void execute(Runnable command) {
    super.execute(MdcThreadPoolExecutor.wrap(command, MDC.getCopyOfContextMap()));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return super.submit(MdcThreadPoolExecutor.wrap(task, MDC.getCopyOfContextMap()));
  }
}
