package com.proxy;

import java.util.Date;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.ResendMessage;
import com.proxy.model.ServiceRequest;
import com.proxy.model.ServiceResponse;
import com.proxy.persistence.DownlinkMessageService;
import com.proxy.persistence.UplinkMessageService;
import com.proxy.persistence.DeviceInfoService;
import com.proxy.persistence.model.DownlinkMessage;
import com.proxy.persistence.model.UplinkMessage;
import com.proxy.persistence.model.DeviceInfo;
import com.proxy.netty.ChannelRegistry;
import com.proxy.transport.ResendDownlinkProcessor;
import com.proxy.transport.TransportDownlinkProcessor;
import com.proxy.utils.ShowFlakeIdGanerator;

@Controller
public class ProxyController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	TransportDownlinkProcessor transportDownlinkProcessor;
	
	@Autowired
	DeviceInfoService deviceInfoService;
	
	@Autowired
	DownlinkMessageService downlinkMessageService;
	
	@Autowired
	UplinkMessageService uplinkMessageService;

	@Autowired
	ResendDownlinkProcessor resendDownlinkProcessor;

	@Autowired
	private ChannelRegistry channelRegistry;

	@RequestMapping("/ping/{pong}")
	@ResponseBody
	public String hello(@PathVariable String pong) {
		return pong;
	}
	
	@RequestMapping("/downlink")
	@ResponseBody
	@Produces({MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_JSON})
	public ServiceResponse downlink(@RequestBody ServiceRequest message) {
		long before = System.currentTimeMillis();
		logger.info("Proxy Controller start to process downlink message eventId=" + message.getEventId() + " for " + message.getDeviceId());
		
		DownlinkTransportMessage transportMessage = new DownlinkTransportMessage();
		transportMessage.setEventId(message.getEventId());

		ShowFlakeIdGanerator snowFlake = new ShowFlakeIdGanerator(2, 3);
		transportMessage.setMessageId(snowFlake.nextRandId(4));
		transportMessage.setMessageCreationTime(new Date());
		transportMessage.setServiceData(message.getServiceData());
		transportMessage.setServiceDataEncoding(message.getServiceDataEncoding());
		transportMessage.setServiceDataProtocolVersion(message.getServiceDataProtocolVersion());
		transportMessage.setServiceDataEncryption(message.getServiceEncryption());
		transportMessage.setServiceType(message.getServiceType());
		transportMessage.setServiceVersion(message.getServiceVersion());
		transportMessage.setDeviceId(message.getDeviceId());
		transportMessage.setNodeId(message.getNodeId());
		
		ServiceResponse response = transportDownlinkProcessor.process(transportMessage);
		long after = System.currentTimeMillis();
		logger.info("Spend " + (after - before) + " millisecond to process the downlink message from " + message.getDeviceId() + " for service type " + message.getServiceType() + " with event id " + message.getEventId());;
		return response;
		
	}
	
	@RequestMapping("/resendDownlink")
	@ResponseBody
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public ServiceResponse resendDownlink(@RequestBody ResendMessage message) {
		ServiceResponse serviceResponse = new ServiceResponse();
		DeviceInfo vehicleStatus = deviceInfoService.queryDeviceInfo(message.getDeviceId());
		if (vehicleStatus == null) {
			logger.error("No terminal information found for vehicle:" + message.getDeviceId());
			serviceResponse.setStatus(Constants.STATUS_ERROR);
			serviceResponse.setMessage("No terminal information found for " + message.getDeviceId() + ", please check!");
			return serviceResponse;
		}
		if (!channelRegistry.getOnlineDeviceIds().contains(message.getDeviceId())) {
			logger.warn("Device " + message.getDeviceId() + " is not online, skip manual resend");
			serviceResponse.setStatus(Constants.STATUS_ERROR);
			serviceResponse.setMessage("Device " + message.getDeviceId() + " is not online, cannot resend now");
			return serviceResponse;
		}

		DownlinkMessage downlinkSendStatus = new DownlinkMessage();
		downlinkSendStatus.setIp(vehicleStatus.getIp());
		downlinkSendStatus.setPort(vehicleStatus.getPort());
		downlinkSendStatus.setMessageBytes(message.getMessageByteHex().getBytes());
		downlinkSendStatus.setMessageTime(new Date());
		
		resendDownlinkProcessor.resend(downlinkSendStatus);
		serviceResponse.setStatus(Constants.STATUS_SUCCESS);
		
		return serviceResponse;
	}

	@RequestMapping("/DownlinkMessages")
	@ResponseBody
	@Produces({MediaType.APPLICATION_JSON})
	public List<DownlinkMessage> queryAllNotAckRecords() {
		return downlinkMessageService.queryAllNotAckRecords();
	}

	@RequestMapping("/UplinkMessages")
	@ResponseBody
	@Produces({MediaType.APPLICATION_JSON})
	public List<UplinkMessage> queryAllUplinkMessages() {
		return uplinkMessageService.queryAllUplinkMessages();
	}

	@RequestMapping("/DeviceInfos")
	@ResponseBody
	@Produces({MediaType.APPLICATION_JSON})
	public List<DeviceInfo> queryAllDeviceInfo() {
		return deviceInfoService.getAllDeviceInfo();
	}

	@RequestMapping("/DeviceInfos/{deviceId}")
	@ResponseBody
	@Produces({MediaType.APPLICATION_JSON})
	public DeviceInfo queryVehicleInfo(@PathVariable String vehicleIdentifier) {
		return deviceInfoService.queryDeviceInfo(vehicleIdentifier);
	}
}
