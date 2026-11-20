package com.proxy.codec;

import org.springframework.stereotype.Service;

import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.MessageHeader;
import com.proxy.model.UplinkTransportMessage;

@Service
public class MessageCodeHandler {

	public UplinkTransportMessage decodePartialUplinkRawMessage(MessageHeader header, byte[] transportMessageBytes) throws Exception{
		TransportCoder messageCoder;
		try {
			messageCoder = MessageCoderFactory.createMessageCoder(header);
			return messageCoder.decode(transportMessageBytes);
		} catch (final DecodeException e) {
			throw e;
		}
	}
	
	public UplinkTransportMessage decodeUplinkRawMessage(MessageHeader header, byte[] transportMessageBytes) throws Exception{
		TransportCoder messageCoder;
		try {
			messageCoder = MessageCoderFactory.createMessageCoder(header);
			UplinkTransportMessage result = messageCoder.decode(transportMessageBytes);
			result.setHeader(header);
			return result;
		} catch (final DecodeException e) {
			throw e;
		}
	}
	
	public byte[] encodeDownlinkMessage(DownlinkTransportMessage downlinkTransportMessage) throws Exception{
		TransportCoder messageCoder;
		try {
			messageCoder = MessageCoderFactory.createMessageCoder(downlinkTransportMessage.getHeader());
			return messageCoder.encode(downlinkTransportMessage);
		}  catch (final EncodeException e) {
			throw e;
		}
	}
}
