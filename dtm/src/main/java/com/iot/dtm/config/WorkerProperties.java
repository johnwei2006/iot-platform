package com.iot.dtm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 报文处理线程池、发布确认与设备画像缓存参数。 */
@Component
@ConfigurationProperties(prefix = "dtm.worker")
public class WorkerProperties {

    /** 核心线程数。 */
    private int coreSize = 8;
    /** 最大线程数。 */
    private int maxSize = 32;
    /** 有界队列容量；满后 CallerRunsPolicy 在消费者线程执行，形成背压。 */
    private int queueCapacity = 1024;
    /** 空闲线程存活秒数。 */
    private int keepAliveSeconds = 60;
    /** 消费者等待 worker 完成的最长时间（毫秒）。 */
    private long awaitTimeoutMs = 60000L;
    /** 等待 RabbitMQ publisher confirm 的最长时间（毫秒）。 */
    private long confirmTimeoutMs = 10000L;
    /** DeviceProfile 内存缓存上限，超过后清空重建，避免设备数无界增长。 */
    private int profileCacheMax = 10000;

    public int getCoreSize() { return coreSize; }
    public void setCoreSize(int coreSize) { this.coreSize = coreSize; }

    public int getMaxSize() { return maxSize; }
    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public int getQueueCapacity() { return queueCapacity; }
    public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }

    public int getKeepAliveSeconds() { return keepAliveSeconds; }
    public void setKeepAliveSeconds(int keepAliveSeconds) { this.keepAliveSeconds = keepAliveSeconds; }

    public long getAwaitTimeoutMs() { return awaitTimeoutMs; }
    public void setAwaitTimeoutMs(long awaitTimeoutMs) { this.awaitTimeoutMs = awaitTimeoutMs; }

    public long getConfirmTimeoutMs() { return confirmTimeoutMs; }
    public void setConfirmTimeoutMs(long confirmTimeoutMs) { this.confirmTimeoutMs = confirmTimeoutMs; }

    public int getProfileCacheMax() { return profileCacheMax; }
    public void setProfileCacheMax(int profileCacheMax) { this.profileCacheMax = profileCacheMax; }
}
