package com.proxy.model;

public class VehicleIdentifierType {
	  public final static VehicleIdentifierType VIN = new VehicleIdentifierType(0, "VIN");
	  public final static VehicleIdentifierType DRIVE_ID = new VehicleIdentifierType(1, "DRIVE_ID");
	  public final static VehicleIdentifierType EQUIPMENT_ID = new VehicleIdentifierType(2, "EQUIPMENT_ID");
	  public final static VehicleIdentifierType EQUIPMENT_SERIAL = new VehicleIdentifierType(3, "EQUIPMENT_SERIAL");
	  public final static VehicleIdentifierType EXTERNAL_DEVICE_ID = new VehicleIdentifierType(4, "EXTERNAL_DEVICE_ID");

	  private final int type;
	  private final String text;

	  private VehicleIdentifierType(final int type, final String text) {
	    this.type = type;
	    this.text = text;
	  }

	  public int getType() {
	    return type;
	  }

	  public static VehicleIdentifierType getFromType(final int type) {
	    VehicleIdentifierType vehicleIdentifierType = null;
	    if (VIN.getType() == type) {
	      vehicleIdentifierType = VIN;
	    } else if (DRIVE_ID.getType() == type) {
	      vehicleIdentifierType = DRIVE_ID;
	    } else if (EQUIPMENT_ID.getType() == type) {
	      vehicleIdentifierType = EQUIPMENT_ID;
	    } else if (EQUIPMENT_SERIAL.getType() == type) {
	      vehicleIdentifierType = EQUIPMENT_SERIAL;
	    } else if (EXTERNAL_DEVICE_ID.getType() == type) {
	      vehicleIdentifierType = EXTERNAL_DEVICE_ID;
	    } else {
	      throw new IllegalArgumentException("Unknown vehicle identifier type: " + type);
	    }
	    return vehicleIdentifierType;
	  }

	  public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + type;
	    return result;
	  }

	  public boolean equals(final Object obj) {
	    if (this == obj)
	      return true;
	    if (obj == null)
	      return false;
	    if (getClass() != obj.getClass())
	      return false;
	    final VehicleIdentifierType other = (VehicleIdentifierType) obj;
	    if (type != other.type)
	      return false;
	    return true;
	  }

	  public String toString() {
	    return text;
	  }

}
