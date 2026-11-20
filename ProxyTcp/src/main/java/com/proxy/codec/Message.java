package com.proxy.codec;

public class Message {
	private byte[] messageHeader;
	private byte[] transportData;
	private byte[] serviceData;
	private byte[] messageSignature;

	public byte[] getMessageHeader() {
		return messageHeader;
	}

	public void setMessageHeader(byte[] messageHeader) {
		this.messageHeader = messageHeader;
	}

	public byte[] getTransportData() {
		return transportData;
	}

	public void setTransportData(byte[] transportData) {
		this.transportData = transportData;
	}

	public byte[] getServiceData() {
		return serviceData;
	}

	public void setServiceData(byte[] serviceData) {
		this.serviceData = serviceData;
	}

	public byte[] getMessageSignature() {
		return messageSignature;
	}

	public void setMessageSignature(byte[] messageSignature) {
		this.messageSignature = messageSignature;
	}

	public byte[] toByteArray() {
		return mergeByteArrays(toByteArrays());
	}

	private byte[][] toByteArrays() {
		return new byte[][] { messageHeader, transportData, serviceData, messageSignature };
	}

	public static byte[] mergeByteArrays(byte[][] arrays) {
		int length = 0;
		if (arrays != null) {
			for (int i = 0; i < arrays.length; i++) {
				if (arrays[i] != null) {
					length += arrays[i].length;
				}
			}
		}

		byte[] data = new byte[length];
		length = 0;
		if (arrays != null) {
			for (int i = 0; i < arrays.length; i++) {
				if (arrays[i] != null) {
					System.arraycopy(arrays[i], 0, data, length, arrays[i].length);
					length += arrays[i].length;
				}
			}
		}

		return data;
	}

}
