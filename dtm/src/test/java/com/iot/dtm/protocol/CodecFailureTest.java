package com.iot.dtm.protocol;

import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 容错：未知类型、空载荷、截断、加密、版本、超长计数都必须 fail-open 而不是抛 MQ 异常。 */
public class CodecFailureTest {

    private ProtocolRegistry registry;
    private TransportEncoderManager manager;
    private DeviceProfile binaryProfile;

    @Before
    public void setUp() throws Exception {
        registry = new ProtocolRegistry();
        registry.init();
        manager = ProtocolTestSupport.manager(registry);
        binaryProfile = ProtocolTestSupport.profile(2, 1, 0);
    }

    @Test
    public void unknownServiceTypeIsUnsupported() {
        DecodeResult r = manager.decode(binaryProfile, 2, "1234567890".getBytes());
        assertEquals(DecodeResult.Status.UNSUPPORTED, r.getStatus());
        assertTrue(r.getReason().contains("unknown serviceType"));
    }

    @Test
    public void gnssWithoutSchemaIsUnsupported() {
        DecodeResult r = manager.decode(binaryProfile, 0x60, new byte[]{1, 2, 3});
        assertEquals(DecodeResult.Status.UNSUPPORTED, r.getStatus());
        assertEquals("UNSUPPORTED", r.getStatus().name());
    }

    @Test
    public void emptyPayloadProtocolReturnsEmpty() {
        DecodeResult r = manager.decode(binaryProfile, 0x01, new byte[0]);
        assertEquals(DecodeResult.Status.EMPTY, r.getStatus());
        assertEquals("GATEWAY_HEART_BEAT", r.getProtocolName());
    }

    @Test
    public void unexpectedBytesInEmptyProtocolIsFailed() {
        DecodeResult r = manager.decode(binaryProfile, 0x01, new byte[]{1});
        assertEquals(DecodeResult.Status.FAILED, r.getStatus());
    }

    @Test
    public void truncatedPayloadIsFailed() {
        DecodeResult r = manager.decode(binaryProfile, 0x65, new byte[]{1});
        assertEquals(DecodeResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReason().contains("truncated"));
    }

    @Test
    public void encryptedPayloadIsUnsupported() {
        DecodeResult r = manager.decode(ProtocolTestSupport.profile(2, 1, 1), 0x65, new byte[]{1, 2});
        assertEquals(DecodeResult.Status.UNSUPPORTED, r.getStatus());
        assertTrue(r.getReason().contains("encrypted"));
    }

    @Test
    public void unsupportedProtocolVersionIsRejected() {
        DecodeResult r = manager.decode(ProtocolTestSupport.profile(2, 2, 0), 0x65, new byte[]{1, 2});
        assertEquals(DecodeResult.Status.UNSUPPORTED, r.getStatus());
    }

    @Test
    public void unsupportedEncodingIsRejected() {
        DecodeResult r = manager.decode(ProtocolTestSupport.profile(9, 1, 0), 0x65, new byte[]{1, 2});
        assertEquals(DecodeResult.Status.UNSUPPORTED, r.getStatus());
    }

    @Test
    public void oversizedPayloadIsFailed() {
        CodecProperties props = new CodecProperties();
        props.setMaxPayloadBytes(4);
        TransportEncoderManager limited = ProtocolTestSupport.manager(registry, props);
        DecodeResult r = limited.decode(binaryProfile, 0x65, new byte[]{1, 2, 3, 4, 5});
        assertEquals(DecodeResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReason().contains("exceeds limit"));
    }

    @Test
    public void repeatCountBeyondLimitIsFailed() {
        CodecProperties props = new CodecProperties();
        props.setMaxRepeatItems(1);
        TransportEncoderManager limited = ProtocolTestSupport.manager(registry, props);
        byte[] payload = new byte[1 + 2 * 16 + 6];
        payload[0] = 2;
        Arrays.fill(payload, 1, payload.length, (byte) 0x11);
        DecodeResult r = limited.decode(binaryProfile, 0x0C, payload);
        assertEquals(DecodeResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReason().contains("exceeds limit"));
    }
}
