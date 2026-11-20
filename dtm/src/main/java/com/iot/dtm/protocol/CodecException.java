package com.iot.dtm.protocol;

/** 解码/编码失败。用于 fail-open 处理，调用方不得让它冒泡成 MQ retry。 */
public class CodecException extends RuntimeException {

    public CodecException(String message) {
        super(message);
    }

    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
