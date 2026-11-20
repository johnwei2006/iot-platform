package com.proxy.concurrent;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;

/**
 * A SLF4J MDC-compatible {@link ThreadPoolExecutor} that sets MDC data before each task
 * appropriately.
 */
public class MdcThreadPoolExecutor extends ThreadPoolExecutor {

  MdcThreadPoolExecutor(
      final int corePoolSize,
      final int maximumPoolSize,
      final long keepAliveTime,
      final TimeUnit unit,
      final BlockingQueue<Runnable> workQueue,
      final ThreadFactory threadFactory) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
  }

  @Override
  public void execute(Runnable command) {
    super.execute(wrap(command, MDC.getCopyOfContextMap()));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return super.submit(wrap(task, MDC.getCopyOfContextMap()));
  }

  public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
    return new Runnable() {
      public void run() {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (context == null) {
          MDC.clear();
        } else {
          MDC.setContextMap(context);
        }
        try {
          runnable.run();
        } finally {
          if (previous == null) {
            MDC.clear();
          } else {
            MDC.setContextMap(previous);
          }
        }
      }
    };
  }

  public static <T> Callable<T> wrap(final Callable<T> task, final Map<String, String> context) {
    return new Callable<T>() {

      public T call() throws Exception {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (context == null) {
          MDC.clear();
        } else {
          MDC.setContextMap(context);
        }
        try {
          return task.call();
        } finally {
          if (previous == null) {
            MDC.clear();
          } else {
            MDC.setContextMap(previous);
          }
        }
      }
    };
  }
}
