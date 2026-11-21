package com.proxy.codec.v3;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;

import com.proxy.Constants;
import com.proxy.codec.MessageCoderFactory;
import com.proxy.model.CommonTransportMessage;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.MessageHeader;
import com.proxy.model.UplinkTransportMessage;
import com.proxy.utils.ByteUtil;

/**
 * 传输层 V3 编解码器（51 字节定长段 + 10 字节 token）单元测试。
 */
public class TransportcoderByteImplV3Test {

	private static final String DEVICE_ID = "GW00000001";
	private static final byte[] TOKEN = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

	private byte[] buildTransportBytes(byte[] token) {
		byte[] bytes = new byte[CommonTransportMessage.TRANSPORT_PART_FIX_LENGTH];
		byte[] deviceIdBytes = String.format("%-11s", DEVICE_ID).getBytes();
		System.arraycopy(deviceIdBytes, 0, bytes, 0, 11);
		byte[] time = { 26, 11, 21, 12, 30, 0 };
		System.arraycopy(time, 0, bytes, 22, 6);
		byte[] messageId = ByteUtil.int2BigByte4Array(0x01020304);
		System.arraycopy(messageId, 0, bytes, 28, 4);
		bytes[32] = (byte) Constants.SERVICETYPE_HEART_BEAT;
		bytes[33] = 1;
		bytes[34] = 2;
		bytes[35] = 1;
		bytes[36] = 0;
		byte[] eventId = ByteUtil.int2BigByte4Array(0x0000ABCD);
		System.arraycopy(eventId, 0, bytes, 37, 4);
		System.arraycopy(token, 0, bytes, 41, CommonTransportMessage.TOKEN_LENGTH);
		return bytes;
	}

	@Test
	public void testDecodeExtractsToken() throws Exception {
		TransportcoderByteImplV3 coder = new TransportcoderByteImplV3();
		UplinkTransportMessage message = coder.decode(buildTransportBytes(TOKEN));
		assertEquals(DEVICE_ID.trim(), message.getDeviceId());
		assertEquals(Constants.SERVICETYPE_HEART_BEAT, message.getServiceType());
		assertArrayEquals(TOKEN, message.getToken());
		assertEquals(0x01020304, message.getMessageId());
		assertEquals(0x0000ABCD, message.getEventId());
	}

	@Test
	public void testDecodeRejectsShortMessage() {
		TransportcoderByteImplV3 coder = new TransportcoderByteImplV3();
		try {
			coder.decode(new byte[41]);
			fail("41-byte legacy transport message must be rejected");
		} catch (Exception expected) {
			assertTrue(expected.getMessage().contains("length"));
		}
	}

	@Test
	public void testEncodeDecodeRoundTrip() throws Exception {
		TransportcoderByteImplV3 coder = new TransportcoderByteImplV3();
		DownlinkTransportMessage downlink = new DownlinkTransportMessage();
		downlink.setDeviceId(DEVICE_ID);
		downlink.setNodeId("           ");
		downlink.setMessageCreationTime(new Date());
		downlink.setMessageId(0x11223344);
		downlink.setServiceType((short) Constants.SERVICETYPE_ACK);
		downlink.setServiceVersion((byte) 1);
		downlink.setServiceDataEncoding(0);
		downlink.setServiceDataProtocolVersion(1);
		downlink.setServiceDataEncryption(0);
		downlink.setEventId(0x00CDEF01);
		downlink.setToken(TOKEN);

		byte[] encoded = coder.encode(downlink);
		assertEquals(CommonTransportMessage.TRANSPORT_PART_FIX_LENGTH, encoded.length);
		assertArrayEquals(TOKEN, java.util.Arrays.copyOfRange(encoded, 41, 51));

		UplinkTransportMessage decoded = coder.decode(encoded);
		assertArrayEquals(TOKEN, decoded.getToken());
		assertEquals(DEVICE_ID.trim(), decoded.getDeviceId());
	}

	@Test
	public void testEncodeWithoutTokenFillsZero() throws Exception {
		TransportcoderByteImplV3 coder = new TransportcoderByteImplV3();
		DownlinkTransportMessage downlink = new DownlinkTransportMessage();
		downlink.setDeviceId(DEVICE_ID);
		downlink.setNodeId("           ");
		downlink.setMessageCreationTime(new Date());
		downlink.setMessageId(1);
		downlink.setServiceType((short) Constants.SERVICETYPE_ACK);
		downlink.setServiceVersion((byte) 1);
		downlink.setServiceDataEncoding(0);
		downlink.setServiceDataProtocolVersion(1);
		downlink.setServiceDataEncryption(0);
		downlink.setEventId(2);

		byte[] encoded = coder.encode(downlink);
		byte[] tokenSlice = java.util.Arrays.copyOfRange(encoded, 41, 51);
		assertArrayEquals(new byte[CommonTransportMessage.TOKEN_LENGTH], tokenSlice);
	}

	@Test
	public void testFactoryRejectsVersion1() {
		MessageHeader header = new MessageHeader();
		header.setTransportVersion((byte) 1);
		try {
			MessageCoderFactory.createMessageCoder(header);
			fail("transport version 1 (legacy firmware) must be rejected");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("transport version 2"));
		}
	}
}
