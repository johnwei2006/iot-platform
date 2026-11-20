package com.iot.platform.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TdEngineConfigTest {

    @Test
    public void nativeJdbcUrlByDefault() {
        TdEngineProperties properties = new TdEngineProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(6030);
        properties.setDatabase("iot");

        assertEquals("jdbc:TAOS://127.0.0.1:6030/iot?charset=UTF-8&locale=en_US.UTF-8&timezone=UTC",
                TdEngineConfig.buildJdbcUrl(properties));
    }

    @Test
    public void restJdbcUrlWhenRestEnabled() {
        TdEngineProperties properties = new TdEngineProperties();
        properties.setHost("47.104.160.158");
        properties.setPort(6041);
        properties.setRest(true);
        properties.setDatabase("iot");

        assertEquals("jdbc:TAOS-RS://47.104.160.158:6041/iot?charset=UTF-8&locale=en_US.UTF-8&timezone=UTC",
                TdEngineConfig.buildJdbcUrl(properties));
    }

    @Test
    public void databaseNameIsSanitized() {
        assertEquals("iotdrop", TdEngineConfig.sanitizeIdentifier("iot;drop"));
        assertEquals("iot", TdEngineConfig.sanitizeIdentifier(null));
    }
}
