package com.iot.dtm.protocol;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 37 条协议（除空载荷与无 schema 的 0x60）全部做 encode -> decode -> re-encode 字节级回归。 */
public class TransportEncoderRoundTripTest {

    private ProtocolRegistry registry;
    private TransportEncoderManager manager;
    private DeviceProfile binaryProfile;

    @BeforeEach
    public void setUp() throws Exception {
        registry = new ProtocolRegistry();
        registry.init();
        manager = ProtocolTestSupport.manager(registry);
        binaryProfile = ProtocolTestSupport.profile(2, 1, 0);
    }

    @Test
    public void allDecodableProtocolsRoundTrip() {
        int checked = 0;
        for (ProtocolSpec spec : registry.all()) {
            if (spec.isEmpty() || spec.isUnsupported()) {
                continue;
            }
            ObjectNode sample = ProtocolTestSupport.sample(spec.getFields());
            byte[] bytes = manager.encode(binaryProfile, spec.code(), sample);
            DecodeResult result = manager.decode(binaryProfile, spec.code(), bytes);
            assertEquals(DecodeResult.Status.OK, result.getStatus(),
                    spec.getName() + " decode status: " + result.getReason());
            byte[] again = manager.encode(binaryProfile, spec.code(), result.getDecoded());
            assertArrayEquals(bytes, again, spec.getName() + " re-encode differs");
            checked++;
        }
        assertEquals(31, checked);
    }

    @Test
    public void jsonEncodedPayloadStaysStructured() {
        DeviceProfile jsonProfile = ProtocolTestSupport.profile(1, 1, 0);
        ObjectNode sample = ProtocolTestSupport.sample(registry.get(ServiceType.GATEWAY_REBOOT).getFields());
        byte[] bytes = manager.encode(jsonProfile, 101, sample);
        DecodeResult result = manager.decode(jsonProfile, 101, bytes);
        assertEquals(DecodeResult.Status.OK, result.getStatus());
        assertEquals(1, result.getDecoded().path("appType").asInt());
    }
}
