package com.proxy.handler;

public class Constants {
	public static int VEHICLEIDENTIFIERTYPE_VIN = 0;
	public static int VEHICLEIDENTIFIERTYPE_TERMINALNO = 1;
	public static int VEHICLEIDENTIFIERTYPE_DEVICEID = 2;
	public static int ENCODINGTYPE_PROTOBUF = 0;
	public static int ENCODINGTYPE_JSON = 1;
	public static int ENCRYPTIONTYPE_AECGCM = 0;
	public static int ENCRYPTIONTYPE_DES = 1;
	public static int ENCRYPTIONTYPE_RSA = 2;
	public static int ENCRYPTIONTYPE_SHA = 3;
	
	public static int SIGNATURE_LENGTH = 16;
	
	public static int SERVICETYPE_ACK = 0;
	public static int SERVICETYPE_HEART_BEAT = 1;
	public static int SERVICETYPE_POSITION_REPORT = 2;
	public static int SERVICETYPE_STATUS_REPORT = 3;
	public static int SERVICETYPE_ALARM = 4;
	public static int SERVICETYPE_NOTIFICATION = 5;
	public static int SERVICETYPE_DIAGNOSE = 6;
	public static int SERVICETYPE_GEOFENCE = 7;

	public static int SERVICETYPE_REMOTE_START = 31;
	public static int SERVICETYPE_REMOTE_STOP = 32;
	public static int SERVICETYPE_REMOTE_LOCK = 33;

	public static int SERVICETYPE_TASK_QUALITY_SPEED = 41;
	public static int SERVICETYPE_TASK_QUALITY_HEIGHT = 42;
	public static int SERVICETYPE_TASK_QUALITY_DEPTH = 43;
	public static int SERVICETYPE_TASK_QUALITY_WIDTH = 44;
	public static int SERVICETYPE_TASK_QUALITY_SPACE = 45;

	public static int SERVICETYPE_TASK_START = 61;
	public static int SERVICETYPE_TASK_PAUSE = 62;
	public static int SERVICETYPE_TASK_STOP = 63;
	public static int SERVICETYPE_TASK_CANCEL = 64;

	public static int DEFAULT_TTL = 60*1000;
}
