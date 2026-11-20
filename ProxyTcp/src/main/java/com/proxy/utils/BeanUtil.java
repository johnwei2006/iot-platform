package com.proxy.utils;

import org.springframework.context.ConfigurableApplicationContext;

public class BeanUtil {
	static ConfigurableApplicationContext applicationContext;
    public static <T> T getBeanByName(String name, Class<T> c){
        return applicationContext.getBean(name, c);
    }
	public static ConfigurableApplicationContext getApplicationContext() {
		return applicationContext;
	}
	public static void setApplicationContext(ConfigurableApplicationContext applicationContext) {
		BeanUtil.applicationContext = applicationContext;
	}
}
