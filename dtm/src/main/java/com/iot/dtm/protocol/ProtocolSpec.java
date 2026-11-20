package com.iot.dtm.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单个 serviceType 的声明式规格。字段顺序即线上字节顺序；
 * 端序在字段上显式声明（默认 big），避免把 TCP 帧长度的小端误套到业务字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtocolSpec {

    private String hex;
    private int ordinal;
    private String name;
    private String cn;
    private String direction;
    private String transactionType;
    private String initiator;
    private String description;
    private Integer excelRow;
    private boolean empty;
    private boolean unsupported;
    private List<FieldSpec> fields = new ArrayList<FieldSpec>();

    public int code() {
        return Integer.decode(hex);
    }

    public String getHex() { return hex; }
    public void setHex(String hex) { this.hex = hex; }
    public int getOrdinal() { return ordinal; }
    public void setOrdinal(int ordinal) { this.ordinal = ordinal; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCn() { return cn; }
    public void setCn(String cn) { this.cn = cn; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getInitiator() { return initiator; }
    public void setInitiator(String initiator) { this.initiator = initiator; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getExcelRow() { return excelRow; }
    public void setExcelRow(Integer excelRow) { this.excelRow = excelRow; }
    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
    public boolean isUnsupported() { return unsupported; }
    public void setUnsupported(boolean unsupported) { this.unsupported = unsupported; }
    public List<FieldSpec> getFields() { return Collections.unmodifiableList(fields); }
    public void setFields(List<FieldSpec> fields) { this.fields = fields == null ? new ArrayList<FieldSpec>() : fields; }

    /** 字段类型：u8 / u16 / u32 / string / hex / time6 / time3 / repeat。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldSpec {

        private String name;
        private String type;
        private String endian;
        private String countRef;
        private int length;
        private boolean variable;
        private List<FieldSpec> fields = new ArrayList<FieldSpec>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getEndian() { return endian; }
        public void setEndian(String endian) { this.endian = endian; }
        public String getCountRef() { return countRef; }
        public void setCountRef(String countRef) { this.countRef = countRef; }
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
        public boolean isVariable() { return variable; }
        public void setVariable(boolean variable) { this.variable = variable; }
        public List<FieldSpec> getFields() { return Collections.unmodifiableList(fields); }
        public void setFields(List<FieldSpec> fields) { this.fields = fields == null ? new ArrayList<FieldSpec>() : fields; }
        public boolean isLittleEndian() { return "little".equalsIgnoreCase(endian); }
    }
}
