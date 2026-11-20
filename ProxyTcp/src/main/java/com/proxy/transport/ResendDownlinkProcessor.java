package com.proxy.transport;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.proxy.Constants;
import com.proxy.Constants.Status;
import com.proxy.handler.DownlinkProcessor;
import com.proxy.model.Message;
import com.proxy.persistence.DownlinkMessageService;
import com.proxy.persistence.SendFailureMessageService;
import com.proxy.persistence.model.DownlinkMessage;
import com.proxy.persistence.model.SendFailureMessage;

@Service
public class ResendDownlinkProcessor {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	Environment environment;

	@Value("${maxResendTimes:8}")
	private int maxResendTimes;

	@Autowired
	DownlinkProcessor downlinkProcessor;
	
	@Autowired
	DownlinkMessageService downlinkMessageService;
	
	@Autowired
	SendFailureMessageService sendFailureMessageService;

	public void resend(DownlinkMessage downlinkSendStatus) {
		try {
			sendDownlink(downlinkSendStatus);
		} catch (Exception e) {
			logger.error("Error during resend downlink message for device " + downlinkSendStatus.getDeviceId()
					+ ", messageId " + downlinkSendStatus.getMessageId(), e);
		}
	}

	private void sendDownlink(DownlinkMessage downlinkMessage) {
		try {
			Message message = new Message();
			// 本网关为 TCP（Netty 按连接下发），连接类型由连接层决定，无需设置 UDP
			message.setMessageTime(downlinkMessage.getMessageTime());
			message.setIp(downlinkMessage.getIp());
			message.setPort(downlinkMessage.getPort());
			message.setPayload(downlinkMessage.getMessageBytes());
			message.setDeviceId(downlinkMessage.getDeviceId());
			downlinkProcessor.send(message);
			downlinkMessage.setStatus(Status.SENT); // SENT = 已提交发送队列，非已送达
			downlinkMessage.setSendTime(new Date());
			downlinkMessage.setLastSendTime(new Date());
			logger.info("Success to resend the downlink message **************************");
			logger.info("Device       :" + downlinkMessage.getDeviceId());
			logger.info("ip/port      :" + downlinkMessage.getIp() + ":" + downlinkMessage.getPort());
			logger.info("service type :" + downlinkMessage.getServiceType());
			logger.info("message id   :" + downlinkMessage.getMessageId());
			logger.info("event id     :" + downlinkMessage.getEventId());
			logger.info("*****************************************************************");
		} catch (final Throwable e) {
			downlinkMessage.setStatus(Status.FAILED);
			logger.error("Failed to resend message to device " + downlinkMessage.getDeviceId()
					+ ", message id " + downlinkMessage.getMessageId(), e);
		} finally {
			int newCount = downlinkMessage.getResendCount() + 1;
			downlinkMessage.setResendCount(newCount);
			if (newCount >= maxResendTimes) {
				// 达到最大重发次数 → 判失败并通知平台（终止条件，替代原 >100 死分支）
				downlinkMessage.setStatus(Status.FAILED);
				downlinkMessageService.updateDownlinkMessage(downlinkMessage);
				logger.warn("Device " + downlinkMessage.getDeviceId() + " messageId " + downlinkMessage.getMessageId()
						+ " exceeded maxResendTimes=" + maxResendTimes + ", marked FAILED.");
				SendFailureMessage sendFailureMessage = new SendFailureMessage();
				sendFailureMessage.setDeviceId(downlinkMessage.getDeviceId());
				sendFailureMessage.setSourceServiceType(downlinkMessage.getServiceType());
				sendFailureMessage.setServiceType(-1);
				sendFailureMessage.setEventId(downlinkMessage.getEventId());
				sendFailureMessage.setMessageId(downlinkMessage.getMessageId());
				sendFailureMessage.setIp(downlinkMessage.getIp());
				sendFailureMessage.setPort(downlinkMessage.getPort());
				sendFailureMessage.setErrorCode(Constants.SEND_FAILURE_ERROR_CODE_MULTIPLE_SEND_FAILURE);
				sendFailureMessage.setErrorMessage("Downlink message exceeded maxResendTimes=" + maxResendTimes);
				sendFailureMessageService.saveSendFailureMessage(sendFailureMessage);
			} else {
				downlinkMessageService.updateDownlinkMessage(downlinkMessage);
			}
		}
	}
	
	private class DownlinkMessageProcessorTask implements Runnable {
		private final DownlinkMessage downlinkSendStatus;

		private DownlinkMessageProcessorTask(final DownlinkMessage downlinkSendStatus) {
			this.downlinkSendStatus = downlinkSendStatus;
		}

		public void run() {
			sendDownlink(downlinkSendStatus);
		}
	}
}
