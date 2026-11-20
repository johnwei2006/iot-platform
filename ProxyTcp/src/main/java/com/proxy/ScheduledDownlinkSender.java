package com.proxy;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.ServiceRequest;
import com.proxy.model.ServiceResponse;
import com.proxy.persistence.DeviceInfoService;
import com.proxy.persistence.model.DeviceInfo;
import com.proxy.transport.TransportDownlinkProcessor;
import com.proxy.utils.ShowFlakeIdGanerator;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

//@Service("ScheduledDownlinkSender")
public class ScheduledDownlinkSender
{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	TransportDownlinkProcessor transportDownlinkProcessor;
	
	@Autowired
	DeviceInfoService deviceInfoService;
	
	@Autowired
	Environment environment;
	
	//@Scheduled(cron = "${downlinkResendCron}")
	public void handle() throws IOException {
		long before = System.currentTimeMillis();
		
		Date currentTime = new Date();
		int onlineCheckJudgeMinutes 	= this.getProperty(Constants.ONLINE_CHECK_JUDGE_MINUTES, 5);
		Date noLateThanTime 			= new Date(currentTime.getTime() - onlineCheckJudgeMinutes*60*1000);
		List<DeviceInfo> devices = deviceInfoService.queryDeviceInfoNotLaterThan(noLateThanTime);
		ShowFlakeIdGanerator snowFlake = new ShowFlakeIdGanerator(2, 3);
		
		if(devices != null && devices.size() > 0) {
			for(int i = 0; i < devices.size(); i++) {
				ServiceRequest message = new ServiceRequest();
				message.setDeviceId(devices.get(i).getDeviceId());
				//message.setDeviceId("10200100079");
				message.setEventCreationTime(new Date());
				message.setEventId(snowFlake.nextRandId(4));
				message.setNodeId("12345678900");
				message.setServiceDataEncoding(1);
				message.setServiceDataProtocolVersion(1);
				message.setServiceEncryption(0);
				message.setServiceType(0);
				message.setServiceVersion((short)1);
		
				DownlinkTransportMessage transportMessage = new DownlinkTransportMessage();
				transportMessage.setEventId(message.getEventId());
		
				transportMessage.setMessageId(snowFlake.nextRandId(4));
				transportMessage.setMessageCreationTime(new Date());
				transportMessage.setServiceData(message.getServiceData());
				transportMessage.setServiceDataEncoding(message.getServiceDataEncoding());
				transportMessage.setServiceDataProtocolVersion(message.getServiceDataProtocolVersion());
				transportMessage.setServiceDataEncryption(message.getServiceEncryption());
				transportMessage.setServiceType(message.getServiceType());
				transportMessage.setServiceVersion(message.getServiceVersion());
				transportMessage.setDeviceId(message.getDeviceId());
				
				deviceInfoService.queryDeviceInfo(message.getDeviceId());
				
				ServiceResponse response = transportDownlinkProcessor.process(transportMessage);
				logger.info("Success to process the downlink message from " + message.getDeviceId() + " for service type " + message.getServiceType() + " with event id " + message.getEventId());;
			}
		}
		long after = System.currentTimeMillis();
		return;
	}
	
	private int getProperty(String key, int defaultValue){
		String value = environment.getProperty(key);
		if (value == null)
			return defaultValue;
		try {
			return Integer.parseInt(value);
		} catch(Exception err) {}
		
		return defaultValue;
	}
}
