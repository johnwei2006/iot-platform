package com.proxy.codec.v2;

import java.util.Date;

import com.proxy.codec.DecodeException;
import com.proxy.codec.EncodeException;
import com.proxy.codec.TransportCoder;
import com.proxy.model.CommonTransportMessage;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.UplinkTransportMessage;
import com.proxy.utils.ByteUtil;

public class TransportcoderByteImplV2  implements TransportCoder {

	@Override
	public boolean isSupportedVersion(byte[] data) {
		return true;
	}

	@Override
	public UplinkTransportMessage decode(byte[] transportMessage) throws DecodeException{
		UplinkTransportMessage message = new UplinkTransportMessage();
		
    	String 	deviceId;
    	String 	nodeId;
    	Date 	messageCreationTime;
    	int  	messageId;
    	short 	serviceType;
    	short 	serviceVersion;
    	short 	serviceDataEncoding;
    	short 	serviceDataProtocolVersion;
    	short 	serviceDataEncryption;
    	int 	eventId;
    	
    	//byte[] transportBytes = new byte[11+11+6+4+2+5]; //35 bytes
    	byte[] deviceIdBytes 			= new byte[11];
    	byte[] nodeIdBytes 				= new byte[11];
    	byte[] messageCreationTimeBytes = new byte[6];
    	byte[] messageIdBytes 			= new byte[4];
    	byte[] eventIdBytes				= new byte[4];
    	
    	System.arraycopy(transportMessage, 0, deviceIdBytes, 0, 11);
    	deviceId = new String(deviceIdBytes).trim();
    	System.arraycopy(transportMessage, 11, nodeIdBytes, 0, 11);
    	nodeId = new String(nodeIdBytes).trim();
    	System.arraycopy(transportMessage, 22, messageCreationTimeBytes, 0, 6);
    	messageCreationTime = new Date();
    	messageCreationTime.setYear(messageCreationTimeBytes[0]+100);
    	messageCreationTime.setMonth(messageCreationTimeBytes[1]-1);
    	messageCreationTime.setDate(messageCreationTimeBytes[2]);
    	messageCreationTime.setHours(messageCreationTimeBytes[3]);
    	messageCreationTime.setMinutes(messageCreationTimeBytes[4]);
    	messageCreationTime.setSeconds(messageCreationTimeBytes[5]);
    	
    	System.arraycopy(transportMessage, 28, messageIdBytes, 0, 4);
    	messageId = ByteUtil.byte4BigArray2Int(messageIdBytes);

   		serviceType 				= transportMessage[32];
       	serviceVersion 				= transportMessage[33];
       	serviceDataEncoding 		= transportMessage[34];
       	serviceDataProtocolVersion 	= transportMessage[35];
       	serviceDataEncryption 		= transportMessage[36];

    	System.arraycopy(transportMessage, 37, eventIdBytes, 0, 4);
    	eventId = ByteUtil.byte4BigArray2Int(eventIdBytes);

    	message.setDeviceId(deviceId);
    	message.setNodeId(nodeId);
    	message.setMessageCreationTime(messageCreationTime);
    	message.setMessageId(messageId);
    	message.setServiceType(serviceType);
    	message.setServiceVersion(serviceVersion);
    	message.setServiceDataEncoding(serviceDataEncoding);
    	message.setServiceDataProtocolVersion(serviceDataProtocolVersion);
    	message.setServiceDataEncryption(serviceDataEncryption);   	
    	message.setEventId(eventId);
    	
    	return message;
	}

	@Override
	public byte[] encode(DownlinkTransportMessage downlinkTransportMessage) throws EncodeException{
    	String 	deviceId 			= downlinkTransportMessage.getDeviceId();
    	String 	nodeId 				= downlinkTransportMessage.getNodeId();
    	Date 	messageCreationTime = downlinkTransportMessage.getMessageCreationTime();
    	int 	messageId 			= downlinkTransportMessage.getMessageId();
    	short 	serviceType 		= (short)downlinkTransportMessage.getServiceType();
    	short 	serviceVersion 		= downlinkTransportMessage.getServiceVersion();
    	short 	serviceDataEncoding = (short)downlinkTransportMessage.getServiceDataEncoding();
    	short 	serviceDataProtocolVersion = (short)downlinkTransportMessage.getServiceDataProtocolVersion();
    	short 	serviceDataEncryption = (short)downlinkTransportMessage.getServiceDataEncryption();
    	int   	eventId 			= downlinkTransportMessage.getEventId();
    	
    	byte[] transportBytes = new byte[CommonTransportMessage.TRANSPORT_PART_FIX_LENGTH];
    	System.arraycopy(deviceId.getBytes(), 0, transportBytes, 0, deviceId.length() > 11 ? 11 : deviceId.length());
    	if(nodeId != null) {
    		System.arraycopy(nodeId.getBytes(), 0, transportBytes, 11, nodeId.length() > 11 ? 11 : nodeId.length());
    	}
    	byte[] dateByte = new byte[6];
    	System.arraycopy(new byte[]{(byte)(messageCreationTime.getYear()-100)}, 0, dateByte, 0, 1);
    	System.arraycopy(new byte[]{(byte)(messageCreationTime.getMonth()+1)}, 0, dateByte, 1, 1);
    	System.arraycopy(new byte[]{(byte)messageCreationTime.getDate()}, 0, dateByte, 2, 1);
    	System.arraycopy(new byte[]{(byte)messageCreationTime.getHours()}, 0, dateByte, 3, 1);
    	System.arraycopy(new byte[]{(byte)messageCreationTime.getMinutes()}, 0, dateByte, 4, 1);
    	System.arraycopy(new byte[]{(byte)messageCreationTime.getSeconds()}, 0, dateByte, 5, 1);
    	
    	System.arraycopy(dateByte, 0, transportBytes, 22, 6);

//    	byte[] msgHexBytes = HexConvert.fromHexString(messageId);
    	byte[] msgIdHexBytes = ByteUtil.int2BigByte4Array(messageId);
    	System.arraycopy(msgIdHexBytes, 0, transportBytes, 28, msgIdHexBytes.length > 4 ? 4 : msgIdHexBytes.length);
    	//System.arraycopy(ByteUtil.short2Byte2Array(serviceType), 0, transportBytes, 32, 2);
    	System.arraycopy(new byte[]{(byte)serviceType}, 0, transportBytes, 32, 1);
    	System.arraycopy(new byte[]{(byte)serviceVersion}, 0, transportBytes, 33, 1);
    	System.arraycopy(new byte[]{(byte)serviceDataEncoding}, 0, transportBytes, 34, 1);
    	System.arraycopy(new byte[]{(byte)serviceDataProtocolVersion}, 0, transportBytes, 35, 1);
    	System.arraycopy(new byte[]{(byte)serviceDataEncryption}, 0, transportBytes, 36, 1);
    	
    	byte[] eventIdHexBytes = ByteUtil.int2BigByte4Array(eventId);
    	System.arraycopy(eventIdHexBytes, 0, transportBytes, 37, eventIdHexBytes.length > 4 ? 4 : eventIdHexBytes.length);
   	
    	return transportBytes;
	}

}
