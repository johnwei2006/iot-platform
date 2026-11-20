package com.proxy.codec;

public class EncodeException extends Exception {
    public EncodeException(Throwable cause) {
        super(cause != null ? cause.toString() : "NULL");
    }

    public EncodeException(String msg) {
        super(msg);
    }

    public EncodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
