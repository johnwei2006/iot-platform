package com.proxy;

import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.proxy.model.Message;
import com.proxy.netty.NettyTcpServer;
import com.proxy.transport.TransportUplinkProcessor;
import com.proxy.utils.BeanUtil;

@SpringBootApplication
@ComponentScan(basePackages={"com.proxy"})
public class ProxyApplication {

	private static final Logger logger = LoggerFactory.getLogger(ProxyApplication.class);
	
	@Bean
    public LinkedBlockingQueue<Message> receiveQueue() {
        return new LinkedBlockingQueue<>();
    }
	
	@Bean
    public LinkedBlockingQueue<Message> sendQueue() {
        return new LinkedBlockingQueue<>(50);
    }
	
	public static void main(String[] args) {
    	ConfigurableApplicationContext context = SpringApplication.run(ProxyApplication.class,args);
    	BeanUtil.setApplicationContext(context);
    	
    	TransportUplinkProcessor transportUplinkProcessor = context.getBean(TransportUplinkProcessor.class);
    	transportUplinkProcessor.startup();

    	logger.info("Starting ProxyTcp (Netty TCP server)");
    	NettyTcpServer nettyServer = context.getBean(NettyTcpServer.class);
    	try {
    		nettyServer.start();
    	} catch (InterruptedException e) {
    		Thread.currentThread().interrupt();
    	}
    }
}
