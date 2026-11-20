package com.proxy.netty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.netty.channel.Channel;

/**
 * Netty 接入层的会话注册表（ProxyTcp 唯一 TCP 接入层使用）。
 *
 * <ul>
 *   <li>addressChannels: "ip:port" -> Channel，连接建立时注册、关闭时移除；</li>
 *   <li>deviceChannels: deviceId -> Channel，上行解码得到 deviceId 后由
 *       TransportUplinkProcessor 调用 bindDevice 绑定，供下行按 deviceId 取 Channel 写回。</li>
 * </ul>
 */
@Component
public class ChannelRegistry {

    private final Map<String, Channel> addressChannels = new ConcurrentHashMap<>();
    private final Map<String, Channel> deviceChannels = new ConcurrentHashMap<>();

    public void registerAddress(String addressKey, Channel channel) {
        addressChannels.put(addressKey, channel);
    }

    public void unregisterAddress(String addressKey) {
        addressChannels.remove(addressKey);
    }

    public void bindDevice(String deviceId, String ip, int port) {
        Channel channel = addressChannels.get(ip + ":" + port);
        if (channel != null) {
            deviceChannels.put(deviceId, channel);
        }
    }

    public Channel getChannel(String deviceId) {
        return deviceChannels.get(deviceId);
    }

    public void unregisterDevice(String deviceId) {
        deviceChannels.remove(deviceId);
    }

    /**
     * 当前在线的设备 deviceId 集合（即已绑定 Channel 的设备），供重发调度判断设备是否在线。
     */
    public java.util.Set<String> getOnlineDeviceIds() {
        return deviceChannels.keySet();
    }
}
