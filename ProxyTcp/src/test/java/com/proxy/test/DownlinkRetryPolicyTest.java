package com.proxy.test;

import java.util.Date;

import org.junit.Test;

import com.proxy.DownlinkRetryPolicy;
import com.proxy.DownlinkRetryPolicy.Decision;
import com.proxy.persistence.model.DownlinkMessage;

import static org.junit.Assert.assertEquals;

/**
 * 下行重发决策纯逻辑单测（无 Mongo / 无 Channel 依赖）。
 */
public class DownlinkRetryPolicyTest {

	private static final int MAX_RESEND = 3;
	private static final int JUDGE_FAILED_MIN = 10;   // judgeSendFailedAferMinutes
	private static final int INTERVAL_SEC = 60;        // resendIntervalSeconds

	private DownlinkMessage msg(long ageMillis, int resendCount, Long sinceLastSendMillis) {
		DownlinkMessage m = new DownlinkMessage();
		Date now = new Date();
		m.setMessageTime(new Date(now.getTime() - ageMillis));
		m.setResendCount(resendCount);
		if (sinceLastSendMillis != null) {
			m.setLastSendTime(new Date(now.getTime() - sinceLastSendMillis));
		}
		return m;
	}

	@Test
	public void expiredOnlineStillFails() {
		// 在线但超过时间上限 → 判失败（修复离线/在线统一收敛）
		DownlinkMessage m = msg((JUDGE_FAILED_MIN + 1) * 60_000L, 0, null);
		assertEquals(Decision.FAIL_EXPIRED,
				DownlinkRetryPolicy.decide(m, new Date(), true, MAX_RESEND, JUDGE_FAILED_MIN, INTERVAL_SEC));
	}

	@Test
	public void expiredOfflineFails() {
		// 离线且超过时间上限 → 同样判失败（消除离线永久堆积）
		DownlinkMessage m = msg((JUDGE_FAILED_MIN + 1) * 60_000L, 0, null);
		assertEquals(Decision.FAIL_EXPIRED,
				DownlinkRetryPolicy.decide(m, new Date(), false, MAX_RESEND, JUDGE_FAILED_MIN, INTERVAL_SEC));
	}

	@Test
	public void countCapFails() {
		// 达到最大重发次数 → 判失败（替代原 >100 死分支）
		DownlinkMessage m = msg(1_000L, MAX_RESEND, null);
		assertEquals(Decision.FAIL_EXPIRED,
				DownlinkRetryPolicy.decide(m, new Date(), true, MAX_RESEND, JUDGE_FAILED_MIN, INTERVAL_SEC));
	}

	@Test
	public void offlineNotExpiredSkips() {
		// 离线且未过期、未达上限 → 跳过（待上线或超时清理）
		DownlinkMessage m = msg(1_000L, 0, null);
		assertEquals(Decision.SKIP,
				DownlinkRetryPolicy.decide(m, new Date(), false, MAX_RESEND, JUDGE_FAILED_MIN, INTERVAL_SEC));
	}

	@Test
	public void onlineNotExpiredNoLastSendResends() {
		// 在线、未过期、未达上限、无 lastSendTime → 重发
		DownlinkMessage m = msg(1_000L, 0, null);
		assertEquals(Decision.RESEND,
				DownlinkRetryPolicy.decide(m, new Date(), true, MAX_RESEND, JUDGE_FAILED_MIN, INTERVAL_SEC));
	}

	@Test
	public void onlineWithinIntervalSkips() {
		// 在线、未过期、lastSendTime 距现在小于最小间隔 → 跳过（防大包洪泛）
		DownlinkMessage m = msg(1_000L, 0, (long) (INTERVAL_SEC - 10) * 1000L);
		assertEquals(Decision.SKIP,
				DownlinkRetryPolicy.decide(m, new Date(), true, MAX_RESEND, JUDGE_FAILED_MIN, INTERVAL_SEC));
	}

	@Test
	public void onlinePastIntervalResends() {
		// 在线、未过期、lastSendTime 距现在大于最小间隔 → 重发
		DownlinkMessage m = msg(1_000L, 0, (long) (INTERVAL_SEC + 10) * 1000L);
		assertEquals(Decision.RESEND,
				DownlinkRetryPolicy.decide(m, new Date(), true, MAX_RESEND, JUDGE_FAILED_MIN, INTERVAL_SEC));
	}

	@Test
	public void nullMessageSkips() {
		assertEquals(Decision.SKIP,
				DownlinkRetryPolicy.decide(null, new Date(), true, MAX_RESEND, JUDGE_FAILED_MIN, INTERVAL_SEC));
	}
}
