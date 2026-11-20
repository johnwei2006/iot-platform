package com.proxy.handler;

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proxy.model.Message;

@Service
public class DownlinkProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private LinkedBlockingQueue<Message> sendQueue; 
    
	public void send(Message downlinkMessage) throws Exception {
		//InetSocketAddress targetAddr = new InetSocketAddress(downlinkMessage.getIp(), downlinkMessage.getPort());
		try {
			//udpConnector.sendData(downlinkMessage.getPayload(), targetAddr, null);
			sendQueue.offer(downlinkMessage);
		} catch (Exception err) {
			//logger.error("Failed to send data to " + downlinkMessage.getIp() + ":" + downlinkMessage.getPort());
			throw err;
		}
	}
}
