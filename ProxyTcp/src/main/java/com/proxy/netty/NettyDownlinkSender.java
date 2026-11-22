package com.proxy.netty;

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.proxy.model.Message;
import com.proxy.utils.ByteUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import jakarta.annotation.PostConstruct;

/**
 * Netty 下行发送（ProxyTcp 唯一 TCP 接入层的下行路径）。从 sendQueue 取 Message，按 deviceId 取
 * 已注册的 Channel 写回 0xFFEE + 长度(小端) + payload 帧，复用与旧 TcpSendThread 完全一致的帧格式。
 */
@Component
public class NettyDownlinkSender implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Qualifier("sendQueue")
    private LinkedBlockingQueue<Message> sendQueue;

    @Autowired
    private ChannelRegistry channelRegistry;

    @PostConstruct
    public void init() {
        new Thread(this, "NettyDownlinkSender").start();
        logger.info("NettyDownlinkSender started");
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Message message = sendQueue.take();
                if (message == null) {
                    continue;
                }
                Channel channel = channelRegistry.getChannel(message.getDeviceId());
                if (channel == null) {
                    logger.debug("Netty downlink: no channel for device " + message.getDeviceId());
                    continue;
                }
                byte[] payload = message.getPayload();
                if (payload == null) {
                    continue;
                }
                // 帧格式与旧 TcpSendThread 一致：0xFF 0xEE + 2字节小端长度 + payload
                byte[] sizeBytes = ByteUtil.short2BigByte2Array(payload.length);
                ByteBuf out = channel.alloc().buffer(4 + payload.length);
                out.writeByte((byte) 0xFF);
                out.writeByte((byte) 0xEE);
                out.writeBytes(sizeBytes);
                out.writeBytes(payload);
                channel.writeAndFlush(out);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("Netty downlink error: " + e.getMessage());
            }
        }
    }
}
