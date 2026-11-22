package com.iot.dtm.profile;

import com.iot.dtm.config.WorkerProperties;
import com.iot.dtm.model.DeviceProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DeviceProfile 合并：缺省字段复用缓存、默认值回退、容量上限。 */
public class DeviceProfileCacheTest {

    @Test
    public void missingFieldsReuseCachedProfile() {
        DeviceProfileCache cache = new DeviceProfileCache(new WorkerProperties());
        DeviceProfile first = new DeviceProfile();
        first.setServiceVersion(1);
        first.setServiceDataEncoding(2);
        first.setServiceDataProtocolVersion(1);
        first.setServiceEncryption(0);
        cache.merge("d1", first);

        DeviceProfile merged = cache.merge("d1", new DeviceProfile());
        assertEquals(Integer.valueOf(1), merged.getServiceVersion());
        assertEquals(Integer.valueOf(2), merged.getServiceDataEncoding());
        assertEquals(Integer.valueOf(1), merged.getServiceDataProtocolVersion());
        assertEquals(Integer.valueOf(0), merged.getServiceEncryption());
        assertEquals(merged.getDeviceId(), "d1");
    }

    @Test
    public void absentProfileFallsBackToProtocolDefaults() {
        DeviceProfileCache cache = new DeviceProfileCache(new WorkerProperties());
        DeviceProfile merged = cache.merge("d2", new DeviceProfile());
        assertEquals(2, merged.effectiveServiceDataEncoding());
        assertEquals(1, merged.effectiveServiceDataProtocolVersion());
        assertEquals(0, merged.effectiveServiceEncryption());
        assertNull(merged.getServiceVersion());
    }

    @Test
    public void cacheIsBounded() {
        WorkerProperties properties = new WorkerProperties();
        properties.setProfileCacheMax(2);
        DeviceProfileCache cache = new DeviceProfileCache(properties);
        cache.merge("a", new DeviceProfile());
        cache.merge("b", new DeviceProfile());
        assertEquals(2, cache.size());

        cache.merge("c", new DeviceProfile());
        assertTrue(cache.size() <= 2, "cache should stay bounded");
        assertEquals(1, cache.size());
    }

    @Test
    public void invalidateRemovesEntry() {
        DeviceProfileCache cache = new DeviceProfileCache(new WorkerProperties());
        cache.merge("a", new DeviceProfile());
        cache.merge("b", new DeviceProfile());
        cache.invalidate("a");
        assertEquals(1, cache.size());
    }
}
