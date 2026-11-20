package com.iot.dtm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** dtm 业务协议编解码开关与防护参数。 */
@Component
@ConfigurationProperties(prefix = "dtm.protocol")
public class CodecProperties {

    /** 置 false 可一键回退为纯字节透传。 */
    private boolean enabled = true;

    /** 解码/编码失败时是否仍把原报文转发出去（不丢消息）。 */
    private boolean failOpen = true;

    /** 单条 serviceData 最大字节数，防止畸形报文造成内存问题。 */
    private int maxPayloadBytes = 8192;

    /** repeat 组最大条目数，防止畸形计数导致 OOM。 */
    private int maxRepeatItems = 256;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isFailOpen() { return failOpen; }
    public void setFailOpen(boolean failOpen) { this.failOpen = failOpen; }
    public int getMaxPayloadBytes() { return maxPayloadBytes; }
    public void setMaxPayloadBytes(int maxPayloadBytes) { this.maxPayloadBytes = maxPayloadBytes; }
    public int getMaxRepeatItems() { return maxRepeatItems; }
    public void setMaxRepeatItems(int maxRepeatItems) { this.maxRepeatItems = maxRepeatItems; }
}
