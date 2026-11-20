package com.proxy.netty;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * 上行帧拆包（自定义，精确复刻旧 ServerThread 拆包语义）。
 *
 * <p>帧格式 = 0xFF 0xEE (2字节) + 2字节长度(小端) + 载荷。
 * 处理半包/粘包：不足整帧则等待更多数据；找不到合法帧头则跳过 1 字节重新同步；
 * 长度字段非法的超大帧直接丢弃帧头，避免无限等待。
 *
 * <p>不使用 LengthFieldBasedFrameDecoder 的 ByteOrder 构造器（4.1.36 不提供），
 * 改由本类显式按小端解析长度，保证与既有的上行/下行字节序完全一致。
 */
public class UplinkFrameDecoder extends ByteToMessageDecoder {

    private static final int HEADER_SIZE = 4; // FF EE + 2 bytes length
    private static final int MAX_FRAME_LENGTH = 10 * 1024 * 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < HEADER_SIZE) {
            return;
        }
        in.markReaderIndex();
        byte b0 = in.readByte();
        byte b1 = in.readByte();
        if (b0 != (byte) 0xFF || b1 != (byte) 0xEE) {
            // 非法帧头：回退并跳过 1 字节重新同步
            in.resetReaderIndex();
            in.skipBytes(1);
            return;
        }
        int low = in.readByte() & 0xFF;
        int high = in.readByte() & 0xFF;
        int payloadLength = (high << 8) | low; // 小端，与旧解码/短2BigByte2Array 一致
        int totalLength = HEADER_SIZE + payloadLength;

        if (payloadLength < 0 || totalLength > MAX_FRAME_LENGTH) {
            // 长度非法：丢弃帧头，避免死等
            in.resetReaderIndex();
            in.skipBytes(HEADER_SIZE);
            return;
        }
        if (in.readableBytes() < payloadLength) {
            // 半包：等待更多数据
            in.resetReaderIndex();
            return;
        }
        byte[] frame = new byte[totalLength];
        in.resetReaderIndex();
        in.readBytes(frame);
        // 产出完整帧 ByteBuf（含 4 字节头），交由 UplinkChannelHandler 校验并提取 payload
        out.add(ctx.alloc().buffer(totalLength).writeBytes(frame));
    }
}
