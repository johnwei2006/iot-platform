package com.proxy;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.proxy.Constants.Status;
import com.proxy.persistence.DownlinkMessageService;
import com.proxy.persistence.ProxyLockServiceMongo;
import com.proxy.persistence.model.DownlinkMessage;
import com.proxy.persistence.mongodb.model.ProxyLock;


@Configuration
@EnableScheduling
public class ScheduledDeleteObsoleteFailedMessageTask {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	DownlinkMessageService downlinMessageService;
	
	@Autowired(required = false)
	ProxyLockServiceMongo proxyLockServiceMongo;

	@Autowired
	Environment environment;

	@Scheduled(cron = "${deleteObsoleteFailedMessageCron}")
	private void configureTasks() {
		deleteObsoleteFailedMessage();
	}
	
	private void deleteObsoleteFailedMessage(){
		String dbType = environment.getProperty(Constants.DB_TYPE);
		boolean isMongoDB = dbType != null && dbType.trim().equalsIgnoreCase(Constants.DB_TYPE_MONGODB);
		ProxyLock lock = new ProxyLock();
		boolean isDeleteObsoleteFailedMessageLocked = false;
		Date currentTime = new Date();
		try {
			if (isMongoDB)
			{
				lock.setLockedTime(new Date());
				lock.setLockType(Constants.LOCK_TYPE_DELETE_OBSOLETE_FAILED_MESSAGE);
				lock.setLocker(Thread.currentThread().getName());
				isDeleteObsoleteFailedMessageLocked = proxyLockServiceMongo.lock(lock);
			}
			if (isMongoDB && isDeleteObsoleteFailedMessageLocked) {
				logger.info("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
				logger.info("Start to delete obsolete failed downlink message");

				List<DownlinkMessage> cachedFailedDownlinkMessageList = downlinMessageService.findAllDownlinkMessagesWithStatus(Status.FAILED);
				if (cachedFailedDownlinkMessageList == null) {
					logger.info("	No failed downlink messages");
					return;
				}				
		
				logger.info("	Failed downlink message size : " + cachedFailedDownlinkMessageList.size());
				Date obsoleteFailedMessageTime;
				int deletedNumber = 0;
				for (int i = 0; i < cachedFailedDownlinkMessageList.size(); i++)
				{
					obsoleteFailedMessageTime = new Date(cachedFailedDownlinkMessageList.get(i).getMessageTime().getTime() + 24*60*60*1000);
					if(obsoleteFailedMessageTime.before(currentTime)) {
						downlinMessageService.deleteDownlinkMessageByMessageId(cachedFailedDownlinkMessageList.get(i).getDeviceId(), cachedFailedDownlinkMessageList.get(i).getMessageId());
						deletedNumber++;
						logger.info("	Delete obsolete failed downlink message^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
						logger.info("	Message id           : " + cachedFailedDownlinkMessageList.get(i).getMessageId());
						logger.info("	Message Service Type : " + cachedFailedDownlinkMessageList.get(i).getServiceType());
						logger.info("	Vehicle No           : " + cachedFailedDownlinkMessageList.get(i).getDeviceId());
						logger.info("	Message time         : " + cachedFailedDownlinkMessageList.get(i).getMessageTime());
					}
				}
				logger.info("	Deleted obsolete failed downlink message size : " + deletedNumber);
			}
		} catch(Exception err) {
			logger.error("	Error during deleting obsolete failure messages:" + err.getMessage());
		} finally {
			if (isMongoDB && isDeleteObsoleteFailedMessageLocked) {
				proxyLockServiceMongo.unLock(lock);
			}
		}
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
