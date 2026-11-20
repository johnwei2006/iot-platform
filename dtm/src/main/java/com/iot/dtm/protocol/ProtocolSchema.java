package com.iot.dtm.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/** resources/protocol-schemas.json 的根结构。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtocolSchema {

    private String version;
    private String source;
    private List<ProtocolSpec> protocols = new ArrayList<ProtocolSpec>();

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public List<ProtocolSpec> getProtocols() { return protocols; }
    public void setProtocols(List<ProtocolSpec> protocols) { this.protocols = protocols; }
}
