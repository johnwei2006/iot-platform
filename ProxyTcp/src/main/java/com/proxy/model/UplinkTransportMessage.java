package com.proxy.model;

public class UplinkTransportMessage extends CommonTransportMessage{
	private Position position;
	
	public Position getPosition() {
		return position;
	}
	public void setPosition(Position position) {
		this.position = position;
	}
}
