package com.proxy.handler;

import com.proxy.model.Message;

public interface UplinkProcessor {
	void process(Message uplinkMessage);
}
