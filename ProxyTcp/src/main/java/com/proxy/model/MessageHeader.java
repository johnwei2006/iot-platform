package com.proxy.model;

public class MessageHeader {
	int MESSAGE_HEADER_LENGTH 		= 4;
	int TRANSPORT_LENGTH_SIZE 		= 2;
	int TRANSPORT_CRC_SIZE 			= 1;

	int TRANSPORT_ENCODING_INDEX 	= 0;
	int TRANSPORT_VERSION_INDEX 	= 1;
	int TRANSPORT_LENGTH_INDEX 		= 2;

	byte TransportDataEncoding;
	byte TransportVersion;
	short TransportDataLength;

	public int getTRANSPORT_ENCODING_INDEX() {
		return TRANSPORT_ENCODING_INDEX;
	}

	public void setTRANSPORT_ENCODING_INDEX(int tRANSPORT_ENCODING_INDEX) {
		TRANSPORT_ENCODING_INDEX = tRANSPORT_ENCODING_INDEX;
	}

	public int getTRANSPORT_VERSION_INDEX() {
		return TRANSPORT_VERSION_INDEX;
	}

	public void setTRANSPORT_VERSION_INDEX(int tRANSPORT_VERSION_INDEX) {
		TRANSPORT_VERSION_INDEX = tRANSPORT_VERSION_INDEX;
	}

	public int getTRANSPORT_LENGTH_INDEX() {
		return TRANSPORT_LENGTH_INDEX;
	}

	public void setTRANSPORT_LENGTH_INDEX(int tRANSPORT_LENGTH_INDEX) {
		TRANSPORT_LENGTH_INDEX = tRANSPORT_LENGTH_INDEX;
	}

	public int getMESSAGE_HEADER_LENGTH() {
		return MESSAGE_HEADER_LENGTH;
	}

	public void setMESSAGE_HEADER_LENGTH(int mESSAGE_HEADER_LENGTH) {
		MESSAGE_HEADER_LENGTH = mESSAGE_HEADER_LENGTH;
	}

	public int getTRANSPORT_LENGTH_SIZE() {
		return TRANSPORT_LENGTH_SIZE;
	}

	public void setTRANSPORT_LENGTH_SIZE(int tRANSPORT_LENGTH_SIZE) {
		TRANSPORT_LENGTH_SIZE = tRANSPORT_LENGTH_SIZE;
	}

	public int getTRANSPORT_CRC_SIZE() {
		return TRANSPORT_CRC_SIZE;
	}

	public void setTRANSPORT_CRC_SIZE(int tRANSPORT_CRC_SIZE) {
		TRANSPORT_CRC_SIZE = tRANSPORT_CRC_SIZE;
	}

	public byte getTransportVersion() {
		return TransportVersion;
	}

	public void setTransportVersion(byte transportVersion) {
		TransportVersion = transportVersion;
	}

	public short getTransportDataLength() {
		return TransportDataLength;
	}

	public void setTransportDataLength(short transportDataLength) {
		TransportDataLength = transportDataLength;
	}

	public byte getTransportDataEncoding() {
		return TransportDataEncoding;
	}

	public void setTransportDataEncoding(byte transportDataEncoding) {
		TransportDataEncoding = transportDataEncoding;
	}
}
