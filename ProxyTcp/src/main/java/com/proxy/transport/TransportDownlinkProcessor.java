package com.proxy.transport;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.internal.HexUtils;
import com.proxy.Constants;
import com.proxy.auth.GatewayAuthService;
import com.proxy.Constants.Status;
import com.proxy.codec.MessageCodeHandler;
import com.proxy.handler.DownlinkProcessor;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.Message;
import com.proxy.model.MessageHeader;
import com.proxy.model.ServiceResponse;
import com.proxy.model.UplinkTransportMessage;
import com.proxy.persistence.DownlinkMessageService;
import com.proxy.persistence.SendFailureMessageService;
import com.proxy.persistence.DeviceInfoService;
import com.proxy.persistence.model.DownlinkMessage;
import com.proxy.persistence.model.SendFailureMessage;
import com.proxy.persistence.model.DeviceInfo;
import com.proxy.utils.ByteUtil;
import com.proxy.utils.MD5Util;

@Service
public class TransportDownlinkProcessor extends DownlinkProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	DeviceInfoService deviceInfoService;

	@Autowired
	DownlinkMessageService downlinkMessageService;

	@Autowired
	SendFailureMessageService sendFailureMessageService;

	@Autowired
	private MessageCodeHandler messageCodeHandler;

	@Autowired
	private GatewayAuthService gatewayAuthService;

	public ServiceResponse process(DownlinkTransportMessage downlinkMessage) {
		ServiceResponse serviceResponse = new ServiceResponse();
		serviceResponse.setDeviceId(downlinkMessage.getDeviceId());
		serviceResponse.setEventId(downlinkMessage.getEventId());
		serviceResponse.setServiceType(downlinkMessage.getServiceType());
		serviceResponse.setServiceVersion(downlinkMessage.getServiceVersion());
		//serviceResponse.setMessageId(downlinkMessage.getMessageId());
		
		DeviceInfo deviceStatus = deviceInfoService.queryDeviceInfo(downlinkMessage.getDeviceId());
		
		SendFailureMessage sendFailureMessage = new SendFailureMessage();
		sendFailureMessage.setDeviceId(downlinkMessage.getDeviceId());
		sendFailureMessage.setSourceServiceType(downlinkMessage.getServiceType());
		sendFailureMessage.setServiceType(-1);
		sendFailureMessage.setEventId(downlinkMessage.getEventId());
		sendFailureMessage.setMessageId(downlinkMessage.getMessageId());
		if (deviceStatus == null) {
			logger.error("No terminal information found for vehicle:" + downlinkMessage.getDeviceId());
			serviceResponse.setStatus(Constants.STATUS_ERROR);
			serviceResponse.setMessage("No terminal information found for " + downlinkMessage.getDeviceId() + ", please check!");
			
			sendFailureMessage.setErrorCode(Constants.SEND_FAILURE_ERROR_CODE_UNKNOW_VEHICLE);
			sendFailureMessage.setErrorMessage("No terminal information found with " + downlinkMessage.getDeviceId());
			sendFailureMessageService.saveSendFailureMessage(sendFailureMessage);

			return serviceResponse;
		}
		
		Message message = new Message();
		byte[] completeMessage = null;
		try {
			// 填充下行 token：认证响应已显式携带新签发 token；其余报文取当前有效 token，
			// 未认证设备填全 0（由网关侧校验拒绝）
			if (downlinkMessage.getToken() == null) {
				downlinkMessage.setToken(gatewayAuthService.getTokenForDownlink(downlinkMessage.getDeviceId()));
			}
			completeMessage = this.createCompleteMessage(downlinkMessage, deviceStatus);
	
			// 本网关为 TCP（Netty 按连接下发），连接类型由连接层决定，无需设置 UDP
			message.setPayload(completeMessage);
			message.setIp(deviceStatus.getIp());
			message.setPort(deviceStatus.getPort());
			message.setMessageTime(new Date());
			message.setDeviceId(downlinkMessage.getDeviceId());
		} catch(Exception err) {
			logger.error("Error to encode message sending to " + downlinkMessage.getDeviceId() + " for messageId:" + downlinkMessage.getMessageId() + ", error message:" + err.getMessage());
			serviceResponse.setStatus(Constants.STATUS_ERROR);
			serviceResponse.setMessage("Error to encode message sending to " + downlinkMessage.getDeviceId() + " for messageId:" + downlinkMessage.getMessageId() + ", error message:" + err.getMessage());
			
			sendFailureMessage.setErrorCode(Constants.SEND_FAILURE_ERROR_CODE_ENCODE_ERROR);
			sendFailureMessage.setErrorMessage("Error to encode message sending to" + downlinkMessage.getDeviceId() + ", error message:" + err.getMessage());
			sendFailureMessageService.saveSendFailureMessage(sendFailureMessage);
			return serviceResponse;
		}

		DownlinkMessage status = new DownlinkMessage();
		status.setDeviceId(downlinkMessage.getDeviceId());
		status.setEventId(downlinkMessage.getEventId());
		status.setServiceType(downlinkMessage.getServiceType());
		status.setMessageId(downlinkMessage.getMessageId());
		status.setSendTime(new Date());
		status.setLastSendTime(new Date());
		status.setMessageBytes(message.getPayload());
		status.setIp(message.getIp());
		status.setPort(message.getPort());
		status.setMessageTime(message.getMessageTime());
		status.setResendCount(0);
		try {
			send(message);
			status.setStatus(Status.SENT);
			logger.info("Success to send message to device##############################");
			logger.info("Device  Id      : " + downlinkMessage.getDeviceId());
			logger.info("Node    Id      : " + downlinkMessage.getNodeId());
			logger.info("ip/port         : " + message.getIp() + "/" + message.getPort());
			logger.info("Service type    : " + downlinkMessage.getServiceType());
			logger.info("Message id      : " + downlinkMessage.getMessageId());
			logger.info("Event   id      : " + downlinkMessage.getEventId());
			logger.info("Message size    : " + completeMessage.length);
			logger.info("ServiceData size: " + (downlinkMessage.getServiceData() == null ? 0 : downlinkMessage.getServiceData().length));
			logger.info("Message payload : " + HexUtils.toHex(message.getPayload()));
//			if (downlinkMessage.getServiceType() != 0) {
//			    logger.info("Event Id     : " + downlinkMessage.getEventId());
//			}
		} catch (Exception err) {
			status.setStatus(Status.FAILED);
			status.setResendCount(0);
			
			serviceResponse.setStatus(Constants.STATUS_ERROR);
			serviceResponse.setMessage("Exception happened for " + downlinkMessage.getDeviceId() + "," + err.getMessage());
			logger.error("Error to send message to vehicle: " + downlinkMessage.getDeviceId() + " with service type " + downlinkMessage.getServiceType() + " for messageId:" + downlinkMessage.getMessageId());

			sendFailureMessage.setErrorCode(Constants.SEND_FAILURE_ERROR_CODE_OTHER);
			sendFailureMessage.setErrorMessage("Error to send message to vehicle: " + downlinkMessage.getDeviceId() + " for messageId:" + downlinkMessage.getMessageId() + " error:" +  err.getMessage());
			sendFailureMessageService.saveSendFailureMessage(sendFailureMessage);
			return serviceResponse;
		}
		// ACK message don't need to get ACK, then don't need to resend logic
		if (downlinkMessage.getServiceType() != Constants.SERVICETYPE_ACK) {
			downlinkMessageService.updateDownlinkMessage(status);
		}
		serviceResponse.setStatus(Constants.STATUS_SUCCESS);
		return serviceResponse;
	}

	private byte[] createCompleteMessage(DownlinkTransportMessage downlinkMessage, DeviceInfo deviceInfo) throws Exception {

		//header information
		MessageHeader header = new MessageHeader();
		if (deviceInfo == null) {
			header.setTransportDataEncoding((byte)0);
			header.setTransportVersion((byte)1);
		} else {
			header.setTransportDataEncoding((byte)deviceInfo.getTransportProtocol());
			header.setTransportVersion((byte)deviceInfo.getTransportProtocolVersion());
		}

		byte[] serviceData = downlinkMessage.getServiceData();
		short serviceDataLength = serviceData == null ? 0 : (short)serviceData.length;
		header.setTransportDataLength((short)(UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH + serviceDataLength));
		downlinkMessage.setHeader(header);
		
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
}
