package com.proxy.test;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.proxy.model.ServiceRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class DeviceMockWebService {
	private static Logger logger = LoggerFactory.getLogger(DeviceMockWebService.class);

	public static void main(String[] args) {
		try {
			DeviceMockWebService sender = new DeviceMockWebService();
			sender.sendServiceData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendServiceData() {
		String url = "http://39.107.77.230:8010/downlink";
		//String url = "http://globalfinding.com:8010/downlink";
		
		byte[] serviceData 	= new String("PONG").getBytes();
		int eventId 		= 58788;
		int serviceType 	= 4;
		
		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setDeviceId("10200100071");
		//serviceRequest.setNodeId(decodedUplinkTransportMessage.getNodeId());
		serviceRequest.setEventCreationTime(new Date());
		serviceRequest.setEventId(eventId);
		serviceRequest.setServiceDataEncoding(1);
		serviceRequest.setServiceDataProtocolVersion(1);
		serviceRequest.setServiceEncryption(0);
		serviceRequest.setServiceType(serviceType);
		serviceRequest.setServiceVersion((short)1);
		serviceRequest.setServiceData(serviceData);
		
		callWebService(url, serviceRequest);
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
}
