package com.proxy.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proxy.Constants.Status;
import com.proxy.persistence.h2.DownlinkMessageRespository;
import com.proxy.persistence.h2.model.DownlinkMessage;
import com.proxy.utils.HexConvert;

@org.springframework.context.annotation.Profile("h2")
@Service
public class DownlinkMessageServiceH2 implements DownlinkMessageService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	DownlinkMessageRespository downlinkMessageRespository;
	
	public List<com.proxy.persistence.model.DownlinkMessage> findAllDownlinkMessages(){
		Iterator<DownlinkMessage> downlinkMessages = downlinkMessageRespository.findAll().iterator();
		if (downlinkMessages != null) {
			if (!downlinkMessages.hasNext())
				return null;
			
			List<com.proxy.persistence.model.DownlinkMessage> results = new ArrayList<com.proxy.persistence.model.DownlinkMessage>();
			while (downlinkMessages.hasNext()) {
				DownlinkMessage h2Message = downlinkMessages.next();
				
				results.add(h2Message.toDownlinkMessage());
			}
			return results;
		} else {
			return null;
		}
	}

	public List<com.proxy.persistence.model.DownlinkMessage> findAllDownlinkMessagesExceptStatus(Status p_status){
		try { 
		List<DownlinkMessage> downlinkMessages = downlinkMessageRespository.queryAllDownlinkMessagesExceptStatus(p_status);
		
		int size = downlinkMessages == null ? 0 : downlinkMessages.size();
		List<com.proxy.persistence.model.DownlinkMessage> results = new ArrayList<com.proxy.persistence.model.DownlinkMessage>();
		for(int i = 0; i < size; i++) {
			DownlinkMessage h2Message = downlinkMessages.get(i);
			results.add(h2Message.toDownlinkMessage());
		}
		return results;
		} catch(Exception err) {
			err.printStackTrace();
			return null;
		}
	}

	public List<com.proxy.persistence.model.DownlinkMessage> findAllDownlinkMessagesExceptStatus(Status p_status, Date since){
		try { 
		List<DownlinkMessage> downlinkMessages = downlinkMessageRespository.queryAllDownlinkMessagesExceptStatusSince(p_status, since);
		
		int size = downlinkMessages == null ? 0 : downlinkMessages.size();
		List<com.proxy.persistence.model.DownlinkMessage> results = new ArrayList<com.proxy.persistence.model.DownlinkMessage>();
		for(int i = 0; i < size; i++) {
			results.add(downlinkMessages.get(i).toDownlinkMessage());
		}
		return results;
		} catch(Exception err) {
			err.printStackTrace();
			return null;
		}
	}

	public int failExpiredDownlinkMessages(Date expireBefore){
		return downlinkMessageRespository.updateExpiredDownlinkMessagesFailed(Status.FAILED, expireBefore);
	}
	
	public List<com.proxy.persistence.model.DownlinkMessage> findVehicleDownlinkMessagesExceptStatus(String vin, Status p_status){
		List<DownlinkMessage> downlinkMessages = downlinkMessageRespository.queryDeviceDownlinkMessagesExceptStatus(vin, p_status);
		
		int size = downlinkMessages == null ? 0 : downlinkMessages.size();
		List<com.proxy.persistence.model.DownlinkMessage> results = new ArrayList<com.proxy.persistence.model.DownlinkMessage>();
		for(int i = 0; i < size; i++) {
			DownlinkMessage h2Message = downlinkMessages.get(i);
			results.add(h2Message.toDownlinkMessage());
		}
		return results;
	}

	public List<com.proxy.persistence.model.DownlinkMessage> findAllDownlinkMessagesWithStatus(Status p_status){
		List<DownlinkMessage> downlinkMessages = downlinkMessageRespository.queryAllDownlinkMessagesWithStatus(p_status);
		
		int size = downlinkMessages == null ? 0 : downlinkMessages.size();
		List<com.proxy.persistence.model.DownlinkMessage> results = new ArrayList<com.proxy.persistence.model.DownlinkMessage>();
		for(int i = 0; i < size; i++) {
			DownlinkMessage h2Message = downlinkMessages.get(i);
			results.add(h2Message.toDownlinkMessage());
		}
		return results;
	}

	public com.proxy.persistence.model.DownlinkMessage getDownlinkMessageByMessageId(String vehicleIdentifier, long messageId){
		DownlinkMessage msg = downlinkMessageRespository.queryDownlinkMessageByMessageId(vehicleIdentifier, messageId);
		return msg == null ? null : msg.toDownlinkMessage();
	}
	
	@Transactional
	public com.proxy.persistence.model.DownlinkMessage updateDownlinkMessage(com.proxy.persistence.model.DownlinkMessage downlinkSendStatus) {
		synchronized(this) {
			DownlinkMessage status  = downlinkMessageRespository.queryDownlinkMessageByMessageId(downlinkSendStatus.getDeviceId(), downlinkSendStatus.getMessageId());
			if (status == null) {
				status = DownlinkMessage.fromDownlinkMessage(downlinkSendStatus);
				status = downlinkMessageRespository.save(status);
				return status.toDownlinkMessage();
			} else {
				status.setDeviceId(downlinkSendStatus.getDeviceId());
				status.setEventId(downlinkSendStatus.getEventId());
				status.setServiceType(downlinkSendStatus.getServiceType());
				status.setMessageBytes(downlinkSendStatus.getMessageBytes());
				status.setMessageId(downlinkSendStatus.getMessageId());
				status.setStatus(downlinkSendStatus.getStatus());
				status.setResendCount(downlinkSendStatus.getResendCount());
				status.setIp(downlinkSendStatus.getIp());
				status.setPort(downlinkSendStatus.getPort());
				//status.setMessageTime(downlinkSendStatus.getMessageTime());
				status.setSendTime(downlinkSendStatus.getSendTime());
				status = downlinkMessageRespository.save(status);
				return status.toDownlinkMessage();
			}
		}
	}
	
	@Transactional
	public int  deleteDownlinkMessageByMessageId(String vehicleIdentifier, long messageId) {
		return downlinkMessageRespository.deleteDownlinkMessageByMessageId(vehicleIdentifier, messageId);
	}
	
	@Transactional
	public int deleteAllAckDownlinkMessage() {
		return downlinkMessageRespository.deleteAllAckDownlinkMessage(Status.ACKNOWLEDGED);
	}
	
	public List<com.proxy.persistence.model.DownlinkMessage> queryAllNotAckRecords() {
		//return downlinkMessageRespository.queryAllNotAckRecords(Status.SENT);
		List<DownlinkMessage> msgs =  downlinkMessageRespository.queryAllNotAckRecords(Status.SENT);
		if (msgs == null || msgs.size() == 0)
			return null;
		List<com.proxy.persistence.model.DownlinkMessage> results = new ArrayList<com.proxy.persistence.model.DownlinkMessage>();
		for(int i = 0; i < msgs.size(); i++) {
			results.add(msgs.get(i).toDownlinkMessage());
		}
		return results;
	}
	
	public List<com.proxy.persistence.model.DownlinkMessage> queryAllDownlinkMessages() {
		//return downlinkMessageRespository.queryAllDownlinkMessages();
		List<DownlinkMessage> msgs =  downlinkMessageRespository.queryAllDownlinkMessages();
		if (msgs == null || msgs.size() == 0)
			return null;
		List<com.proxy.persistence.model.DownlinkMessage> results = new ArrayList<com.proxy.persistence.model.DownlinkMessage>();
		for(int i = 0; i < msgs.size(); i++) {
			results.add(msgs.get(i).toDownlinkMessage());
		}
		return results;
	}
	
	@Transactional
	public int deleteOverResendMessages(int resendCount){
		synchronized(this) {
			List<DownlinkMessage> overResendRecords = downlinkMessageRespository.queryOverResendMessages(resendCount);
			
			int deletedCount = downlinkMessageRespository.deleteOverResendMessages(resendCount);
			
			int len = (overResendRecords == null ? 0 : overResendRecords.size());
			for (int i = 0; i < len; i ++) {
//				failedDownlinkLogger.error("Error: downlink message will be ignored due to exceed the resend times, message information: ");
//				failedDownlinkLogger.error("vin:"+  overResendRecords.get(i).getVehicleIdentifier());
//				failedDownlinkLogger.error("messageId :  " + overResendRecords.get(i).getMessageId());
//				failedDownlinkLogger.error("service type :  " + overResendRecords.get(i).getServiceType());
//				failedDownlinkLogger.error("eventId :  " + overResendRecords.get(i).getEventId());
//				failedDownlinkLogger.error("data :  " + HexConvert.toHexString(overResendRecords.get(i).getMessageBytes()));

				logger.error("Error: downlink message will be ignored due to exceed the resend times, message information: ");
				logger.error("device:"+  overResendRecords.get(i).getDeviceId());
				logger.error("messageId :  " + overResendRecords.get(i).getMessageId());
				logger.error("service type :  " + overResendRecords.get(i).getServiceType());
				logger.error("eventId :  " + overResendRecords.get(i).getEventId());
				logger.error("data :  " + HexConvert.toHexString(overResendRecords.get(i).getMessageBytes()));
			}
			return deletedCount;
		}
	}
}
