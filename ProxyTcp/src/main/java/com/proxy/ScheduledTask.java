package com.proxy;

import java.util.Date;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.proxy.Constants.Status;
import com.proxy.DownlinkRetryPolicy;
import com.proxy.DownlinkRetryPolicy.Decision;
import com.proxy.model.SendFailureRequest;
import com.proxy.persistence.DownlinkMessageService;
import com.proxy.persistence.ProxyLockServiceMongo;
import com.proxy.persistence.SendFailureMessageService;
import com.proxy.persistence.UplinkMessageService;
import com.proxy.persistence.DeviceInfoService;
import com.proxy.persistence.model.DownlinkMessage;
import com.proxy.persistence.model.SendFailureMessage;
import com.proxy.persistence.model.DeviceInfo;
import com.proxy.persistence.mongodb.model.ProxyLock;
import com.proxy.netty.ChannelRegistry;
import com.proxy.transport.ResendDownlinkProcessor;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@Configuration
@EnableScheduling
public class ScheduledTask {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	DownlinkMessageService downlinMessageService;
	
	@Autowired
	UplinkMessageService uplinMessageService;

	@Autowired
	DeviceInfoService deviceInfoService;
	
	@Autowired
	SendFailureMessageService sendFailureMessageService;
	
	@Autowired
	DownlinkMessageService downlinkMessageService;

	@Autowired
	ResendDownlinkProcessor resendDownlinkProcessor;
	
	@Autowired(required = false)
	ProxyLockServiceMongo proxyLockServiceMongo;

	@Autowired
	Environment environment;

	@Autowired
	private BeanRebuildUtil beanRebuildUtil;
	
	@Autowired
	private ConfigurableApplicationContext context;
	
	@Autowired
	private ChannelRegistry channelRegistry;

    // 下行重试决策参数：查询窗口 = 超时 + 缓冲，确保非过期消息不被跳过，同时限制扫描范围
    private static final int RESEND_QUERY_BUFFER_MINUTES = 60;

	@Scheduled(cron = "${downlinkResendCron}")
	private void resendMessage2Device() {
		long before = System.currentTimeMillis();
		try {
			int duplicateMessageDurationMinutes = this.getProperty(Constants.DUPLICATE_MESSAGE_DURATION_MINUTES, 2);
			Date noLateThanTime = new Date(new Date().getTime() - duplicateMessageDurationMinutes*60*1000);
			String dbType = environment.getProperty(Constants.DB_TYPE);
			boolean isUplinkLocked = true;
			boolean isMongoDB = dbType != null && dbType.trim().equalsIgnoreCase(Constants.DB_TYPE_MONGODB);	
			
			ProxyLock lock = new ProxyLock();
			if (isMongoDB)
			{
				lock.setLockedTime(new Date());
				lock.setLockType(Constants.LOCK_TYPE_UPLINK);
				lock.setLocker(Thread.currentThread().getName());
				isUplinkLocked = proxyLockServiceMongo.lock(lock);
			}
			
			//1. delete obsolete uplinkMessages from devices
			if (isUplinkLocked) {
				try {
					int deletedCount = uplinMessageService.deleteUplinkMessageLaterThanTime(noLateThanTime);
					logger.info("Deleted over time uplink message, size: " + deletedCount);
				}catch(Exception e) {
					logger.error("Error during delete overtime cached UplinkMessage", e);
				}
			} else {
				logger.info("Failed to get the uplink lock, this schedule will be ignored!");
			}
			
			if (isMongoDB && isUplinkLocked) {
				proxyLockServiceMongo.unLock(lock);
			}
			
			
			//2. re-send failed downlinkMessages to devices
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			logger.info("Start to resend cached downlink message");
			Date currentTime = new Date();
			int onlineCheckJudgeMinutes 	= this.getProperty(Constants.ONLINE_CHECK_JUDGE_MINUTES, 5);
			int judgeSendFailedAferMinutes 	= this.getProperty(Constants.JUDGE_SEND_FAILED_AFTER_MINUTES, 5); 
			int maxResendTimes 				= this.getProperty(Constants.MAX_RESEND_TIMES, 8);
			int resendIntervalSeconds 		= this.getProperty(Constants.RESEND_INTERVAL_SECONDS, 60);
			noLateThanTime 					= new Date(currentTime.getTime() - onlineCheckJudgeMinutes*60*1000);

			List<DownlinkMessage> cachedDownlinkMessageList = null;
			boolean isResendDownlinkLocked = true;
			if (isMongoDB)
			{
				lock.setLockedTime(new Date());
				lock.setLockType(Constants.LOCK_TYPE_DOWNLINK_RESEND);
				lock.setLocker(Thread.currentThread().getName());
				isResendDownlinkLocked = proxyLockServiceMongo.lock(lock);
			}
			
			if (isResendDownlinkLocked) {
				// 2.0 统一超时判失败：所有非 FAILED 且超过 judgeSendFailedAferMinutes 的下行（在线/离线一致）标记为 FAILED，避免离线消息永久堆积
				Date expireBefore = new Date(currentTime.getTime() - (long)judgeSendFailedAferMinutes*60*1000L);
				int expiredCleaned = downlinkMessageService.failExpiredDownlinkMessages(expireBefore);
				if (expiredCleaned > 0) {
					logger.info("Bulk marked expired downlink messages as FAILED: " + expiredCleaned);
				}

				// 仅扫描近期非 FAILED 下行（限制范围；超窗口的过期消息已由上面批量判失败处理）
				Date since = new Date(currentTime.getTime() - ((long)judgeSendFailedAferMinutes + RESEND_QUERY_BUFFER_MINUTES)*60*1000L);
				cachedDownlinkMessageList = downlinkMessageService.findAllDownlinkMessagesExceptStatus(Status.FAILED, since);
				if (cachedDownlinkMessageList == null) {
					logger.info("No cached downlink message");
					return;
				}

				Set<String> onlineDeviceIds = channelRegistry.getOnlineDeviceIds();
			
				Map devicesMessageSizeMap = new HashMap();
				logger.info("Online vehicle size : " + (onlineDeviceIds == null ? 0 : onlineDeviceIds.size()));
				logger.info("Cached downlink message size : " + cachedDownlinkMessageList.size());

				for (int i = 0; i < cachedDownlinkMessageList.size(); i++)
				{
					DownlinkMessage cachedDownlinkMessage = cachedDownlinkMessageList.get(i);
					boolean online = onlineDeviceIds != null && onlineDeviceIds.contains(cachedDownlinkMessage.getDeviceId());
					Decision decision = DownlinkRetryPolicy.decide(
							cachedDownlinkMessage, currentTime, online,
							maxResendTimes, judgeSendFailedAferMinutes, resendIntervalSeconds);

					if (decision == Decision.SKIP) {
						continue;
					}

					if (decision == Decision.FAIL_EXPIRED) {
						// 达到重发次数上限 → 判失败并通知平台（时间超限由批量判失败处理，此处补齐通知）
						cachedDownlinkMessage.setStatus(Status.FAILED);
						downlinkMessageService.updateDownlinkMessage(cachedDownlinkMessage);
						SendFailureMessage sendFailureMessage = new SendFailureMessage();
						sendFailureMessage.setDeviceId(cachedDownlinkMessage.getDeviceId());
						sendFailureMessage.setSourceServiceType(cachedDownlinkMessage.getServiceType());
						sendFailureMessage.setServiceType(-1);
						sendFailureMessage.setEventId(cachedDownlinkMessage.getEventId());
						sendFailureMessage.setMessageId(cachedDownlinkMessage.getMessageId());
						sendFailureMessage.setIp(cachedDownlinkMessage.getIp());
						sendFailureMessage.setPort(cachedDownlinkMessage.getPort());
						sendFailureMessage.setErrorCode(Constants.SEND_FAILURE_ERROR_CODE_MULTIPLE_SEND_FAILURE);
						sendFailureMessage.setErrorMessage("Downlink message exceeded resend limit");
						sendFailureMessageService.saveSendFailureMessage(sendFailureMessage);
						continue;
					}

					// RESEND：仅对在线设备重发（decide 已保证 online==true），并写入最后发送时间以便最小间隔保护
					int deviceMessageSize = 0;
					Object deviceMessageSizeObj = devicesMessageSizeMap.get(cachedDownlinkMessage.getDeviceId());
					deviceMessageSize = deviceMessageSizeObj == null ? 0 : Integer.parseInt(deviceMessageSizeObj.toString());
					devicesMessageSizeMap.put(cachedDownlinkMessage.getDeviceId(), new Integer(deviceMessageSize + 1));
					if(deviceMessageSize > 3) {
						logger.info("Cached downlink message is too much, rebuild the udp connector..............");
					}
					logger.info("    Start to resend downlink message: " + cachedDownlinkMessage.getMessageId() + " for device " + cachedDownlinkMessage.getDeviceId() + ", resend times " + (cachedDownlinkMessage.getResendCount() + 1));
					try {
						resendDownlinkProcessor.resend(cachedDownlinkMessage);
					} catch(Exception error) {
						logger.error("    Error during resend downlink message: " + cachedDownlinkMessage.getMessageId() + " for device " + cachedDownlinkMessage.getDeviceId() + ", resend times " + (cachedDownlinkMessage.getResendCount() + 1), error);
					}
				}
			} else {
				logger.info("Failed to get the resend downlink lock, this schedule will be ignored!");
			}
			
			if (isMongoDB && isResendDownlinkLocked) {
				proxyLockServiceMongo.unLock(lock);
			}
			
			long after = System.currentTimeMillis();
			logger.info("Spend " + (after - before) + " millisecond to process all of the cached downlink messages, message size = " + (cachedDownlinkMessageList == null ? 0 : cachedDownlinkMessageList.size()));
		} catch(Exception err) {
			logger.error("Error during resend cached downlink messages", err);
		}
	}
	
	private void sendFailureInformation2Platform() {
		String dbType = environment.getProperty(Constants.DB_TYPE);
		boolean isMongoDB = dbType != null && dbType.trim().equalsIgnoreCase(Constants.DB_TYPE_MONGODB);
		ProxyLock lock = new ProxyLock();
		try {
			boolean isSendFailureMessageLocked = true;
			if (isMongoDB)
			{
				lock.setLockedTime(new Date());
				lock.setLockType(Constants.LOCK_TYPE_SEND_FAILURE);
				lock.setLocker(Thread.currentThread().getName());
				isSendFailureMessageLocked = proxyLockServiceMongo.lock(lock);
			}
			if (isMongoDB && isSendFailureMessageLocked) {
				ClientConfig config = new DefaultClientConfig();
				Client client = Client.create(config);
				String sendfailureproxy = environment.getProperty(Constants.SENDFAILUREPROXY);
				WebResource webResource = client.resource(sendfailureproxy);

				List<SendFailureMessage> msgs = sendFailureMessageService.findAllSendFailureMessages();
				int msgSize = (msgs == null) ? 0 : msgs.size();
				
				for(int i = 0; i < msgSize; i++) {
					try {
						SendFailureMessage sendFailureMessage = msgs.get(i);
						
						SendFailureRequest serviceRequest = new SendFailureRequest();
						serviceRequest.setDeviceId(sendFailureMessage.getDeviceId());
						serviceRequest.setMessageId(sendFailureMessage.getMessageId());
						serviceRequest.setEventId(sendFailureMessage.getEventId());
						serviceRequest.setSourceServiceType(sendFailureMessage.getSourceServiceType());
						serviceRequest.setIp(sendFailureMessage.getIp());
						serviceRequest.setPort(sendFailureMessage.getPort());
						serviceRequest.setErrorCode(sendFailureMessage.getErrorCode());
						serviceRequest.setErrorMessage(sendFailureMessage.getErrorMessage());
						
						logger.info("Try to send SendFailureMessage to proxyService with messageId" + sendFailureMessage.getMessageId() + " for vehicle " + sendFailureMessage.getDeviceId());
						ClientResponse response = webResource.type("application/json").post(ClientResponse.class,serviceRequest);
						if(response.getStatus() == ClientResponse.Status.OK.getStatusCode()) {
							sendFailureMessageService.deleteSendFailureMessageByMessageId(sendFailureMessage.getDeviceId(), sendFailureMessage.getMessageId());
						}
					} catch(Exception err) {}
				}
			}
			if (isMongoDB && isSendFailureMessageLocked) {
				proxyLockServiceMongo.unLock(lock);
			}
		} catch(Exception err) {
			logger.error("Error during send failure messages", err);
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
