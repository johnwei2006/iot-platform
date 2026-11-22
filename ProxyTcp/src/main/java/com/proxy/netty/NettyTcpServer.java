package com.proxy.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import jakarta.annotation.PreDestroy;

/**
 * Netty 事件循环接入层（ProxyTcp 唯一 TCP 入口）。采用 boss+worker 事件循环，支撑大量并发连接，
 * 消除经典阻塞 I/O 的每连接一线程 + 忙轮询瓶颈。
 */
@Component
public class NettyTcpServer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${tcpport:50001}")
    private int port;

    @Autowired
    private UplinkChannelHandler uplinkChannelHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private volatile int boundPort = -1;

    public int getBoundPort() {
        return boundPort;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 1024)
             .childOption(ChannelOption.SO_KEEPALIVE, true)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline()
                       .addLast(new UplinkFrameDecoder())
                       .addLast(uplinkChannelHandler);
                 }
             });
            ChannelFuture f = b.bind(port).sync();
            boundPort = ((java.net.InetSocketAddress) f.channel().localAddress()).getPort();
            logger.info("Netty TCP server started on port " + boundPort);
            f.channel().closeFuture().sync();
        } finally {
            // 资源释放交由 @PreDestroy stop() 处理，避免重复 shutdown
        }
    }

    @PreDestroy
    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        logger.info("Netty TCP server stopped");
    }
}
