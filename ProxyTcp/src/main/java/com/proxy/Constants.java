package com.proxy;

public class Constants {
	public static int VEHICLEIDENTIFIERTYPE_VIN 		= 0;
	public static int VEHICLEIDENTIFIERTYPE_TERMINALNO 	= 1;
	public static int VEHICLEIDENTIFIERTYPE_DEVICEID 	= 2;
	
	public static final byte TRANSPORT_VERSION_1 		= 1;
	public static final byte TRANSPORT_VERSION_2 		= 2;
	
	public static int ENCODINGTYPE_BYTE 				= 0;
	public static int ENCODINGTYPE_PROTOBUF 			= 1;
	public static int ENCODINGTYPE_JSON 				= 2;

	public static int SIGNATURE_LENGTH 					= 16;
	
	public static String SERVICEPROXY 					= "serviceproxy";
	public static String SENDFAILUREPROXY 				= "sendfailureproxy";

	public static int SERVICETYPE_ACK 					= 0;
	public static short SERVICETYPE_HEART_BEAT 			= 1;//vehicle
	public static short SERVICETYPE_ONLINE_CHECK 		= 2;//dual
	
	public static int DEFAULT_TTL 						= 60*1000;
	
	public static int STATUS_SUCCESS 					= 200;
	public static int STATUS_ERROR 						= 400;
	public static int STATUS_WARN 						= 300;

	public static final String DB_TYPE 					= "dbtype";
	public static final String DB_TYPE_H2 				= "h2";
	public static final String DB_TYPE_MONGODB 			= "mongo";
	public static final String DUPLICATE_MESSAGE_DURATION_MINUTES 	= "duplicateMessageDurationMinutes";
	public static final String ONLINE_CHECK_JUDGE_MINUTES 			= "onlineCheckJudgeMinutes";
	public static final String JUDGE_SEND_FAILED_AFTER_MINUTES 		= "judgeSendFailedAferMinutes";
	public static final String RESEND_INTERVAL_SECONDS 				= "resendIntervalSeconds";
	public static final String MAX_RESEND_TIMES 						= "maxResendTimes";
	
	
	public static enum Status {
		SENT, ACKNOWLEDGED, FAILED, RESEND_OVERTIMES, UNKNOW
	}
	
	public static final String LOCK_TYPE_UPLINK 			= "uplink";
	public static final String LOCK_TYPE_DOWNLINK 			= "downlink";
	public static final String LOCK_TYPE_DOWNLINK_RESEND 	= "downlink_resend";
	public static final String LOCK_TYPE_SEND_FAILURE 		= "send_failure";
	public static final String LOCK_TYPE_DELETE_OBSOLETE_FAILED_MESSAGE = "deleteObsoleteFailedMessage";
	
	
	public static final int SEND_FAILURE_ERROR_CODE_OFFLINE 				= 1;
	public static final int SEND_FAILURE_ERROR_CODE_MULTIPLE_SEND_FAILURE 	= 2;
	public static final int SEND_FAILURE_ERROR_CODE_UNKNOW_VEHICLE   		= 3;
	public static final int SEND_FAILURE_ERROR_CODE_ENCODE_ERROR			= 4;
	public static final int SEND_FAILURE_ERROR_CODE_OTHER					= 5;
	
	public static final String RABBITMQ_SERVER_VHOST= "mq_server_vhost";
	public static final String RABBITMQ_SERVER_URL	= "mq_server_url";
	public static final String RABBITMQ_SERVER_PORT = "mq_server_port";
	public static final String RABBITMQ_SERVER_USER = "mq_server_user";
	public static final String RABBITMQ_SERVER_PASSWORD = "mq_server_password";	
}
