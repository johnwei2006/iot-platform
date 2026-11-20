package com.proxy.codec;

//import com.proxy.model.DeviceInfo;

public class CodecContext {

	  public static final int DECODE_MODE = 1;
	  public static final int ENCODE_MODE = 2;

	  private int mode;
	  private long serviceType;
	  //private DeviceInfo deviceInfo;
	  private Message message;

	  public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public int getMode() {
	    return mode;
	  }

	  public void setMode(int mode) {
	    this.mode = mode;
	  }

	  public long getServiceType() {
	    return serviceType;
	  }

	  public void setServiceType(long serviceType) {
	    this.serviceType = serviceType;
	  }

//	  public VehicleIdentifier getVehicleIdentifier() {
//	    return vehicleIdentifier;
//	  }
//
//	  public void setVehicleIdentifier(VehicleIdentifier vehicleIdentifier) {
//	    this.vehicleIdentifier = vehicleIdentifier;
//	  }
}
