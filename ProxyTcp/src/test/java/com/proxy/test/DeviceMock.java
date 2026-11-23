package com.proxy.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.tomcat.util.buf.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.proxy.app.Constants;
import com.proxy.codec.MessageCoderFactory;
import com.proxy.codec.TransportCoder;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.MessageHeader;
import com.proxy.model.UplinkTransportMessage;
import com.proxy.utils.HexConvert;
import com.proxy.utils.MD5Util;

public class DeviceMock {
	private static Logger logger = LoggerFactory.getLogger(DeviceMock.class);
	static final int MTU = 1024 * 1024;
	private volatile DatagramChannel sendChannel;
	private volatile DatagramChannel receiveChannel;
	private UDPReceiverThread receiverThread;
	private volatile boolean keepRunning = true;
	
	
	public static void main(String[] args) {
		try {
			DeviceMock sender = new DeviceMock();
//			sender.sendProtobuf();
			sender.init();
			sender.sendByte();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendByte() {
		final String ip = "127.0.0.1";// destinationProps.get(DestinationPropertyKey.UDP_HOST);
		//final String ip = "39.107.77.230";
		SocketAddress targetAddr = new InetSocketAddress(ip, 50001);

		//11 appVersion
		short serviceType = 11;
		//ServiceType = Ack
		//byte[] serviceData = HexUtils.fromHexString("0201290031303230303130303037310000000000000000000000ff0b1e000000b4a4000000010201007701000090");
		//ServiceType = Hearbeat
		byte[] serviceData = HexUtils.fromHexString("FFEE2D000201290033303431303030373130320000000000000000000000000000000000b06500000101020100af55000034");
		//byte[] serviceData = HexUtils.fromHexString("0201290033303431303030373130320000000000000000000000ff0b1e0000000e0f00000001020100564d0000ea");
		
		////byte[] serviceData = HexUtils.fromHexString("02012900333034313030303731303200000000000000000000000000000000009f4c00000101020100a0b80000d0");
		sendData(serviceData, targetAddr, null);
		
		//41 alarm_start
//		short serviceType = 41;
//		byte[] serviceData = HexUtils.fromHexString("0000000055b0e11d29373030313031303333333334303236370102020127d800ca3ce87402cd4eb810007a000003eb");//alarm start

		//48 alarm finish
//		short serviceType = 48;
//		byte[] serviceData = HexUtils.fromHexString("0000000055b0e11d30373030313031303333333334303236370102020127d800ede697d402c729de74007a000003eb");//alarmSetting

		//42 alarmSettingQuery
//		short serviceType = 42; 
//		byte[] serviceData = HexUtils.fromHexString("0000000055b0e11d2a");//alarmSettingQuery
		
		//43 toolrequrationresponse
//		short serviceType = 43;
//		byte[] serviceData = HexUtils.fromHexString("0000000055b0e11d2b6669656c643030310000000027100dac");//
		
		//45 quality info
//		short serviceType = 45;
//		byte[] serviceData = HexUtils.fromHexString("0000000055b0e11d2d3730303130313033333333343032363704b00bb803e8000009c4041a00ede697d402c729de74");

		//46 tool matching response
//		short serviceType = 46;
//		byte[] serviceData = HexUtils.fromHexString("00000000000030392e746f6f6c2d303031000000000000000002a83a934600e931a84e");

		//taskstartreport 60
//		short serviceType = 60;
//		byte[] serviceData = HexUtils.fromHexString("0000000000a550563c373030313031303333333334303236370200012c0101f4140a1a0a1d1c00ca3ce87402cd4eb810");
		
		//taskfinishreport 62
//		short serviceType = 62;
//		byte[] serviceData = HexUtils.fromHexString("0000000000a550563e01373030313031303333333334303236370200012c0101f4140a1a0a1d39140a1a0a1d39138800ca3ce87402cd4eb810");

		//49 QUALITY_SETTING_QUERY_RESPONSE
//		short serviceType = 49;
//		byte[] serviceData = HexUtils.fromHexString("0047c4b936c31706310102010203e807d0030403040bb80fa0");

		//byte[] payload = TransportDataCreator.createUplinkMessage(serviceData,0, serviceType);
		
		//sendData(payload, targetAddr, null);
	}

	private void init() {
		try {
			receiveChannel = DatagramChannel.open();
			receiveChannel.bind(new InetSocketAddress("0.0.0.0", 7755));
			receiveChannel.configureBlocking(true);
			sendChannel = receiveChannel;
			receiverThread = new UDPReceiverThread();
			new Thread(receiverThread).start();
			//startUDPResendThread();
		} catch (final Exception e) {
			logger.error("Failed to initialize IF1ConnectorUDP");
			throw new RuntimeException(e);
		}
	}

    private void sendData(final byte[] data, final SocketAddress targetAddr,
			final Map<String, String> destinationProps) {
		try {
			final ByteBuffer srcBuffer = ByteBuffer.wrap(data);
//			sendChannel = DatagramChannel.open();
//			sendChannel.bind(new InetSocketAddress("0.0.0.0", 7755));
			//sendChannel.connect(targetAddr);
			//boolean isConnected = sendChannel.isConnected();
			boolean isOpen = sendChannel.isOpen();
			if (isOpen) {
				int size = sendChannel.send(srcBuffer, targetAddr);
				if (size == 0) {
					System.out.println("connection is not available!");
				} else {
					System.out.println("Success to send " + size + " bytes to " + targetAddr);
				}
			} else {
				System.out.println("connection is not available!");
			}

		} catch (final IOException ioe) {
			System.out.println("Could not send message on UDP socket!");
		}
	}
	private class UDPReceiverThread implements Runnable {

		private UDPReceiverThread() {
//			setName("udp-" + receivePort + "-receiver-thread");
//			setDaemon(true);
		}

		public void run() {
			// final byte[] receiveBuffer = new byte[MTU];
			final ByteBuffer buffer = ByteBuffer.allocateDirect(MTU);
			InetSocketAddress sourceAddress = null;
			while (keepRunning) {
				try {
					// Reset buffer before reusing it.
					buffer.clear();
					// Wait to receive a datagram (Blocking read)
					sourceAddress = (InetSocketAddress) receiveChannel.receive(buffer);

					logger.info("Receive data from " + sourceAddress);
					// Create uplink message from received datagram
					final byte[] payload = new byte[buffer.position()];
					buffer.clear();
					buffer.get(payload);
					logger.info("Received data: " + payload.length + " : " + new String(payload));
					logger.info("Hex data     : " + HexConvert.toHexString(payload));
					//handlePayLoad(payload);
				} catch (final IOException e) {
					logger.warn("Caught an exception when receiving a message from the socket.", e);
					if (!receiveChannel.isOpen()) {
						logger.error("Socket is closed, stopping thread!");
						return;
					}
				}
			}
			closeChannel(receiveChannel);
		}

		private URI toUdpUri(final String host, final int port) throws URISyntaxException {
			return new URI("udp", "", host, port, "", "", "");
		}
	}
	
	private void handlePayLoad( byte[] payload ) {
		UplinkTransportMessage message = decodeUplinkTransportMessage(payload);
		logger.info("Service type: " + message.getServiceType());
		logger.info("Message Id: " + message.getMessageId());
		logger.info("Event Id: " + message.getEventId());
		if(message.getServiceType() == Constants.SERVICETYPE_ONLINE_CHECK) {
			DownlinkTransportMessage downlinkMsg = new DownlinkTransportMessage();
			downlinkMsg.setHeader(message.getHeader());
			downlinkMsg.setMessageId(message.getMessageId());
			downlinkMsg.setServiceType(Constants.SERVICETYPE_ACK);
			downlinkMsg.setMessageCreationTime(message.getMessageCreationTime());
			downlinkMsg.setDeviceId(message.getDeviceId());
			downlinkMsg.setServiceData(null);
		}
		
//		Message sendbackMsg = new Message();
//		byte[] completeMessage = null;
//		try {
//			completeMessage = this.encodeUplinkTransportMessage(message);
//	
//			sendbackMsg.setConnectType(Message.CONNECTTYPE_UDP);
//			sendbackMsg.setPayload(completeMessage);
//			sendbackMsg.setIp(vehicleStatus.getIp());
//			sendbackMsg.setPort(vehicleStatus.getPort());
//		} catch(Exception err) {
//
//		}
	}
	private void closeChannel(final DatagramChannel channel) {
		try {
			if (channel != null && channel.isOpen()) {
				channel.close();
			}
		} catch (final Exception e) {
			logger.warn("Could not close socket!", e);
		}
	}
	
	private UplinkTransportMessage decodeUplinkTransportMessage(byte[] payload) {
		MessageHeader header = new MessageHeader();
		UplinkTransportMessage decodedUplinkTransportMessage = null;

		if (payload.length < header.getMESSAGE_HEADER_LENGTH()) {
			logger.info("the message is too short, payload length= " + payload.length);
			return null;
		}

		header.setTransportDataEncoding(payload[header.getTRANSPORT_ENCODING_INDEX()]);
		header.setTransportVersion(payload[header.getTRANSPORT_VERSION_INDEX()]);
		header.setTransportDataLength(payload[header.getTRANSPORT_LENGTH_INDEX()]);
		//header.setSignatureLength(payload[header.getSIGNATURE_LENGTH_INDEX()]);

		// the length of message is less than the length of TransportData
		if ((payload.length - header.getMESSAGE_HEADER_LENGTH()) < (int) header.getTransportDataLength()) {
			logger.info("the message header is too short, payload length= " + payload.length);
			return null;
		}

		byte[] transportBytes = new byte[(int) header.getTransportDataLength()];
		System.arraycopy(payload, header.getMESSAGE_HEADER_LENGTH(), transportBytes, 0, (int) header.getTransportDataLength());

		// the total length of the message is less then the length of the message plus the length of the signature
		byte[] signatureBytes = new byte[(int) header.getTRANSPORT_CRC_SIZE()];
		if ((payload.length - header.getMESSAGE_HEADER_LENGTH() - (int) header.getTransportDataLength()) < (int) header.getTRANSPORT_CRC_SIZE()) {
			logger.info("the total length of the message is less then the length of the message plus the length of the signature" + payload.length);
			return null;
		}
		System.arraycopy(payload, (payload.length - header.getTRANSPORT_CRC_SIZE()), signatureBytes, 0, (int) header.getTRANSPORT_CRC_SIZE());

		// check if the signature of the message equals the result of the signature of the messageBody, this means the messageBody has not been modified during the transportation
		byte[] messageBody = new byte[(payload.length - header.getTRANSPORT_CRC_SIZE())];
		System.arraycopy(payload, 0, messageBody, 0, (payload.length - header.getTRANSPORT_CRC_SIZE()));
		byte[] resultSignature = MD5Util.getMD5(messageBody);
		if (!MD5Util.bufferToHex(resultSignature).equals(MD5Util.bufferToHex(signatureBytes))) {
			logger.error("the signature of the message is not the same with the original signature, original signature:" + signatureBytes + ", result signature:" + resultSignature);
			return null;
		}

		int serviceDataLength = payload.length - header.getMESSAGE_HEADER_LENGTH() - (int) header.getTransportDataLength() - (int) header.getTRANSPORT_CRC_SIZE();
		byte[] serviceData = new byte[serviceDataLength];
		if (serviceDataLength > 0) {
			System.arraycopy(payload, (header.getMESSAGE_HEADER_LENGTH() + (int) header.getTransportDataLength()),serviceData, 0, serviceDataLength);
		}

		byte[] transportMessageBytes = new byte[header.getTransportDataLength()];
		System.arraycopy(payload, header.getMESSAGE_HEADER_LENGTH(), transportMessageBytes, 0, header.getTransportDataLength());
		// Decode transport message
		try {
			TransportCoder messageCoder;
			messageCoder = MessageCoderFactory.createMessageCoder(header);
			decodedUplinkTransportMessage = messageCoder.decode(transportMessageBytes);
			decodedUplinkTransportMessage.setServiceData(serviceData);
			logger.info("decoded transport message id:" + decodedUplinkTransportMessage.getMessageId());
		} catch (Exception err) {
			err.printStackTrace();
			logger.error("Error happened during decoding the uplink message for " + decodedUplinkTransportMessage.getMessageId() + ", error message:" + err.getMessage());
		}
		decodedUplinkTransportMessage.setHeader(header);
		return decodedUplinkTransportMessage;
	}

//	private byte[] encodeUplinkTransportMessage(CommonTransportMessage downlinkMessage) throws Exception {
//
//		MessageHeader header = new MessageHeader();
//
//		header.setTransportDataEncoding((byte)0);
//		header.setTransportVersion((byte)1);
//		header.setSignatureLength((byte) Constants.SIGNATURE_LENGTH);
//
//		downlinkMessage.setHeader(header);
//		byte[] transportMsg = null;
//		try {
//			TransportCoder messageCoder;
//			messageCoder = MessageCoderFactory.createMessageCoder(header);
//			transportMsg = messageCoder.encode(transportMessageBytes);
//			decodedUplinkTransportMessage.setServiceData(serviceData);
//			logger.info("decoded transport message id:" + decodedUplinkTransportMessage.getMessageId());
//		} catch (Exception err) {
//			err.printStackTrace();
//			logger.error("Error happened during decoding the uplink message for " + decodedUplinkTransportMessage.getMessageId() + ", error message:" + err.getMessage());
//		}
//		
//		transportMsg = messageCodeHandler.encodeDownlinkMessage(downlinkMessage);
//		header.setTransportDataLength((byte) transportMsg.length);
//
//		byte[] serviceData = downlinkMessage.getServiceData();
//		int serviceDataLength = serviceData == null ? 0 : serviceData.length;
//		byte[] md5Bytes = new byte[header.getMESSAGE_HEADER_LENGTH() + transportMsg.length + serviceDataLength];
//		md5Bytes[0] = (byte) header.getTransportDataEncoding();
//		md5Bytes[1] = (byte) header.getTransportVersion();
//		md5Bytes[2] = (byte) transportMsg.length;
//		md5Bytes[3] = (byte) Constants.SIGNATURE_LENGTH;
//		System.arraycopy(transportMsg, 0, md5Bytes, header.getMESSAGE_HEADER_LENGTH(), transportMsg.length);
//		if (serviceData != null) {
//			System.arraycopy(serviceData, 0, md5Bytes, header.getMESSAGE_HEADER_LENGTH() + transportMsg.length, serviceDataLength);
//		}
//
//		byte[] resultSignature = MD5Util.getMD5(md5Bytes);
//
//		byte[] completeBytes = new byte[header.getMESSAGE_HEADER_LENGTH() + transportMsg.length + serviceDataLength + Constants.SIGNATURE_LENGTH];
//		System.arraycopy(md5Bytes, 0, completeBytes, 0, md5Bytes.length);
//		System.arraycopy(resultSignature, 0, completeBytes, md5Bytes.length, Constants.SIGNATURE_LENGTH);
//		return completeBytes;
//	}
}
