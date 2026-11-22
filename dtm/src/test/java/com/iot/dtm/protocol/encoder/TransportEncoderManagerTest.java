package com.iot.dtm.protocol.encoder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.protocol.CodecException;
import com.iot.dtm.protocol.DecodeResult;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ProtocolTestSupport;
import com.iot.dtm.protocol.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** 编码器管理器：37 条全覆盖、按 DeviceProfile 校验、JSON 透传与启动校验。 */
public class TransportEncoderManagerTest {

    private ProtocolRegistry registry;
    private TransportEncoderManager manager;
    private DeviceProfile binary;

    @BeforeEach
    public void setUp() throws Exception {
        registry = new ProtocolRegistry();
        registry.init();
        manager = ProtocolTestSupport.manager(registry);
        binary = ProtocolTestSupport.profile(2, 1, 0);
    }

    @Test
    public void registersAll37Encoders() {
        assertEquals(ServiceType.values().length, manager.size());
        for (ServiceType type : ServiceType.values()) {
            assertNotNull(manager.getEncoder(type.getCode()), "missing encoder for " + type.name());
            assertEquals(type, manager.getEncoder(type.getCode()).serviceType());
        }
    }

    @Test
    public void missingEncoderFailsFastAtStartup() {
        List<TransportEncoder> encoders = encoders();
        encoders.removeIf(e -> e.serviceType() == ServiceType.GATEWAY_REBOOT);
        try {
            new TransportEncoderManager(encoders, registry, new JsonPassThroughCodec());
            fail("expected startup validation failure");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("GATEWAY_REBOOT"));
        }
    }

    @Test
    public void duplicateEncoderFailsFastAtStartup() {
        List<TransportEncoder> encoders = encoders();
        encoders.add(new SchemaTransportEncoder(ServiceType.GATEWAY_HEART_BEAT, registry,
                new CodecProperties()));
        try {
            new TransportEncoderManager(encoders, registry, new JsonPassThroughCodec());
            fail("expected duplicate validation failure");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("duplicate"));
        }
    }

    @Test
    public void unknownServiceTypeIsUnsupported() {
        DecodeResult r = manager.decode(binary, 0x02, new byte[]{1});
        assertEquals(DecodeResult.Status.UNSUPPORTED, r.getStatus());
        assertTrue(r.getReason().contains("unknown serviceType"));
    }

    @Test
    public void encryptedOrVersionOrEncodingIsUnsupported() {
        assertEquals(DecodeResult.Status.UNSUPPORTED,
                manager.decode(ProtocolTestSupport.profile(2, 1, 1), 101, new byte[]{1, 2}).getStatus());
        assertEquals(DecodeResult.Status.UNSUPPORTED,
                manager.decode(ProtocolTestSupport.profile(2, 2, 0), 101, new byte[]{1, 2}).getStatus());
        assertEquals(DecodeResult.Status.UNSUPPORTED,
                manager.decode(ProtocolTestSupport.profile(9, 1, 0), 101, new byte[]{1, 2}).getStatus());
    }

    @Test
    public void jsonEncodingPassesThroughWithoutBinarySchema() {
        DeviceProfile json = ProtocolTestSupport.profile(1, 1, 0);
        ObjectNode sample = ProtocolTestSupport.sample(registry.get(ServiceType.GATEWAY_REBOOT).getFields());
        byte[] bytes = manager.encode(json, 101, sample);
        DecodeResult r = manager.decode(json, 101, bytes);
        assertEquals(DecodeResult.Status.OK, r.getStatus());
        assertEquals(1, r.getDecoded().path("appType").asInt());
        assertEquals(1, r.getDecoded().path("action").asInt());
    }

    @Test
    public void unknownServiceTypeEncodeThrows() {
        try {
            manager.encode(binary, 0x02, ProtocolTestSupport.sample(
                    registry.get(ServiceType.GATEWAY_REBOOT).getFields()));
            fail("expected CodecException");
        } catch (CodecException expected) {
            assertTrue(expected.getMessage().contains("unknown serviceType"));
        }
    }

    private List<TransportEncoder> encoders() {
        List<TransportEncoder> list = new ArrayList<TransportEncoder>();
        for (ServiceType type : ServiceType.values()) {
            list.add(new SchemaTransportEncoder(type, registry, new CodecProperties()));
        }
        return list;
    }
}
