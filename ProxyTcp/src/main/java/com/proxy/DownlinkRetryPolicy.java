package com.proxy;

import java.util.Date;

import com.proxy.persistence.model.DownlinkMessage;

/**
 * 下行重发决策（纯函数，便于无 Mongo/无 Channel 的单测）。
 *
 * <p>决策优先级：
 * 1. 超过时间上限 {@code judgeSendFailedAferMinutes} → {@link Decision#FAIL_EXPIRED}（在线/离线一致收敛，避免离线消息永久堆积）；
 * 2. 已达最大重发次数 {@code maxResendTimes} → {@link Decision#FAIL_EXPIRED}；
 * 3. 设备离线 → {@link Decision#SKIP}（在线后由下次调度补发，或超时判失败）；
 * 4. 距上次发送不足 {@code resendIntervalSeconds} → {@link Decision#SKIP}（最小间隔，避免升级包等大下行在 ACK 前被洪泛）；
 * 5. 其余 → {@link Decision#RESEND}。
 */
public final class DownlinkRetryPolicy {

	public enum Decision {
		RESEND,       // 在线且可重发
		FAIL_EXPIRED, // 超过时间/次数上限，判失败
		SKIP          // 离线或最小间隔未到，本轮跳过
	}

	private DownlinkRetryPolicy() {
	}

	public static Decision decide(DownlinkMessage message, Date now, boolean online,
			int maxResendTimes, int judgeSendFailedAferMinutes, int resendIntervalSeconds) {
		if (message == null || message.getMessageTime() == null) {
			return Decision.SKIP;
		}
		long ageMillis = now.getTime() - message.getMessageTime().getTime();
		// 时间上限：超过 judgeSendFailedAferMinutes 一律判失败（在线/离线一致收敛）
		if (ageMillis > (long) judgeSendFailedAferMinutes * 60_000L) {
			return Decision.FAIL_EXPIRED;
		}
		// 次数上限：已达最大重发次数，判失败
		if (message.getResendCount() >= maxResendTimes) {
			return Decision.FAIL_EXPIRED;
		}
		// 离线：本轮不重发，待上线或超时清理
		if (!online) {
			return Decision.SKIP;
		}
		// 最小重发间隔：避免大下行在 ACK 前被重复洪泛
		if (message.getLastSendTime() != null) {
			long sinceLastSend = now.getTime() - message.getLastSendTime().getTime();
			if (sinceLastSend < (long) resendIntervalSeconds * 1000L) {
				return Decision.SKIP;
			}
		}
		return Decision.RESEND;
	}
}
