package com.proxy.transport;

import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.proxy.Constants;
import com.proxy.Constants.Status;
import com.proxy.auth.GatewayAuthConstants;
import com.proxy.auth.GatewayAuthService;
import com.proxy.codec.DecodeException;
import com.proxy.codec.MessageCodeHandler;
import com.proxy.handler.DownlinkProcessor;
import com.proxy.handler.UplinkHandler;
import com.proxy.handler.UplinkProcessor;
import com.proxy.model.CommonTransportMessage;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.Message;
import com.proxy.model.MessageHeader;
import com.proxy.model.ServiceRequest;
import com.proxy.model.UplinkTransportMessage;
import com.proxy.persistence.DownlinkMessageService;
import com.proxy.persistence.UplinkMessageService;
import com.proxy.persistence.DeviceInfoService;
import com.proxy.persistence.model.DownlinkMessage;
import com.proxy.persistence.model.UplinkMessage;
import com.proxy.netty.ChannelRegistry;
import com.proxy.persistence.model.DeviceInfo;
import com.proxy.utils.ByteUtil;
import com.proxy.utils.HexConvert;
import com.proxy.utils.MD5Util;
import com.proxy.utils.TimeToLiveHandler;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@Service
public class TransportUplinkProcessor implements UplinkProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	Environment environment;

	@Autowired
	private TransportDownlinkProcessor transportDownlinkProcessor;

	@Autowired
	private MessageCodeHandler messageCodeHandler;

	@Autowired
	private TimeToLiveHandler timeToLiveHandler;

	@Autowired
	private DeviceInfoService deviceInfoService;

	@Autowired
	private GatewayAuthService gatewayAuthService;

	@Autowired
	private DownlinkMessageService downlinkMessageService;

	@Autowired
	private UplinkMessageService uplinkMessageService;
	
	@Autowired
	ResendDownlinkProcessor resendDownlinkProcessor;
	
	@Autowired
	private RabbitTemplate rabbitTemplate;

	private static final String UPLINK_EXCHANGE = "iot.exchange";
	private static final String UPLINK_ROUTING_KEY = "proxy.uplink";
	
    @Autowired
    private LinkedBlockingQueue<Message> receiveQueue;

    @Autowired
    private ChannelRegistry channelRegistry;
    
	private volatile boolean isRunning = true; // 安全关闭线程

	private UplinkProcessThread uplinkProcessThread;
	
	public void startup() {
		try {
			uplinkProcessThread = new UplinkProcessThread();
			new Thread(uplinkProcessThread, "Uplink-Process-Thread").start();
		} catch (final Exception e) {
			logger.error("Failed to initialize UplinkProcessThread");
			throw new RuntimeException(e);
		}		
	}
	
	private class UplinkProcessThread implements Runnable {
		public void run() {
            while (isRunning) {
                try {
                    // take() 会阻塞，直到有消息 → 不浪费CPU
                    Message message = receiveQueue.take();
                    //Message message = receiveQueue.poll();

                    if(message != null) {
                    // 日志 + 处理（同一个队列取出的消息）
	                    logger.info(" -> 处理消息: ip={}, port={}, 数据长度={}",
	                            message.getIp(),
	                            message.getPort(),
	                            message.getPayload() == null ? 0 : message.getPayload().length);
	
	                    // 真正处理
	                    process(message);
                    } 
   					//wait(10);
                } catch (InterruptedException e) {
                    logger.info("消费线程被中断，退出循环");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("消息处理异常", e);
                }
            }
		}
	}
	
    public void putMessage2ReceiveQueue(Message uplinkMessage) {
    	if(uplinkMessage != null) {
    		receiveQueue.offer(uplinkMessage);
    	}
    }
    
	public void process(Message uplinkMessage) {
		final Message uplinkMsg = uplinkMessage;
		long before = System.currentTimeMillis();
		final byte[] completeMessage = uplinkMessage.getPayload();

		if (completeMessage == null) {
			logger.info("From " + uplinkMsg.getIp() + ":" + uplinkMsg.getPort() + ",  the uplink message is null: " + uplinkMessage.getIp() + ":" + uplinkMessage.getPort());
		}

		UplinkTransportMessage decodedUplinkTransportMessage = null;
		logger.debug("TransportUplinkProcessor process data from " + uplinkMsg.getIp() + ":" + uplinkMsg.getPort() + " size:" + completeMessage.length);
		try {
			// decode the coming transport message
			decodedUplinkTransportMessage = decodeUplinkTransportMessage(uplinkMsg, completeMessage);
		} catch (DecodeException e) {
			logger.error("From " + uplinkMsg.getIp() + ":" + uplinkMsg.getPort() + ",  Error to decode the coming message, " + e.getMessage());
			logger.error("Decode error message:" + e.getMessage());
			logger.error("Coming message:" + HexConvert.toHexString(completeMessage));
			return;
		} catch(Exception err) {
			err.printStackTrace();
			logger.error(err.getMessage());
		}

		logger.debug("Device  Id      : " + decodedUplinkTransportMessage.getDeviceId());
		logger.debug("Node    Id      : " + decodedUplinkTransportMessage.getNodeId());
		logger.debug("Service type    : " + decodedUplinkTransportMessage.getServiceType());
		logger.debug("Service version : " + decodedUplinkTransportMessage.getServiceVersion());
		logger.debug("Message Id      : " + decodedUplinkTransportMessage.getMessageId());
		logger.debug("Event   Id      : " + decodedUplinkTransportMessage.getEventId());		
		logger.debug("Message size    : " + completeMessage.length);
		logger.debug("ServiceData size: " + decodedUplinkTransportMessage.getServiceDataSize());
		//logger.info("Event Id: " + decodedUplinkTransportMessage.getEventId());
		
		// ===== 网关认证（传输版本 2）=====
		// 记录连接事件（同一 TCP 连接只记一次，设备换连接视为重连）
		gatewayAuthService.recordConnection(decodedUplinkTransportMessage.getDeviceId(), uplinkMessage.getIp(), uplinkMessage.getPort());

		// 认证请求在 ProxyTcp 终结，不进入 ACK/去重/MQ 流程
		if (decodedUplinkTransportMessage.getServiceType() == GatewayAuthConstants.SERVICE_TYPE_GATEWAY_AUTH_REQUEST) {
			handleGatewayAuthRequest(decodedUplinkTransportMessage, uplinkMessage);
			return;
		}

		// 将 deviceId 绑定到 Netty Channel，供下行按 deviceId 取 Channel 写回
		channelRegistry.bindDevice(decodedUplinkTransportMessage.getDeviceId(), uplinkMessage.getIp(), uplinkMessage.getPort());
		logDeviceConnectStatus(decodedUplinkTransportMessage.getDeviceId(), uplinkMessage.getIp(), uplinkMessage.getPort(), decodedUplinkTransportMessage.getHeader());

		// token 校验：认证通过前的报文一律拒绝，不转发 MQ
		boolean tokenValid = gatewayAuthService.validateToken(decodedUplinkTransportMessage.getDeviceId(),
				decodedUplinkTransportMessage.getToken(), uplinkMessage.getIp(), uplinkMessage.getPort());

		// ack message which comes from vehicle, it will stop the process after deleting the send record
		if (decodedUplinkTransportMessage.getServiceType() == Constants.SERVICETYPE_ACK) {
			if (!tokenValid) {
				// 网关 ACK 校验失败仅记录丢弃，不回 NAK，避免 ACK 风暴
				logger.warn("Gateway ACK rejected (unauthenticated or invalid token): deviceId="
						+ decodedUplinkTransportMessage.getDeviceId() + ", ip=" + uplinkMessage.getIp() + ":" + uplinkMessage.getPort()
						+ ", messageId=" + decodedUplinkTransportMessage.getMessageId());
				return;
			}
			logger.info("Received ack message from " + decodedUplinkTransportMessage.getDeviceId() + " for service type " + decodedUplinkTransportMessage.getServiceType() + " with message id " + decodedUplinkTransportMessage.getMessageId());
			dealAckMessage(decodedUplinkTransportMessage);
			long after = System.currentTimeMillis();
			logger.info("Spend " + (after - before) + " millisecond to process ack message from vin " + decodedUplinkTransportMessage.getDeviceId()  + " with messge id " + decodedUplinkTransportMessage.getMessageId());
		} else {
			if (!tokenValid) {
				int nakCode = gatewayAuthService.hasActiveSession(decodedUplinkTransportMessage.getDeviceId())
						? GatewayAuthConstants.NAK_CODE_TOKEN_INVALID
						: GatewayAuthConstants.NAK_CODE_NOT_AUTHENTICATED;
				logger.warn("Gateway uplink rejected (token check failed, nakCode=" + nakCode + "): deviceId="
						+ decodedUplinkTransportMessage.getDeviceId() + ", ip=" + uplinkMessage.getIp() + ":" + uplinkMessage.getPort()
						+ ", serviceType=" + decodedUplinkTransportMessage.getServiceType()
						+ ", messageId=" + decodedUplinkTransportMessage.getMessageId());
				sendNakMessage(decodedUplinkTransportMessage, nakCode);
				return;
			}
			try {
				logger.debug("Try to send ack message to vehicle : " + decodedUplinkTransportMessage.getDeviceId() + " for service type " + decodedUplinkTransportMessage.getServiceType() + " with message id " + decodedUplinkTransportMessage.getMessageId());
				sendAckMessage(decodedUplinkTransportMessage);
			} catch (Exception e) {
				logger.error("Failed to send ack message to vehicle : " + decodedUplinkTransportMessage.getDeviceId() + " for service type " + decodedUplinkTransportMessage.getServiceType() + " with message id " + decodedUplinkTransportMessage.getMessageId());
				logger.error("Error message : " + e.getMessage());
			}
	
			// duplicated uplink messages(with the same messageId coming from the same vehicleIdentifier) will be discarded
			boolean isDuplicatedUplinkMessage = isDuplicatedUplinkMessage(decodedUplinkTransportMessage);
			if (isDuplicatedUplinkMessage) {
				logger.debug("From " + uplinkMsg.getIp() + ":" + uplinkMsg.getPort() + ", received duplicated message from vehicle : " + decodedUplinkTransportMessage.getDeviceId() + " for service type " + decodedUplinkTransportMessage.getServiceType() + " with message id " + decodedUplinkTransportMessage.getMessageId());
			} else {
				this.persistUplinkMessage(decodedUplinkTransportMessage);
				
				// Stop process if message TTL has expired
				if (timeToLiveHandler.hasExpired(decodedUplinkTransportMessage)) {
					//logger.info("the uplink message has expired:" + uplinkMessage.getIp() + ":" + uplinkMessage.getPort());
					//ignore TTL temporary
					//return;
				}
		
				String serviceProxy = environment.getProperty(Constants.SERVICEPROXY);
				if (serviceProxy == null) {
					logger.error("Couldn't find the service proxy, please setting " + Constants.SERVICEPROXY);
					return;
				}
		
				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setDeviceId(decodedUplinkTransportMessage.getDeviceId());
				serviceRequest.setNodeId(decodedUplinkTransportMessage.getNodeId());
				//serviceRequest.setMessageId(decodedUplinkTransportMessage.getMessageId());
				//serviceRequest.setEventCreationTime(decodedUplinkTransportMessage.getMessageCreationTime());
				serviceRequest.setEventId(decodedUplinkTransportMessage.getEventId());
				serviceRequest.setServiceDataEncoding(decodedUplinkTransportMessage.getServiceDataEncoding());
				serviceRequest.setServiceDataProtocolVersion(decodedUplinkTransportMessage.getServiceDataProtocolVersion());
				serviceRequest.setServiceEncryption(decodedUplinkTransportMessage.getServiceDataEncryption());
				serviceRequest.setServiceType(decodedUplinkTransportMessage.getServiceType());
				serviceRequest.setServiceVersion(decodedUplinkTransportMessage.getServiceVersion());
				serviceRequest.setServiceData(decodedUplinkTransportMessage.getServiceData());
		
				try {
					callMQService(serviceRequest);
				} catch (Exception e) {
					logger.error("Failed to call platform : " + decodedUplinkTransportMessage.getDeviceId() + " for service type " + decodedUplinkTransportMessage.getServiceType() + " with message id " + decodedUplinkTransportMessage.getMessageId());
					logger.error("Error message : " + e.getMessage());
				}
				long after = System.currentTimeMillis();
				logger.debug("Spend " + (after - before) + " millisecond to process the uplink message from " + decodedUplinkTransportMessage.getDeviceId() + " for service type " + decodedUplinkTransportMessage.getServiceType() + " with message id " + decodedUplinkTransportMessage.getMessageId());
			}
		}
		
	}

	private void callMQService(ServiceRequest serviceRequest) {
		try {
			Gson gson = com.proxy.utils.JsonUtils.gson();
			String requestJsonString = gson.toJson(serviceRequest);
			logger.info("Call MQ Service : " + requestJsonString);
			MessagePostProcessor postProcessor = message -> {
				message.getMessageProperties().setContentType("application/json");
				return message;
			};
			rabbitTemplate.convertAndSend(UPLINK_EXCHANGE, UPLINK_ROUTING_KEY,
					requestJsonString.getBytes(StandardCharsets.UTF_8), postProcessor);
			logger.info("Published uplink ServiceRequest to " + UPLINK_EXCHANGE + "/" + UPLINK_ROUTING_KEY + " for device " + serviceRequest.getDeviceId());
		} catch (Exception e) {
			logger.error("Failed to publish uplink ServiceRequest to MQ for device "
					+ serviceRequest.getDeviceId() + ": " + e.getMessage(), e);
		}
	}
	
	private void callWebService(String url, ServiceRequest serviceRequest) {
		try {
			ClientConfig config = new DefaultClientConfig();
			Client client = Client.create(config);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").post(ClientResponse.class,serviceRequest);
			response.getStatus();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error to call the backend service, device:" + serviceRequest.getDeviceId() + ",service type:" + serviceRequest.getServiceType());
			logger.error(e.getMessage());
		}
	}

	private int logDeviceConnectStatus(String deviceId, String ip, int port, MessageHeader header) {
		DeviceInfo status = new DeviceInfo();
		status.setDeviceId(deviceId);
		status.setLastConnectedTime(new Date());
		status.setIp(ip);
		status.setPort(port);
		status.setStatus(1);
		status.setTransportProtocol(header.getTransportDataEncoding());
		status.setTransportProtocolVersion(header.getTransportVersion());
		return deviceInfoService.updateDeviceStatus(status);
	}

	private UplinkTransportMessage decodeUplinkTransportMessage(Message uplinkMsg, byte[] payload) throws DecodeException{
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
			logger.error("From " + uplinkMsg.getIp() + ":" + uplinkMsg.getPort() + "(gateway signature=" + resultSignature + ")!=(proxy signature=" + payload[payload.length - header.getTRANSPORT_CRC_SIZE()] + ")");
			throw new DecodeException(DecodeException.ERRORCODE_SIGNATURE_NOT_SAME, "(gateway signature=" + resultSignature + ")!=(proxy signature=" + payload[payload.length - header.getTRANSPORT_CRC_SIZE()] + ")");
		}

		//int serviceDataLength = payload.length - header.getMESSAGE_HEADER_LENGTH() - header.getTransportDataLength() - (int) header.getTRANSPORT_CRC_SIZE();
		int serviceDataLength = header.getTransportDataLength() - UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH;
		
		byte[] serviceData = new byte[serviceDataLength];
		if (serviceDataLength > 0) {
			System.arraycopy(payload, (header.getMESSAGE_HEADER_LENGTH() + UplinkTransportMessage.TRANSPORT_PART_FIX_LENGTH),serviceData, 0, serviceDataLength);
		}

		decodedUplinkTransportMessage.setHeader(header);
		decodedUplinkTransportMessage.setPackageSize(payload.length);
		decodedUplinkTransportMessage.setServiceData(serviceData);
		decodedUplinkTransportMessage.setServiceDataSize(serviceDataLength);
		
		return decodedUplinkTransportMessage;
	}

	private void sendAckMessage(UplinkTransportMessage decodedUplinkTransportMessage) {
		DownlinkTransportMessage downlinkMsg = new DownlinkTransportMessage();
		downlinkMsg.setHeader(decodedUplinkTransportMessage.getHeader());
		downlinkMsg.setDeviceId(decodedUplinkTransportMessage.getDeviceId());
		downlinkMsg.setNodeId(decodedUplinkTransportMessage.getNodeId());
		downlinkMsg.setMessageCreationTime(decodedUplinkTransportMessage.getMessageCreationTime());
		downlinkMsg.setMessageId(decodedUplinkTransportMessage.getMessageId());
		downlinkMsg.setServiceType(Constants.SERVICETYPE_ACK);
		downlinkMsg.setServiceVersion(decodedUplinkTransportMessage.getServiceVersion());
		downlinkMsg.setServiceDataEncoding(decodedUplinkTransportMessage.getServiceDataEncoding());
		downlinkMsg.setServiceDataProtocolVersion(decodedUplinkTransportMessage.getServiceDataProtocolVersion());
		downlinkMsg.setServiceDataEncryption(decodedUplinkTransportMessage.getServiceDataEncryption());
		downlinkMsg.setEventId(decodedUplinkTransportMessage.getEventId());
		downlinkMsg.setServiceData(null);
		transportDownlinkProcessor.process(downlinkMsg);
	}

	/**
	 * 处理网关认证请求：校验 gateway_id + 预共享密钥，签发/拒绝 token 并回认证响应。
	 * 响应 serviceData：result(1) + token(10，成功时)；token 字段成功时为新 token。
	 */
	private void handleGatewayAuthRequest(UplinkTransportMessage authRequest, Message rawMsg) {
		logger.info("Gateway auth request: deviceId=" + authRequest.getDeviceId()
				+ ", ip=" + rawMsg.getIp() + ":" + rawMsg.getPort());
		GatewayAuthService.AuthResult result = gatewayAuthService.handleAuthRequest(
				authRequest.getDeviceId(), authRequest.getServiceData(), rawMsg.getIp(), rawMsg.getPort());

		DownlinkTransportMessage authResponse = new DownlinkTransportMessage();
		authResponse.setHeader(authRequest.getHeader());
		authResponse.setDeviceId(authRequest.getDeviceId());
		authResponse.setNodeId(authRequest.getNodeId());
		authResponse.setMessageCreationTime(new Date());
		authResponse.setMessageId(authRequest.getMessageId());
		authResponse.setServiceType(GatewayAuthConstants.SERVICE_TYPE_GATEWAY_AUTH_RESPONSE);
		authResponse.setServiceVersion(authRequest.getServiceVersion());
		authResponse.setServiceDataEncoding(Constants.ENCODINGTYPE_BYTE);
		authResponse.setServiceDataProtocolVersion(authRequest.getServiceDataProtocolVersion());
		authResponse.setServiceDataEncryption(authRequest.getServiceDataEncryption());
		authResponse.setEventId(authRequest.getEventId());

		if (result.success) {
			authResponse.setToken(result.token);
			byte[] serviceData = new byte[1 + CommonTransportMessage.TOKEN_LENGTH];
			serviceData[0] = (byte) GatewayAuthConstants.AUTH_RESULT_SUCCESS;
			System.arraycopy(result.token, 0, serviceData, 1, CommonTransportMessage.TOKEN_LENGTH);
			authResponse.setServiceData(serviceData);
		} else {
			authResponse.setServiceData(new byte[] { (byte) result.code });
		}

		try {
			transportDownlinkProcessor.process(authResponse);
			logger.info("Gateway auth response sent: deviceId=" + authRequest.getDeviceId()
					+ ", result=" + (result.success ? "SUCCESS" : "code=" + result.code));
		} catch (Exception e) {
			logger.error("Failed to send auth response to gateway: deviceId=" + authRequest.getDeviceId()
					+ ", error=" + e.getMessage());
		}
	}

	/**
	 * 发送错误 ACK（NAK）：serviceType=0，serviceData 为 1 字节错误码。
	 */
	private void sendNakMessage(UplinkTransportMessage decodedUplinkTransportMessage, int nakCode) {
		DownlinkTransportMessage nakMsg = new DownlinkTransportMessage();
		nakMsg.setHeader(decodedUplinkTransportMessage.getHeader());
		nakMsg.setDeviceId(decodedUplinkTransportMessage.getDeviceId());
		nakMsg.setNodeId(decodedUplinkTransportMessage.getNodeId());
		nakMsg.setMessageCreationTime(new Date());
		nakMsg.setMessageId(decodedUplinkTransportMessage.getMessageId());
		nakMsg.setServiceType(Constants.SERVICETYPE_ACK);
		nakMsg.setServiceVersion(decodedUplinkTransportMessage.getServiceVersion());
		nakMsg.setServiceDataEncoding(decodedUplinkTransportMessage.getServiceDataEncoding());
		nakMsg.setServiceDataProtocolVersion(decodedUplinkTransportMessage.getServiceDataProtocolVersion());
		nakMsg.setServiceDataEncryption(decodedUplinkTransportMessage.getServiceDataEncryption());
		nakMsg.setEventId(decodedUplinkTransportMessage.getEventId());
		nakMsg.setServiceData(new byte[] { (byte) nakCode });
		transportDownlinkProcessor.process(nakMsg);
	}

	private void dealAckMessage(UplinkTransportMessage decodedUplinkTransportMessage) {
		int deletedCount = downlinkMessageService.deleteDownlinkMessageByMessageId(decodedUplinkTransportMessage.getDeviceId(), decodedUplinkTransportMessage.getMessageId());
//		DownlinkMessage status = downlinkMessageService.getDownlinkMessageByMessageId(decodedUplinkTransportMessage.getVehicleIdentifier(), decodedUplinkTransportMessage.getMessageId());
//		if (status != null) {
//			int deletedCount = downlinkMessageService.deleteDownlinkMessageByMessageId(decodedUplinkTransportMessage.getVehicleIdentifier(), decodedUplinkTransportMessage.getMessageId());
//		} else {
		if (deletedCount == 0) {
			logger.info("Can't find the corresponding messageid in platform, message id = "  + decodedUplinkTransportMessage.getMessageId());
			logger.info("vin = "  + decodedUplinkTransportMessage.getDeviceId());
			//logger.info("eventId = "  + decodedUplinkTransportMessage.getEventId());
		}
	}

	private boolean isDuplicatedUplinkMessage(UplinkTransportMessage decodedUplinkTransportMessage) {
		UplinkMessage uplinkMessage = uplinkMessageService.getUplinkMessageById(decodedUplinkTransportMessage.getDeviceId(), decodedUplinkTransportMessage.getMessageId());
		if (uplinkMessage != null)
			return true;
		else
			return false;
	}
	
	private void persistUplinkMessage(UplinkTransportMessage decodedUplinkTransportMessage) {
		UplinkMessage cachedUplinkMessage = new UplinkMessage();
		cachedUplinkMessage.setDeviceId(decodedUplinkTransportMessage.getDeviceId());
		cachedUplinkMessage.setMessageId(decodedUplinkTransportMessage.getMessageId());
//		cachedUplinkMessage.setReceivedTime(decodedUplinkTransportMessage.getMessageCreationTime()new Date());
		cachedUplinkMessage.setReceivedTime(new Date());
		uplinkMessageService.saveUplinkMessage(cachedUplinkMessage);
	}
	
	private void resendVehicleDownlinkMessages(String  deviceId) {
		DeviceInfo deviceInfo = deviceInfoService.queryDeviceInfo(deviceId);
		if(deviceInfo != null) {
			//1. query all the downlink messages for this vehicle
			List<DownlinkMessage> cachedDownlinkMessageList = downlinkMessageService.findVehicleDownlinkMessagesExceptStatus(deviceInfo.getDeviceId(), Status.FAILED);
					
			//2. resend the downlink message
			int size = cachedDownlinkMessageList == null ? 0 : cachedDownlinkMessageList.size();
			logger.info("Cached downlink message size = " + size + " for device: " +  deviceInfo.getDeviceId());
			for(int i = 0; i < size; i ++) {
				cachedDownlinkMessageList.get(i).setIp(deviceInfo.getIp());
				cachedDownlinkMessageList.get(i).setPort(deviceInfo.getPort());
				logger.info("Send downlink message index : " + (i + 1));
				resendDownlinkProcessor.resend(cachedDownlinkMessageList.get(i));
				try {
					Thread.sleep(500);
				}catch(Exception err) {}
			}
		}
	}
}