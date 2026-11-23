package com.proxy.utils;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.proxy.app.Constants;
import com.proxy.model.CommonTransportMessage;

@Service("timeToLiveHandler")
public class TimeToLiveHandler{

	static final long CODEC_TO_MODEL_TTL_SCALE_FACTOR = 10000L;
	static final String CONFIG_UNBOUNDED_MESSAGE_TIME_TO_LIVE = "uplink.ttl.default";
	static final long DEFAULT_TIME_TO_LIVE_IF_NOT_CONFIGURED_IN_MS = TimeUnit.HOURS.toMillis(24);
	static final long CACHE_TIME_TO_LIVE_IN_MS = TimeUnit.MINUTES.toMillis(10);

	private static final long DEFAULT_DOWNLINK_MESSAGE_TIME_TO_LIVE = 0;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
    @Autowired 
    Environment environment; 
    
	public boolean hasExpired(final CommonTransportMessage message) {
		final long messageCreationTime = getMessageCreationTime(message);
		final long messageTimeToLiveInMs = getMessageTimeToLive(message);

		if (messageTimeToLiveInMs == 0) {
			// Infinity. This value is from server-side configuration, not from the vehicle.
			return false;
		} else {
			final long messageExpireTime = messageCreationTime + messageTimeToLiveInMs;
			final boolean hasExpired = System.currentTimeMillis() > messageExpireTime;
			if (hasExpired) {
//				logWarn(String.format(Locale.ENGLISH, "Rejecting message from %s. Message expired %s (TTL: %s)",
//						message.getVehicleIdentifier(), new Date(messageExpireTime),
//						messageTimeToLiveInMs));
			}
			return hasExpired;
		}
	}

//	public boolean hasExpired(final DownlinkMessage downlinkMessage) {
//		final boolean hasExpired = System.currentTimeMillis() > downlinkMessage.getMessageExpireTime();
//		if (hasExpired) {
//			logWarn(String.format(Locale.ENGLISH, "Rejecting message from %s. Message expired %s (TTL: %s)",
//					downlinkMessage.getVehicleIdentifier().getId(), new Date(downlinkMessage.getMessageExpireTime()),
//					downlinkMessage.getTimeToLive()));
//		}
//		return hasExpired;
//	}

	/**
	 * Determines the time to live value to use when creating a
	 * If1ConnectorByteArrayDownlinkMessage.
	 *
	 * @param ttl The TTL value specified from the SH.
	 * @return TTL to use for a downlink message of the specified service type.
	 */
	public long determineTimeToLive(final long ttl, final int serviceType) {
		long ttlValueToUse = ttl;

		// Valid value already specified?
		if (ttl <= 0) {
			// No. Fetch value from configuration file.
			//final DispatcherContext dispatcherContext = new DispatcherContext(null, serviceType, null, null, null);
			ttlValueToUse = Long.parseLong(environment.getProperty("downlink.ttl.default"));
//					DEFAULT_DOWNLINK_MESSAGE_TIME_TO_LIVE);

			// Value fetched from configuration file OK?
			// Note that it is OK to specify 0 in the configuration file to mean "infinity"
			// => we use <
			// instead of
			// <= here.
			if (ttlValueToUse < 0) {
				ttlValueToUse = DEFAULT_DOWNLINK_MESSAGE_TIME_TO_LIVE;
			}
		}

		return ttlValueToUse;
	}

	/** ************************ Private methods ************************ */
	private long getMessageCreationTime(final CommonTransportMessage dsptMessage) {
		return dsptMessage.getMessageCreationTime().getTime();
	}

	private long getMessageTimeToLive(final CommonTransportMessage dsptMessage) {
		long messageTimeToLiveInMs = Constants.DEFAULT_TTL * CODEC_TO_MODEL_TTL_SCALE_FACTOR;
		if (messageTimeToLiveInMs <= 0) {
//			messageTimeToLiveInMs = configuration.getLongWithCaching(CONFIG_UNBOUNDED_MESSAGE_TIME_TO_LIVE,
//					DEFAULT_TIME_TO_LIVE_IF_NOT_CONFIGURED_IN_MS, CACHE_TIME_TO_LIVE_IN_MS);
			logger.debug(String.format(Locale.ENGLISH,
					"Message time to live was unbounded, setting to configured value: %s", messageTimeToLiveInMs));
		}
		return messageTimeToLiveInMs;
	}

	private void logWarn(String message) {
		logger.warn(message);
	}
}
