package com.proxy.test;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.proxy.codec.DecodeException;
import com.proxy.codec.MessageCodeHandler;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.Message;
import com.proxy.model.MessageHeader;
import com.proxy.model.UplinkTransportMessage;
import com.proxy.persistence.model.DeviceInfo;
import com.proxy.utils.ByteUtil;
import com.proxy.utils.MD5Util;
import com.proxy.utils.ShowFlakeIdGanerator;

public class TransportDataEncoder {
	private static Logger logger = LoggerFactory.getLogger(TransportDataEncoder.class);
	private MessageCodeHandler messageCodeHandler = new MessageCodeHandler();
    Message uplinkMsg;
	
    public static void main(String[] args){
    	TransportDataEncoder test = new TransportDataEncoder();
//    	test.testTransportMessage();
    }
    
//    public static byte[] createHeader(int length) {
//		byte[] headerBytes = new byte[4];
//		headerBytes[0] = (byte)0;	//TransportDataEncoding
//		headerBytes[1] = 1;	//TransportVersion
//		headerBytes[2] = (byte)length;	//TransportDataLength
//		headerBytes[3] = (byte)16;	//SignatureLength
//		return headerBytes;
//    }
//    
//    public static byte[] createUplinkMessage(byte[] serviceData, int type, short serviceType) {
//		byte[] completeMessage;
//
//		byte[] transportMessage = createTransportMessage(type, serviceType);
//		
//		byte[] headerMessage = createHeader(transportMessage.length);
//		
//		completeMessage = new byte[4 + transportMessage.length + (serviceData == null ? 0 : serviceData.length) + 16];
//		
//		//copy header bytes
//		System.arraycopy(headerMessage, 0, completeMessage, 0, 4);
//		//copy transport bytes
//		System.arraycopy(transportMessage, 0, completeMessage, 4, transportMessage.length);
//		
//		//copy service bytes
//		if (serviceData != null && serviceData.length > 0)
//			System.arraycopy(serviceData, 0, completeMessage, 4 + transportMessage.length, serviceData.length);
//		
//		byte[] md5Bytes = new byte[4 + transportMessage.length + serviceData.length];
//		//copy header bytes
//		System.arraycopy(headerMessage, 0, md5Bytes, 0, 4);
//		//copy transport bytes
//		System.arraycopy(transportMessage, 0, md5Bytes, 4, transportMessage.length);
//		
//		//copy service bytes
//		if (serviceData != null && serviceData.length > 0)
//			System.arraycopy(serviceData, 0, md5Bytes, 4 + transportMessage.length, serviceData.length);
//		
//		//signature
//		byte[] signatureByte = MD5Util.getMD5(md5Bytes);
//		
//		//copy signature bytes
//		System.arraycopy(signatureByte, 0, completeMessage, (4 + transportMessage.length + serviceData.length), signatureByte.length);
//		return completeMessage;
//    }
//    
//    public static byte[] createTransportMessage(int type, short serviceType) {
//    	if (type == 0)
//    		return createUplinkMessageByte(serviceType);
////    	else if (type == 1)
////    		return createTransportMessageProtobuf();
//    	else 
//    		return null;
//    }
//    
//    public static byte[] createUplinkMessageByte(short serviceType) {
//    	String vehicleIdentifier = "NJHYKBOAS0000115";
//    	Date messageCreationTime = new Date();
//    	String messageId = "123456";
//    	//short serviceType = serviceType;//ServiceType.SERVICETYPE_QUALITY_ALARM;
//    	short serviceDataEncoding = 0;
//    	short serviceDataProtocolVersion = 1;
//    	short serviceDataEncryption = 0;
//    	
//    	byte[] transportBytes = new byte[16+6+8+4];
//    	System.arraycopy(vehicleIdentifier.getBytes(), 0, transportBytes, 0, vehicleIdentifier.length());
//    	byte[] dateByte = new byte[6];
//    	System.arraycopy(new byte[]{(byte)(messageCreationTime.getYear())}, 0, dateByte, 0, 1);
//    	System.arraycopy(new byte[]{(byte)messageCreationTime.getMonth()}, 0, dateByte, 1, 1);
//    	System.arraycopy(new byte[]{(byte)messageCreationTime.getDate()}, 0, dateByte, 2, 1);
//    	System.arraycopy(new byte[]{(byte)messageCreationTime.getHours()}, 0, dateByte, 3, 1);
//    	System.arraycopy(new byte[]{(byte)messageCreationTime.getMinutes()}, 0, dateByte, 4, 1);
//    	System.arraycopy(new byte[]{(byte)messageCreationTime.getSeconds()}, 0, dateByte, 5, 1);
//    	
//    	System.arraycopy(dateByte, 0, transportBytes, 16, 6);
//
//    	System.arraycopy(messageId.getBytes(), 0, transportBytes, 22, messageId.getBytes().length);
//    	System.arraycopy(new byte[]{(byte)serviceType}, 0, transportBytes, 30, 1);
//    	System.arraycopy(new byte[]{(byte)serviceDataEncoding}, 0, transportBytes, 31, 1);
//    	System.arraycopy(new byte[]{(byte)serviceDataProtocolVersion}, 0, transportBytes, 32, 1);
//    	System.arraycopy(new byte[]{(byte)serviceDataEncryption}, 0, transportBytes, 33, 1);
//    	return transportBytes;
//    }
    
	public UplinkTransportMessage decodeDownlinkTransportMessage(byte[] payload) throws DecodeException{
		MessageHeader header = new MessageHeader();
		UplinkTransportMessage decodedUplinkTransportMessage = null;

		if (payload.length < header.getMESSAGE_HEADER_LENGTH() + header.getTRANSPORT_CRC_SIZE()) {
			//logger.info("From " + uplinkMsg.getIp() + ":" + uplinkMsg.getPort() + "(payload.length=" + payload.length + "<" + header.getMESSAGE_HEADER_LENGTH());
			throw new DecodeException(DecodeException.ERRORCODE_LENGTH_LESS_THEN_HEADER, "(payload.length=" + payload.length + "<" + header.getMESSAGE_HEADER_LENGTH());
		}

		header.setTransportDataEncoding(payload[header.getTRANSPORT_ENCODING_INDEX()]);
		header.setTransportVersion(payload[header.getTRANSPORT_VERSION_INDEX()]);

		byte[] transportDataLengthBytes = new byte[header.getTRANSPORT_LENGTH_SIZE()];
    	System.arraycopy(payload, header.getTRANSPORT_LENGTH_INDEX(), transportDataLengthBytes, 0, header.getTRANSPORT_LENGTH_SIZE());
		header.setTransportDataLength(ByteUtil.byte2BigArray2Short(transportDataLengthBytes));

		// transport length must at least contain the fixed 41-byte transport part
		if (header.getTransportDataLength() < UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH) {
			throw new DecodeException(DecodeException.ERRORCODE_LENGTH_LESS_THEN_HEADER,
					"(transport.length=" + header.getTransportDataLength() + "<"
							+ UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH + ")");
		}

		// the length of message is less than the length of TransportData
		if ((payload.length - header.getMESSAGE_HEADER_LENGTH()) < header.getTransportDataLength() + header.getTRANSPORT_CRC_SIZE()) {
			//logger.info("From " + uplinkMsg.getIp() + ":" + uplinkMsg.getPort() + "(payload.length-header.length=" + (payload.length - header.getMESSAGE_HEADER_LENGTH()) + ")!=(transport.length=" + header.getTransportDataLength() + ")");
			throw new DecodeException(DecodeException.ERRORCODE_LENGTH_LESS_THEN_HEADER, "(payload.length-header.length=" + (payload.length - header.getMESSAGE_HEADER_LENGTH()) + ")!=(transport.length=" + header.getTransportDataLength() + ")");
		}

		//transport + service
		byte[] transportPlusServiceBytes = new byte[header.getTransportDataLength()]; 
		System.arraycopy(payload, header.getMESSAGE_HEADER_LENGTH(), transportPlusServiceBytes, 0, header.getTransportDataLength());

		//transport only
		byte[] transportMessageBytes = new byte[UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH];
		System.arraycopy(payload, header.getMESSAGE_HEADER_LENGTH(), transportMessageBytes, 0, UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH);
		try {
			decodedUplinkTransportMessage = messageCodeHandler.decodeUplinkRawMessage(header, transportMessageBytes);
			logger.debug("Decoded transport data, message id			:" + decodedUplinkTransportMessage.getMessageId());
			logger.debug("Decoded transport data, event   id			:" + decodedUplinkTransportMessage.getEventId());
			logger.debug("Decoded transport data, message deviceId	:" + decodedUplinkTransportMessage.getDeviceId());
			logger.debug("Decoded transport data, service type		:" + decodedUplinkTransportMessage.getServiceType());
			logger.debug("Decoded transport data, message time		:" + decodedUplinkTransportMessage.getMessageCreationTime());			
		} catch (Exception err) {
			throw new DecodeException(DecodeException.ERRORCODE_SIGNATURE_LENGTH_INCORRECT, "(payload.length-header.length-transport.length=" + (payload.length - header.getMESSAGE_HEADER_LENGTH() - (int) header.getTransportDataLength()) + ")!=(signature.length=16)");
		}
		//System.arraycopy(payload, (payload.length - header.getSignatureLength()), signatureBytes, 0, (int) header.getSignatureLength());

		// check if the signature of the message equals the result of the signature of the messageBody, this means the messageBody has not been modified during the transportation
		byte[] crcMessageBody = new byte[(payload.length - header.getTRANSPORT_CRC_SIZE())];
		System.arraycopy(payload, 0, crcMessageBody, 0, (payload.length - header.getTRANSPORT_CRC_SIZE()));
		byte resultSignature = MD5Util.calculate_checksum(crcMessageBody);
		if(resultSignature != payload[payload.length - header.getTRANSPORT_CRC_SIZE()]) {
		//if (!HexConvert.toHexString(resultSignature).equals(HexConvert.toHexString(signatureBytes))) {
			String source = uplinkMsg == null ? "device-mock"
					: uplinkMsg.getIp() + ":" + uplinkMsg.getPort();
			logger.error("From " + source + "(gateway signature=" + resultSignature + ")!=(proxy signature=" + payload[payload.length - header.getTRANSPORT_CRC_SIZE()] + ")");
			throw new DecodeException(DecodeException.ERRORCODE_SIGNATURE_NOT_SAME, "(gateway signature=" + resultSignature + ")!=(proxy signature=" + payload[payload.length - header.getTRANSPORT_CRC_SIZE()] + ")");
		}

		//int serviceDataLength = payload.length - header.getMESSAGE_HEADER_LENGTH() - header.getTransportDataLength() - (int) header.getTRANSPORT_CRC_SIZE();
		int serviceDataLength = header.getTransportDataLength() - UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH;
		
		byte[] serviceData = new byte[serviceDataLength];
		if (serviceDataLength > 0) {
			System.arraycopy(payload, (header.getMESSAGE_HEADER_LENGTH() + UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH),serviceData, 0, serviceDataLength);
		}

		/*
		byte[] transportMessageBytes = new byte[header.getTransportDataLength()];
		System.arraycopy(payload, header.getMESSAGE_HEADER_LENGTH(), transportMessageBytes, 0, header.getTransportDataLength());
		// Decode transport message
		try {
			decodedUplinkTransportMessage = messageCodeHandler.decodeUplinkRawMessage(header, transportMessageBytes);
			decodedUplinkTransportMessage.setServiceData(serviceData);
			//logger.info("decoded transport message id:" + decodedUplinkTransportMessage.getMessageId());
		} catch (Exception err) {
			err.printStackTrace();
			//logger.error("From " + uplinkMsg.getIp() + ":" + uplinkMsg.getPort() + ", error to decode the uplink message for " + decodedUplinkTransportMessage.getMessageId() + ", " + err.getMessage());
			throw new DecodeException(DecodeException.ERRORCODE_DECODE_EXCEPTION, err.getMessage());
		}
		*/
		decodedUplinkTransportMessage.setHeader(header);
		decodedUplinkTransportMessage.setPackageSize(payload.length);
		decodedUplinkTransportMessage.setServiceData(serviceData);
		decodedUplinkTransportMessage.setServiceDataSize(serviceDataLength);
		
		return decodedUplinkTransportMessage;
	}
	
	public byte[] createUplinkMessage(DownlinkTransportMessage downlinkMessage) throws Exception {
		MessageHeader header  = downlinkMessage.getHeader();
		int serviceDataLength = downlinkMessage.getServiceDataSize();
		byte[] serviceData    = downlinkMessage.getServiceData();
		//1、encode transport part bytes
		byte[] transportPartBytes = messageCodeHandler.encodeDownlinkMessage(downlinkMessage);

		//2、crc calculation part bytes
		int crcDataLength = header.getMESSAGE_HEADER_LENGTH() + UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH + serviceDataLength;
		byte[] crcBytes = new byte[crcDataLength];
		//2.1 crc byte header
		crcBytes[0] = (byte) header.getTransportDataEncoding();
		crcBytes[1] = (byte) header.getTransportVersion();

		//2.2 crc byte transportPartLength
		int transportPartLength = UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH + serviceDataLength;
    	System.arraycopy(ByteUtil.short2BigByte2Array(transportPartLength), 0, crcBytes, header.getTRANSPORT_LENGTH_INDEX(), header.getTRANSPORT_LENGTH_SIZE());

    	//2.3 crc byte transport part 
    	System.arraycopy(transportPartBytes, 0, crcBytes, header.getMESSAGE_HEADER_LENGTH(), UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH);
    	
    	//2.4 crc byte service data
		if (serviceData != null) {
			System.arraycopy(serviceData, 0, crcBytes, header.getMESSAGE_HEADER_LENGTH() + UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH, serviceDataLength);
		}

		//3. crc calculation
		byte resultCrc = MD5Util.calculate_checksum(crcBytes);
		//logger.info("Send to device MD5 result:" + HexUtils.toHex(resultSignature));
		/*
		int serviceType = md5Bytes[35] & 0xFF;
		if (serviceType == 250) {
			logger.info("Upgrade task package: " + HexConvert.toHexString(md5Bytes));
			logger.info("Upgrade task package signature: " + HexConvert.toHexString(resultSignature));
		}
		if (resultSignature.length != Constants.SIGNATURE_LENGTH) {
			logger.error("MD5 length Error:" + resultSignature.length + ", MD5 result:" + HexConvert.toHexString(resultSignature));
			byte[] secondSignature = MD5Util.getMD5(md5Bytes);
			if (secondSignature.length  == Constants.SIGNATURE_LENGTH) {
				resultSignature = secondSignature;
				logger.error("MD5 second result is correct:" + HexConvert.toHexString(resultSignature));
			}
		}
		*/

		byte[] completeBytes = new byte[crcDataLength + header.getTRANSPORT_CRC_SIZE()];
		System.arraycopy(crcBytes, 0, completeBytes, 0, crcDataLength);
		System.arraycopy(new byte[]{resultCrc}, 0, completeBytes, crcDataLength, header.getTRANSPORT_CRC_SIZE());
		return completeBytes;
	}	
	
	public DownlinkTransportMessage mockDownlinkTransportMessage(String deviceId, int serviceType, byte[] serviceData) {
		return mockDownlinkTransportMessage(deviceId, serviceType, serviceData, 2, 1, 0, null, null);
	}

	public DownlinkTransportMessage mockDownlinkTransportMessage(String deviceId, int serviceType, byte[] serviceData,
			int serviceDataEncoding, int serviceDataProtocolVersion, int serviceDataEncryption,
			Integer eventId, Integer messageId) {
		DownlinkTransportMessage message = new DownlinkTransportMessage();
		
		MessageHeader header = new MessageHeader();
		header.setTransportDataEncoding((byte)2);
		header.setTransportDataLength((short)(UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH + (serviceData == null ? 0 : serviceData.length)));
		header.setTransportVersion((byte)1);
		message.setHeader(header);
		
		message.setDeviceId(deviceId);
		message.setNodeId("");
		message.setServiceType(serviceType);
		message.setServiceData(serviceData);
		message.setServiceDataSize(serviceData == null ? 0 : serviceData.length);
		ShowFlakeIdGanerator snowFlake = new ShowFlakeIdGanerator(2, 3);
		message.setMessageId(messageId == null ? snowFlake.nextRandId(4) : messageId);
		message.setEventId(eventId == null ? snowFlake.nextRandId(4) : eventId);
		message.setMessageCreationTime(new Date());
		message.setServiceDataEncoding(serviceDataEncoding);
		message.setServiceDataProtocolVersion(serviceDataProtocolVersion);
		message.setServiceDataEncryption(serviceDataEncryption);
		message.setServiceVersion((short)1);
	    return message;
	}
}
