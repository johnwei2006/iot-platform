package com.proxy.model;

import java.net.URI;

import org.joda.time.DateTime;

public class UplinkRawMessage {
	  private URI sourceURI;
	  private byte[] payload;
	  private String connectType;
	  private DateTime receivedTime;
//	  private VehicleIdentifier vehicleIdentifier;
//	  private Integer uplinkMessageId;
//	  private Integer downlinkMessageId;
//	  private boolean isCreateEventRequest;
//	  private boolean isPingRequest;
//	  private String eventId;
//	  private Object payloadDTO;
//	  private int serviceType;
//	  private String externalTrackingId;

	  public URI getSourceURI() {
	    return sourceURI;
	  }

	  public byte[] getPayload() {
	    return payload;
	  }

	public String getConnectType() {
		return connectType;
	}

	public void setConnectType(String connectType) {
		this.connectType = connectType;
	}

	public DateTime getReceivedTime() {
		return receivedTime;
	}

	public void setReceivedTime(DateTime receivedTime) {
		this.receivedTime = receivedTime;
	}

	public void setSourceURI(URI sourceURI) {
		this.sourceURI = sourceURI;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
}
