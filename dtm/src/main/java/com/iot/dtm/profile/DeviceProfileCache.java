package com.iot.dtm.profile;

import com.iot.dtm.config.WorkerProperties;
import com.iot.dtm.model.DeviceProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 deviceId 缓存最近一次 DeviceProfile。
 * 报文缺省字段（null）复用缓存值；仍缺省时由 {@link DeviceProfile} 提供默认值。
 * 无持久化，重启后首条报文按默认值处理。
 */
@Component
public class DeviceProfileCache {

    private static final Logger log = LoggerFactory.getLogger(DeviceProfileCache.class);

    private final ConcurrentHashMap<String, DeviceProfile> cache =
            new ConcurrentHashMap<String, DeviceProfile>();
    private final WorkerProperties properties;

    @Autowired
    public DeviceProfileCache(WorkerProperties properties) {
        this.properties = properties;
    }

    /**
     * 合并并写入缓存。入参只读，返回值是新的不可共享实例（调用方可安全使用）。
     */
    public DeviceProfile merge(String deviceId, DeviceProfile incoming) {
        DeviceProfile base = deviceId == null ? null : cache.get(deviceId);
        DeviceProfile merged = new DeviceProfile();
        merged.setDeviceId(deviceId != null ? deviceId : (base == null ? null : base.getDeviceId()));
        merged.setServiceVersion(firstNonNull(incoming == null ? null : incoming.getServiceVersion(),
                base == null ? null : base.getServiceVersion()));
        merged.setServiceDataEncoding(firstNonNull(incoming == null ? null : incoming.getServiceDataEncoding(),
                base == null ? null : base.getServiceDataEncoding()));
        merged.setServiceDataProtocolVersion(firstNonNull(
                incoming == null ? null : incoming.getServiceDataProtocolVersion(),
                base == null ? null : base.getServiceDataProtocolVersion()));
        merged.setServiceEncryption(firstNonNull(incoming == null ? null : incoming.getServiceEncryption(),
                base == null ? null : base.getServiceEncryption()));

        if (deviceId != null && !deviceId.trim().isEmpty()) {
            evictIfNeeded(deviceId);
            cache.put(deviceId, merged);
        }
        return merged;
    }

    public DeviceProfile get(String deviceId) {
        return deviceId == null ? null : cache.get(deviceId);
    }

    public void invalidate(String deviceId) {
        if (deviceId != null) {
            cache.remove(deviceId);
        }
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }

    private void evictIfNeeded(String deviceId) {
        int max = properties.getProfileCacheMax();
        if (max > 0 && cache.size() >= max && !cache.containsKey(deviceId)) {
            cache.clear();
            log.warn("device profile cache reached {} entries, cleared to stay bounded", max);
        }
    }

    private static <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }
}
