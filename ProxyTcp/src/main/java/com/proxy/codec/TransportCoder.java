package com.proxy.codec;

import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.UplinkTransportMessage;

public interface TransportCoder {

	boolean isSupportedVersion(byte[] data);

	UplinkTransportMessage decode(byte[] transportMessage) throws DecodeException;
	
	//UplinkTransportMessage decodePartial(byte[] transportMessage) throws DecodeException;
	
	byte[] encode(DownlinkTransportMessage downlinkTransportMessage) throws EncodeException;
}
