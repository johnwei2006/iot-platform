package com.iot.dtm.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtocolRegistryTest {

    private ProtocolRegistry registry;

    @BeforeEach
    public void setUp() throws Exception {
        registry = new ProtocolRegistry();
        registry.init();
    }

    @Test
    public void loadsAll37ProtocolsWithoutDuplicateCodes() {
        assertEquals(37, registry.size());
        assertEquals(registry.getVersion(), "5.0");
        Set<Integer> codes = new HashSet<Integer>();
        for (ProtocolSpec spec : registry.all()) {
            assertTrue(codes.add(spec.code()), "duplicate " + spec.getHex());
            assertNotNull(spec.getName());
            assertTrue(spec.getDirection().length() > 0, "missing direction " + spec.getHex());
            assertTrue(spec.getTransactionType().length() > 0, "missing transactionType " + spec.getHex());
        }
    }

    @Test
    public void enumAndSchemaAreOneToOne() {
        for (ServiceType type : ServiceType.values()) {
            ProtocolSpec spec = registry.get(type);
            assertNotNull(spec, "missing schema for " + type);
            assertEquals(type.getProtocolName(), spec.getName());
            assertEquals(type.getDirection(), spec.getDirection());
            assertEquals(type.getTransactionType(), spec.getTransactionType());
            assertEquals(type.isEmpty(), spec.isEmpty());
            assertEquals(type.isUnsupported(), spec.isUnsupported());
        }
    }

    @Test
    public void gnssIsRegisteredButMarkedUnsupported() {
        ProtocolSpec gnss = registry.get(ServiceType.GATEWAY_GNSS);
        assertNotNull(gnss);
        assertTrue(gnss.isUnsupported());
        assertTrue(gnss.getFields().isEmpty());
    }

    @Test
    public void emptyProtocolsHaveNoFields() {
        for (ServiceType type : ServiceType.values()) {
            if (type.isEmpty()) {
                assertTrue(registry.get(type).getFields().isEmpty(), type.name());
            }
        }
    }
}
