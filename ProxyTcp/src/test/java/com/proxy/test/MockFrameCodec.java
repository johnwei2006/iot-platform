package com.proxy.test;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.proxy.codec.DecodeException;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.UplinkTransportMessage;
import com.proxy.test.protocol.MockDecodeResult;
import com.proxy.test.protocol.MockProtocolCodec;
import com.proxy.utils.ByteUtil;
import com.proxy.utils.MD5Util;

/**
 * 模拟设备侧的 TCP 帧与传输层编解码。
 *
 * <p>外层帧：0xFF 0xEE + 2 字节小端长度 + payload。
 * payload：4 字节传输头 + 41 字节 transport + serviceData + 1 字节 XOR CRC。
 */
public final class MockFrameCodec {

    public static final int FRAME_HEADER_SIZE = 4;
    public static final int TRANSPORT_PART_LENGTH = 41;
    public static final int MAX_FRAME_LENGTH = 10 * 1024 * 1024;

    private MockFrameCodec() {
    }

    public static byte[] wrapFrame(byte[] payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        byte[] frame = new byte[FRAME_HEADER_SIZE + payload.length];
        frame[0] = (byte) 0xFF;
        frame[1] = (byte) 0xEE;
        byte[] length = ByteUtil.short2BigByte2Array(payload.length);
        frame[2] = length[0];
        frame[3] = length[1];
        System.arraycopy(payload, 0, frame, FRAME_HEADER_SIZE, payload.length);
        return frame;
    }

    /**
     * 阻塞读取一个完整帧，返回 payload（不含 4 字节帧头）。
     * EOF 返回 null，非法帧头或超长帧抛 IOException。
     */
    public static byte[] readFrame(InputStream in) throws IOException {
        byte[] header = new byte[FRAME_HEADER_SIZE];
        if (!readFully(in, header, true)) {
            return null;
        }
        if ((header[0] & 0xFF) != 0xFF || (header[1] & 0xFF) != 0xEE) {
            throw new IOException("invalid frame prefix: "
                    + String.format("%02x %02x", header[0] & 0xFF, header[1] & 0xFF));
        }
        int payloadLength = (header[2] & 0xFF) | ((header[3] & 0xFF) << 8);
        if (payloadLength > MAX_FRAME_LENGTH) {
            throw new IOException("frame too large: " + payloadLength);
        }
        byte[] payload = new byte[payloadLength];
        if (!readFully(in, payload, false)) {
            throw new EOFException("unexpected EOF while reading frame payload");
        }
        return payload;
    }

    private static boolean readFully(InputStream in, byte[] buffer, boolean allowEofAtStart)
            throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if (read < 0) {
                if (offset == 0 && allowEofAtStart) {
                    return false;
                }
                throw new EOFException("unexpected EOF after " + offset + " bytes");
            }
            offset += read;
        }
        return true;
    }

    /**
     * 解析下行 payload，并调用 schema 编解码器得到业务字段。
     */
    public static MockTransportMessage decodePayload(byte[] payload, MockProtocolCodec codec)
            throws IOException {
        if (payload == null || payload.length < FRAME_HEADER_SIZE + TRANSPORT_PART_LENGTH + 1) {
            throw new IOException("payload too short: " + (payload == null ? 0 : payload.length));
        }
        int transportEncoding = payload[0] & 0xFF;
        int transportVersion = payload[1] & 0xFF;
        int transportLength = (payload[2] & 0xFF) | ((payload[3] & 0xFF) << 8);
        if (transportLength < TRANSPORT_PART_LENGTH) {
            throw new IOException("transport length too short: " + transportLength);
        }
        int expectedLength = FRAME_HEADER_SIZE + transportLength + 1;
        if (payload.length != expectedLength) {
            throw new IOException("payload length mismatch: expected " + expectedLength
                    + " but got " + payload.length);
        }

        // 最后一字节是 XOR CRC，校验范围为 payload 去掉 CRC 的部分。
        byte expectedCrc = MD5Util.calculate_checksum(
                Arrays.copyOfRange(payload, 0, payload.length - 1));
        byte actualCrc = payload[payload.length - 1];
        if (expectedCrc != actualCrc) {
            throw new IOException(String.format(
                    "CRC mismatch: expected 0x%02x but got 0x%02x",
                    expectedCrc & 0xFF, actualCrc & 0xFF));
        }

        TransportDataEncoder encoder = new TransportDataEncoder();
        UplinkTransportMessage transport;
        try {
            transport = encoder.decodeDownlinkTransportMessage(payload);
        } catch (DecodeException e) {
            throw new IOException("transport decode failed: " + e.getMessage(), e);
        }

        MockTransportMessage message = new MockTransportMessage();
        message.setTransportDataEncoding(transportEncoding);
        message.setTransportVersion(transportVersion);
        message.setTransportDataLength(transportLength);
        message.setDeviceId(transport.getDeviceId());
        message.setNodeId(transport.getNodeId());
        message.setMessageCreationTime(formatTime(transport.getMessageCreationTime()));
        message.setMessageId(transport.getMessageId());
        message.setServiceType(transport.getServiceType());
        message.setServiceVersion(transport.getServiceVersion());
        message.setServiceDataEncoding(transport.getServiceDataEncoding());
        message.setServiceDataProtocolVersion(transport.getServiceDataProtocolVersion());
        message.setServiceDataEncryption(transport.getServiceDataEncryption());
        message.setEventId(transport.getEventId());
        message.setServiceData(transport.getServiceData() == null
                ? new byte[0] : transport.getServiceData());
        message.setFrameLength(payload.length);

        MockDecodeResult business = codec.decode(message.getServiceType(),
                message.getServiceDataEncoding(), message.getServiceDataProtocolVersion(),
                message.getServiceDataEncryption(), message.getServiceData());
        message.setBusiness(business);
        return message;
    }

    private static String formatTime(java.util.Date time) {
        if (time == null) {
            return "-";
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
    }

    /**
     * 构造一帧设备上行 payload。测试侧回写下行业务时复用。
     */
    public static byte[] buildPayload(String deviceId, int serviceType, byte[] serviceData)
            throws Exception {
        return buildPayload(deviceId, serviceType, serviceData,
                MockProtocolCodec.ENCODING_BINARY, null, null);
    }

    public static byte[] buildPayload(String deviceId, int serviceType, byte[] serviceData,
                                      Integer eventId, Integer messageId) throws Exception {
        return buildPayload(deviceId, serviceType, serviceData,
                MockProtocolCodec.ENCODING_BINARY, eventId, messageId);
    }

    public static byte[] buildPayload(String deviceId, int serviceType, byte[] serviceData,
                                      int serviceDataEncoding, Integer eventId, Integer messageId)
            throws Exception {
        return buildPayload(deviceId, serviceType, serviceData, serviceDataEncoding,
                eventId, messageId, null);
    }

    /** 携带网关认证 token 构造上行车；token 为 null 时填充全 0。 */
    public static byte[] buildPayload(String deviceId, int serviceType, byte[] serviceData,
                                      int serviceDataEncoding, Integer eventId, Integer messageId,
                                      byte[] token) throws Exception {
        TransportDataEncoder encoder = new TransportDataEncoder();
        DownlinkTransportMessage message = encoder.mockDownlinkTransportMessage(
                deviceId, serviceType, serviceData, serviceDataEncoding,
                MockProtocolCodec.PROTOCOL_VERSION_1, MockProtocolCodec.ENCRYPTION_NONE,
                eventId, messageId, token);
        return encoder.createUplinkMessage(message);
    }
}
