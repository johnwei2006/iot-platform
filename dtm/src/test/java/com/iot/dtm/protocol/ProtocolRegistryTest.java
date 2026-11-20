package com.iot.dtm.protocol;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProtocolRegistryTest {

    private ProtocolRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new ProtocolRegistry();
        registry.init();
    }

    @Test
    public void loadsAll37ProtocolsWithoutDuplicateCodes() {
        assertEquals(37, registry.size());
        assertEquals("5.0", registry.getVersion());
        Set<Integer> codes = new HashSet<Integer>();
        for (ProtocolSpec spec : registry.all()) {
            assertTrue("duplicate " + spec.getHex(), codes.add(spec.code()));
            assertNotNull(spec.getName());
            assertTrue("missing direction " + spec.getHex(), spec.getDirection().length() > 0);
            assertTrue("missing transactionType " + spec.getHex(), spec.getTransactionType().length() > 0);
        }
    }

    @Test
    public void enumAndSchemaAreOneToOne() {
        for (ServiceType type : ServiceType.values()) {
            ProtocolSpec spec = registry.get(type);
            assertNotNull("missing schema for " + type, spec);
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
                assertTrue(type.name(), registry.get(type).getFields().isEmpty());
            }
        }
    }
}
