package com.proxy.test.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxy.test.MockFrameCodec;
import com.proxy.test.MockTransportMessage;

public class MockProtocolCodecTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void loadsAll37Protocols() {
        MockProtocolCodec codec = new MockProtocolCodec();
        assertEquals(37, codec.size());
        assertNotNull(codec.protocol(1));
        assertNotNull(codec.protocol(3));
        assertNotNull(codec.protocol(101));
        assertEquals(codec.name(101), "GATEWAY_REBOOT");
        assertEquals(codec.direction(101), "downlink");
    }

    @Test
    public void everySupportedProtocolRoundTrips() {
        MockProtocolCodec codec = new MockProtocolCodec();
        List<MockServiceCase> cases = codec.allCases();
        assertEquals(37, cases.size());

        for (MockServiceCase testCase : cases) {
            int type = testCase.getServiceType();
            JsonNode sample = testCase.getSample();
            if (testCase.isUnsupported()) {
                MockDecodeResult result = codec.decode(type, MockProtocolCodec.ENCODING_BINARY,
                        MockProtocolCodec.PROTOCOL_VERSION_1, MockProtocolCodec.ENCRYPTION_NONE,
                        new byte[] {1, 2, 3});
                assertEquals(MockDecodeResult.Status.UNSUPPORTED, result.getStatus(),
                        type + " should be unsupported");
                try {
                    codec.encode(type, sample);
                    fail("unsupported " + type + " should not encode");
                } catch (IllegalArgumentException expected) {
                    assertTrue(expected.getMessage().contains("no field schema"));
                }
                continue;
            }

            byte[] encoded = codec.encode(type, sample);
            MockDecodeResult decoded = codec.decode(type, MockProtocolCodec.ENCODING_BINARY,
                    MockProtocolCodec.PROTOCOL_VERSION_1, MockProtocolCodec.ENCRYPTION_NONE, encoded);
            assertTrue(decoded.isSuccess(),
                    type + " " + testCase.getName() + " decode failed: " + decoded);
            byte[] reEncoded = codec.encode(type, decoded.getBusiness());
            assertArrayEquals(encoded, reEncoded,
                    type + " " + testCase.getName() + " not byte-stable");
        }
    }

    @Test
    public void emptyProtocolsEncodeZeroBytes() {
        MockProtocolCodec codec = new MockProtocolCodec();
        int emptyCount = 0;
        for (MockServiceCase testCase : codec.allCases()) {
            if (testCase.isUnsupported()) {
                continue;
            }
            JsonNode protocol = codec.protocol(testCase.getServiceType());
            if (protocol.path("fields").size() == 0) {
                emptyCount++;
                assertEquals(0, codec.encode(testCase.getServiceType(), testCase.getSample()).length);
                MockDecodeResult decoded = codec.decode(testCase.getServiceType(),
                        MockProtocolCodec.ENCODING_BINARY, MockProtocolCodec.PROTOCOL_VERSION_1,
                        MockProtocolCodec.ENCRYPTION_NONE, new byte[0]);
                assertEquals(MockDecodeResult.Status.EMPTY, decoded.getStatus());
                assertNotNull(decoded.getBusiness());
            }
        }
        assertTrue(emptyCount > 0, "schema should contain empty protocols");
    }

    @Test
    public void jsonEncodingPassesThrough() throws Exception {
        MockProtocolCodec codec = new MockProtocolCodec();
        JsonNode sample = codec.sample(101);
        byte[] json = MAPPER.writeValueAsBytes(sample);
        MockDecodeResult decoded = codec.decode(101, MockProtocolCodec.ENCODING_JSON,
                MockProtocolCodec.PROTOCOL_VERSION_1, MockProtocolCodec.ENCRYPTION_NONE, json);
        assertEquals(MockDecodeResult.Status.OK, decoded.getStatus());
        assertEquals(1, decoded.getBusiness().path("appType").asInt());
        assertEquals(1, decoded.getBusiness().path("action").asInt());
    }

    @Test
    public void unsupportedEncodingVersionAndEncryptionAreReported() {
        MockProtocolCodec codec = new MockProtocolCodec();
        byte[] sample = codec.encode(101, codec.sample(101));
        assertEquals(MockDecodeResult.Status.UNSUPPORTED,
                codec.decode(101, 9, 1, 0, sample).getStatus());
        assertEquals(MockDecodeResult.Status.UNSUPPORTED,
                codec.decode(101, 2, 2, 0, sample).getStatus());
        assertEquals(MockDecodeResult.Status.UNSUPPORTED,
                codec.decode(101, 2, 1, 1, sample).getStatus());
    }

    @Test
    public void serviceAckIsRecognizedAsEmpty() {
        MockProtocolCodec codec = new MockProtocolCodec();
        MockDecodeResult result = codec.decode(0, MockProtocolCodec.ENCODING_BINARY,
                MockProtocolCodec.PROTOCOL_VERSION_1, MockProtocolCodec.ENCRYPTION_NONE,
                new byte[0]);
        assertEquals(MockDecodeResult.Status.EMPTY, result.getStatus());
        assertEquals(codec.name(0), "SERVICE_ACK");
        assertEquals(codec.direction(0), "downlink");
    }

    @Test
    public void unknownServiceTypeIsUnsupported() {
        MockProtocolCodec codec = new MockProtocolCodec();
        MockDecodeResult result = codec.decode(0x02, MockProtocolCodec.ENCODING_BINARY,
                MockProtocolCodec.PROTOCOL_VERSION_1, MockProtocolCodec.ENCRYPTION_NONE,
                new byte[] {1});
        assertEquals(MockDecodeResult.Status.UNSUPPORTED, result.getStatus());
        try {
            codec.encode(0x02, MAPPER.createObjectNode());
            fail("unknown serviceType should not encode");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unknown serviceType"));
        }
    }

    @Test
    public void frameRoundTripAndCorruptionAreRejected() throws Exception {
        MockProtocolCodec codec = new MockProtocolCodec();
        byte[] serviceData = codec.encode(101, codec.sample(101));
        byte[] payload = MockFrameCodec.buildPayload("30410007102", 101, serviceData, 7, 9);
        byte[] frame = MockFrameCodec.wrapFrame(payload);

        ByteArrayInputStream in = new ByteArrayInputStream(concat(frame, frame));
        byte[] first = MockFrameCodec.readFrame(in);
        byte[] second = MockFrameCodec.readFrame(in);
        assertArrayEquals(payload, first);
        assertArrayEquals(payload, second);
        assertEquals(null, MockFrameCodec.readFrame(in));

        MockTransportMessage message = MockFrameCodec.decodePayload(payload, codec);
        assertEquals(message.getDeviceId(), "30410007102");
        assertEquals(101, message.getServiceType());
        assertEquals(7, message.getEventId());
        assertEquals(9, message.getMessageId());
        assertEquals(1, message.getBusiness().getBusiness().path("appType").asInt());

        byte[] corrupted = payload.clone();
        corrupted[corrupted.length - 1] ^= 0x01;
        try {
            MockFrameCodec.decodePayload(corrupted, codec);
            fail("corrupted CRC should be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("CRC"));
        }

        try {
            MockFrameCodec.decodePayload(java.util.Arrays.copyOf(payload, payload.length - 2), codec);
            fail("truncated payload should be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("mismatch")
                    || expected.getMessage().contains("too short"));
        }
    }

    @Test
    public void jsonFrameCarriesEncodingOne() throws Exception {
        MockProtocolCodec codec = new MockProtocolCodec();
        byte[] json = MAPPER.writeValueAsBytes(codec.sample(101));
        byte[] payload = MockFrameCodec.buildPayload("30410007102", 101, json,
                MockProtocolCodec.ENCODING_JSON, 11, 22);
        MockTransportMessage message = MockFrameCodec.decodePayload(payload, codec);
        assertEquals(101, message.getServiceType());
        assertEquals(MockProtocolCodec.ENCODING_JSON, message.getServiceDataEncoding());
        assertEquals(MockDecodeResult.Status.OK, message.getBusiness().getStatus());
        assertEquals(1, message.getBusiness().getBusiness().path("appType").asInt());
    }

    @Test
    public void emptyPayloadDecodeIsNotSuccessForEmptyProtocolWithData() {
        MockProtocolCodec codec = new MockProtocolCodec();
        MockDecodeResult result = codec.decode(1, MockProtocolCodec.ENCODING_BINARY,
                MockProtocolCodec.PROTOCOL_VERSION_1, MockProtocolCodec.ENCRYPTION_NONE,
                new byte[] {1, 2});
        assertEquals(MockDecodeResult.Status.FAILED, result.getStatus());
        assertFalse(result.isSuccess());
    }

    private static byte[] concat(byte[] a, byte[] b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(a.length + b.length);
        out.write(a, 0, a.length);
        out.write(b, 0, b.length);
        return out.toByteArray();
    }
}
