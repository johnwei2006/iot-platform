package com.proxy.codec;

import com.proxy.app.Constants;
import com.proxy.codec.v3.TransportcoderByteImplV3;
import com.proxy.model.MessageHeader;

/**
 * Factory for creating {@link TransportCoder}.
 *
 * 传输版本 2（TRANSPORT_VERSION_2）起，传输层定长段扩展为 51 字节并携带 10 字节网关认证 token，
 * 由 {@link TransportcoderByteImplV3} 编解码；版本 1（旧固件，无 token）自协议升级起不再被接受。
 */
public class MessageCoderFactory {

	public static TransportCoder createMessageCoder(MessageHeader header) {
		if (header != null && header.getTransportVersion() == Constants.TRANSPORT_VERSION_2) {
			return new TransportcoderByteImplV3();
		}
		throw new IllegalArgumentException(
				"Unsupported transport version: " + (header == null ? "null" : header.getTransportVersion())
						+ ", only transport version 2 (with gateway auth token) is accepted");
	}
}
