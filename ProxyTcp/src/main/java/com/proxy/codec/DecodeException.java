package com.proxy.codec;

public class DecodeException extends Exception {
	public static int ERRORCODE_LENGTH_LESS_THEN_HEADER = 1;
	public static int ERRORCODE_TRANSPORT_LENGTH_INCORRECT = 2;
	public static int ERRORCODE_SIGNATURE_LENGTH_INCORRECT = 3;
	public static int ERRORCODE_SIGNATURE_NOT_SAME = 4;
	public static int ERRORCODE_DECODE_EXCEPTION= 5;
	
	int errorCode;
	
	public DecodeException(int m_errorCode, String message) {
		super(message);
		errorCode = m_errorCode;
	}
	
    public DecodeException(Throwable cause) {
        super(cause != null ? cause.toString() : "NULL");
    }

    public DecodeException(String msg) {
        super(msg);
    }

    public DecodeException(String message, Throwable cause) {
        super(message, cause);
    }

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
}
