package com.proxy.codec;

import com.proxy.Constants;
import com.proxy.codec.v2.TransportcoderByteImplV2;
import com.proxy.model.MessageHeader;

/**
 * Factory for creating {@link TransportCoder}.
 */
public class MessageCoderFactory {

	public static TransportCoder createMessageCoder(MessageHeader header) {
		TransportCoder messageCoder = new TransportcoderByteImplV2();

		/*
		 if (header.getTransportDataEncoding() == Constants.ENCODINGTYPE_BYTE) {
			messageCoder = new TransportcoderByteImplV2();

			switch (header.getTransportVersion()) {
			case Constants.TRANSPORT_VERSION_1:
				//messageCoder = new TransportcoderByteImpl();
				break;
			case Constants.TRANSPORT_VERSION_2:
				messageCoder = new TransportcoderByteImplV2();
				break;
			default:
				throw new IllegalArgumentException("Unsupported transport version: " + header.getTransportVersion());
			}
		} else if (header.getTransportDataEncoding() == Constants.ENCODINGTYPE_JSON) {
			//messageCoder = new TransportcoderJsonImpl();
		} else if (header.getTransportDataEncoding() == Constants.ENCODINGTYPE_PROTOBUF) {
			//messageCoder = TransportCoderProtobufImpl.getInstance();
		}*/
		return messageCoder;
	}
}