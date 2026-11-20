package com.proxy.netty;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.proxy.model.Message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 上行通道处理器：把完整帧校验 0xFFEE 后提取 payload，组装成 Message 投入 receiveQueue，
 * 复用现有 UplinkProcessThread -> TransportUplinkProcessor 上行链路（与 classic 模式共用）。
 * 同时在连接建立/关闭时维护 ChannelRegistry。
 */
@Sharable
@Component
public class UplinkChannelHandler extends ChannelInboundHandlerAdapter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Qualifier("receiveQueue")
    private LinkedBlockingQueue<Message> receiveQueue;

    private final ChannelRegistry channelRegistry;

    @Autowired
    public UplinkChannelHandler(ChannelRegistry channelRegistry) {
        this.channelRegistry = channelRegistry;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String key = addressKey(ctx);
        channelRegistry.registerAddress(key, ctx.channel());
        logger.info("Netty channel active: " + key);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String key = addressKey(ctx);
        channelRegistry.unregisterAddress(key);
        logger.info("Netty channel inactive: " + key);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        try {
            byte[] frame = new byte[buf.readableBytes()];
            buf.readBytes(frame);

            if (frame.length < 4 || frame[0] != (byte) 0xFF || frame[1] != (byte) 0xEE) {
                logger.debug("Netty: dropped frame with invalid prefix");
                return;
            }
            int payloadLength = ((frame[3] & 0xFF) << 8) | (frame[2] & 0xFF);
            int totalLength = 4 + payloadLength;
            if (frame.length < totalLength) {
                logger.debug("Netty: frame shorter than declared length, dropped");
                return;
            }

            byte[] payload = Arrays.copyOfRange(frame, 4, totalLength);
            InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
            Message message = new Message();
            message.setPayload(payload);
            message.setIp(remote.getAddress().getHostAddress());
            message.setPort(remote.getPort());
            message.setConnectType(Message.CONNECTTYPE_TCP);
            receiveQueue.offer(message);
            logger.debug("Netty received uplink from " + message.getIp() + ":" + message.getPort()
                    + " size " + payload.length);
        } finally {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Netty channel exception: " + cause.getMessage());
        ctx.close();
    }

    private String addressKey(ChannelHandlerContext ctx) {
        InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
        return remote.getAddress().getHostAddress() + ":" + remote.getPort();
    }
}
