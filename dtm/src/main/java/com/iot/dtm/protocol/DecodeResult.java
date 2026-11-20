package com.iot.dtm.protocol;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** 上行解码结果：状态 + 结构化字段 + 元信息。 */
public class DecodeResult {

    public enum Status {
        OK, EMPTY, UNSUPPORTED, FAILED
    }

    private final Status status;
    private final String protocolName;
    private final String transactionType;
    private final String direction;
    private final String reason;
    private final ObjectNode decoded;

    private DecodeResult(Status status, String protocolName, String transactionType,
                         String direction, String reason, ObjectNode decoded) {
        this.status = status;
        this.protocolName = protocolName;
        this.transactionType = transactionType;
        this.direction = direction;
        this.reason = reason;
        this.decoded = decoded == null ? JsonNodeFactory.instance.objectNode() : decoded;
    }

    public static DecodeResult ok(ProtocolSpec spec, ObjectNode decoded) {
        return new DecodeResult(Status.OK, spec.getName(), spec.getTransactionType(),
                spec.getDirection(), null, decoded);
    }

    public static DecodeResult empty(ProtocolSpec spec) {
        return new DecodeResult(Status.EMPTY, spec.getName(), spec.getTransactionType(),
                spec.getDirection(), null, null);
    }

    public static DecodeResult unsupported(ProtocolSpec spec, String reason) {
        return new DecodeResult(Status.UNSUPPORTED, spec == null ? "UNKNOWN" : spec.getName(),
                spec == null ? "" : spec.getTransactionType(),
                spec == null ? "" : spec.getDirection(), reason, null);
    }

    public static DecodeResult failed(ProtocolSpec spec, String reason) {
        return new DecodeResult(Status.FAILED, spec == null ? "UNKNOWN" : spec.getName(),
                spec == null ? "" : spec.getTransactionType(),
                spec == null ? "" : spec.getDirection(), reason, null);
    }

    public Status getStatus() { return status; }
    public String getProtocolName() { return protocolName; }
    public String getTransactionType() { return transactionType; }
    public String getDirection() { return direction; }
    public String getReason() { return reason; }
    public ObjectNode getDecoded() { return decoded; }
    public boolean hasDecoded() { return status == Status.OK; }
}
