package com.iot.dtm.protocol;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** 依据 C 侧编码器/Excel 明细构造的字节级黄金向量，锁定端序与字段偏移。 */
public class CodecGoldenVectorTest {

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
    public void timeSyncResponseUsesSixPlainBinaryBytes() {
        byte[] bytes = new byte[]{0x1A, 0x01, 0x0F, 0x0A, 0x1E, 0x2D};
        DecodeResult r = decode(0x03, bytes);
        assertEquals(DecodeResult.Status.OK, r.getStatus());
        assertEquals(26, r.getDecoded().path("year").asInt());
        assertEquals(1, r.getDecoded().path("month").asInt());
        assertEquals(15, r.getDecoded().path("date").asInt());
        assertEquals(10, r.getDecoded().path("hour").asInt());
        assertEquals(30, r.getDecoded().path("minute").asInt());
        assertEquals(45, r.getDecoded().path("second").asInt());
        assertArrayEquals(bytes, encode(0x03, r.getDecoded()));
    }

    @Test
    public void rebootIsTwoBytes() {
        DecodeResult r = decode(0x65, new byte[]{0x01, 0x02});
        assertEquals(DecodeResult.Status.OK, r.getStatus());
        assertEquals(1, r.getDecoded().path("appType").asInt());
        assertEquals(2, r.getDecoded().path("action").asInt());
    }

    @Test
    public void configResponseReadsVariableLengthMessage() {
        String message = "update gateway config success";
        byte[] bytes = new byte[1 + message.length()];
        bytes[0] = 0;
        byte[] text = message.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(text, 0, bytes, 1, text.length);
        DecodeResult r = decode(0x0A, bytes);
        assertEquals(DecodeResult.Status.OK, r.getStatus());
        assertEquals(0, r.getDecoded().path("result").asInt());
        assertEquals(message, r.getDecoded().path("message").asText());
        assertArrayEquals(bytes, encode(0x0A, r.getDecoded()));
    }

    @Test
    public void infoReportIs117BytesWithLittleEndianIntegers() {
        ObjectNode sample = ProtocolTestSupport.sample(registry.get(ServiceType.GATEWAY_INFO_REPORT).getFields());
        byte[] bytes = encode(0x06, sample);
        assertEquals(117, bytes.length);
        assertEquals(0x34, bytes[23] & 0xFF);
        assertEquals(0x12, bytes[24] & 0xFF);
    }

    @Test
    public void configReportUsesBigEndianPortAtOffset63() {
        ObjectNode sample = ProtocolTestSupport.sample(registry.get(ServiceType.GATEWAY_CONFIG_REPORT).getFields());
        byte[] bytes = encode(0x08, sample);
        assertEquals(80, bytes.length);
        assertEquals(0x12, bytes[63] & 0xFF);
        assertEquals(0x34, bytes[64] & 0xFF);
    }

    @Test
    public void upgradeCommandPackageSizeIsLittleEndian() {
        ObjectNode sample = ProtocolTestSupport.sample(registry.get(ServiceType.GATEWAY_UPGRADE).getFields());
        byte[] bytes = encode(0x5C, sample);
        assertEquals(391, bytes.length);
        assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, Arrays.copyOfRange(bytes, 354, 358));
    }

    @Test
    public void upgradeStatusIs78Bytes() {
        ObjectNode sample = ProtocolTestSupport.sample(registry.get(ServiceType.GATEWAY_UPGRADE_STATUS).getFields());
        byte[] bytes = encode(0x5D, sample);
        assertEquals(78, bytes.length);
        assertEquals(1, bytes[64] & 0xFF);
        assertEquals(1, bytes[65] & 0xFF);
        assertEquals(1, bytes[66] & 0xFF);
    }

    private DecodeResult decode(int code, byte[] bytes) {
        return manager.decode(binaryProfile, code, bytes);
    }

    private byte[] encode(int code, ObjectNode node) {
        return manager.encode(binaryProfile, code, node);
    }
}
