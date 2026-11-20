package com.proxy.netty;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * UplinkFrameDecoder 确定性单元测试（不依赖 broker / 网络）。
 *
 * 帧格式 = 0xFF 0xEE + 2字节小端长度 + 载荷，与旧 ServerThread 解码、TcpSendThread 下行完全一致。
 */
public class UplinkFrameDecoderTest {

    private static byte[] buildFrame(byte[] payload) {
        byte[] frame = new byte[4 + payload.length];
        frame[0] = (byte) 0xFF;
        frame[1] = (byte) 0xEE;
        int len = payload.length;
        frame[2] = (byte) (len & 0xFF);          // 小端低位
        frame[3] = (byte) ((len >> 8) & 0xFF);   // 小端高位
        System.arraycopy(payload, 0, frame, 4, payload.length);
        return frame;
    }

    private static byte[] toBytes(ByteBuf buf) {
        byte[] out = new byte[buf.readableBytes()];
        buf.readBytes(out);
        return out;
    }

    @Test
    public void testWholeFrameDecoded() {
        byte[] payload = {1, 2, 3, 4, 5};
        byte[] frame = buildFrame(payload);
        EmbeddedChannel ch = new EmbeddedChannel(new UplinkFrameDecoder());

        ch.writeInbound(Unpooled.wrappedBuffer(frame));

        ByteBuf out = ch.readInbound();
        assertNotNull("整帧应解码出一帧", out);
        assertEquals(frame.length, out.readableBytes());
        assertArrayEquals(frame, toBytes(out));
        assertNull("不应产生多余帧", ch.readInbound());
        ch.finish();
    }

    @Test
    public void testHalfPacketDeferredThenDecoded() {
        byte[] payload = {10, 20, 30, 40, 50, 60};
        byte[] frame = buildFrame(payload);
        EmbeddedChannel ch = new EmbeddedChannel(new UplinkFrameDecoder());

        // 先写入帧头 + 部分载荷
        ch.writeInbound(Unpooled.wrappedBuffer(frame, 0, 5));
        assertNull("半包不应解码", ch.readInbound());

        // 补齐剩余字节
        ch.writeInbound(Unpooled.wrappedBuffer(frame, 5, frame.length - 5));
        ByteBuf out = ch.readInbound();
        assertNotNull("补齐后应解码出一帧", out);
        assertArrayEquals(frame, toBytes(out));
        ch.finish();
    }

    @Test
    public void testStickyPacketsSplit() {
        byte[] p1 = {1, 2, 3};
        byte[] p2 = {4, 5, 6, 7, 8};
        byte[] f1 = buildFrame(p1);
        byte[] f2 = buildFrame(p2);
        byte[] combined = new byte[f1.length + f2.length];
        System.arraycopy(f1, 0, combined, 0, f1.length);
        System.arraycopy(f2, 0, combined, f1.length, f2.length);

        EmbeddedChannel ch = new EmbeddedChannel(new UplinkFrameDecoder());
        ch.writeInbound(Unpooled.wrappedBuffer(combined));

        ByteBuf o1 = ch.readInbound();
        ByteBuf o2 = ch.readInbound();
        assertNotNull(o1);
        assertNotNull(o2);
        assertArrayEquals(f1, toBytes(o1));
        assertArrayEquals(f2, toBytes(o2));
        assertNull(ch.readInbound());
        ch.finish();
    }

    @Test
    public void testInvalidHeaderResync() {
        byte[] garbage = {(byte) 0x00, (byte) 0x01}; // 非法帧头
        byte[] payload = {9, 8, 7};
        byte[] frame = buildFrame(payload);
        byte[] stream = new byte[garbage.length + frame.length];
        System.arraycopy(garbage, 0, stream, 0, garbage.length);
        System.arraycopy(frame, 0, stream, garbage.length, frame.length);

        EmbeddedChannel ch = new EmbeddedChannel(new UplinkFrameDecoder());
        ch.writeInbound(Unpooled.wrappedBuffer(stream));

        ByteBuf out = ch.readInbound();
        assertNotNull("丢弃垃圾字节后应解码出合法帧", out);
        assertArrayEquals(frame, toBytes(out));
        ch.finish();
    }

    @Test
    public void testDeclaredLengthLargerThanAvailableWaits() {
        byte[] payload = new byte[100];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) i;
        }
        byte[] frame = buildFrame(payload);
        EmbeddedChannel ch = new EmbeddedChannel(new UplinkFrameDecoder());

        // 只发帧头 + 10 字节载荷，声明长度 100
        ch.writeInbound(Unpooled.wrappedBuffer(frame, 0, 4 + 10));
        assertNull("载荷不足时应等待，不产生帧", ch.readInbound());

        // 补齐剩余 90 字节
        ch.writeInbound(Unpooled.wrappedBuffer(frame, 4 + 10, frame.length - (4 + 10)));
        ByteBuf out = ch.readInbound();
        assertNotNull(out);
        assertArrayEquals(frame, toBytes(out));
        ch.finish();
    }
}
