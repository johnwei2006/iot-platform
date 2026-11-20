package com.proxy.test.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/** 一个 serviceType 的模拟样本，供 CLI 与参数化/循环测试复用。 */
public final class MockServiceCase {

    private final int serviceType;
    private final String name;
    private final String direction;
    private final JsonNode sample;
    private final boolean unsupported;

    public MockServiceCase(int serviceType, String name, String direction,
                           JsonNode sample, boolean unsupported) {
        this.serviceType = serviceType;
        this.name = name;
        this.direction = direction;
        this.sample = sample;
        this.unsupported = unsupported;
    }

    public int getServiceType() {
        return serviceType;
    }

    public String getName() {
        return name;
    }

    public String getDirection() {
        return direction;
    }

    public JsonNode getSample() {
        return sample;
    }

    public boolean isUnsupported() {
        return unsupported;
    }

    @Override
    public String toString() {
        return String.format("0x%02X %s (%s)", serviceType, name, direction);
    }
}
