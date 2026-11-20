package com.proxy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class BeanRebuildUtil {

	public synchronized <T> T rebuildBean(
	        ConfigurableApplicationContext context,
	        Class<T> beanClass
	) throws Exception {

	    DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context.getBeanFactory();
	    String[] beanNames = context.getBeanNamesForType(beanClass);

	    if (beanNames == null || beanNames.length == 0) {
	        throw new RuntimeException("未找到Bean: " + beanClass.getName());
	    }
	    String beanName = beanNames[0];

	    // ==========================================
	    // 1. 销毁旧Bean（必须先销毁，再删定义）
	    // ==========================================
	    if (factory.containsBean(beanName)) {
	        Object oldBean = context.getBean(beanName);
	        // 执行 @PreDestroy 销毁
	        factory.destroyBean(beanName, oldBean);
	    }
	    // 删除Bean定义
	    if (factory.containsBeanDefinition(beanName)) {
	        factory.removeBeanDefinition(beanName);
	    }
	    System.out.println("✅ 旧Bean已安全销毁: " + beanName);

	    // ==========================================
	    // 2. 重新注册Bean
	    // ==========================================
	    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
	            .genericBeanDefinition(beanClass)
	            .setScope(BeanDefinition.SCOPE_SINGLETON)
	            .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR)
	            .getBeanDefinition();

	    factory.registerBeanDefinition(beanName, beanDefinition);

	    // ==========================================
	    // 3. 获取新Bean（触发初始化）
	    // ==========================================
	    T newBean = context.getBean(beanClass);
	    System.out.println("✅ 新Bean已创建: " + beanName);

	    // ==========================================
	    // 4. 【关键】刷新所有依赖此Bean的类（解决影响其他Bean）
	    // ==========================================
	    refreshDependentBeans(factory, beanName);
	    
	    refreshAllScheduledBeans(context);

	    // 清理缓存
	    factory.clearMetadataCache();
	    //factory.clearSingletonCache();
	    //factory.resetBeanDefinition(beanName);

	    System.out.println("✅ 所有依赖Bean已自动更新引用");
	    return newBean;
	}

	// ==============================
	// 核心工具方法：刷新所有依赖Bean
	// ==============================
	private void refreshDependentBeans(DefaultListableBeanFactory factory, String beanName) {
	    // 获取所有依赖了 targetBean 的 Bean
	    String[] dependentBeans = factory.getDependentBeans(beanName);
	    if (dependentBeans == null || dependentBeans.length == 0) {
	        return;
	    }

	    Set<String> processed = new HashSet<>();
	    for (String dependentBeanName : dependentBeans) {
	        if (processed.contains(dependentBeanName)) continue;
	        try {
	            // 只处理单例
	            if (factory.isSingleton(dependentBeanName)) {
	                Object dependentBean = factory.getBean(dependentBeanName);
	                // 重新注入依赖
	                factory.autowireBean(dependentBean);
	                factory.initializeBean(dependentBean, dependentBeanName);
	                processed.add(dependentBeanName);
	            }
	        } catch (Exception e) {
	            System.err.println("刷新依赖Bean失败: " + dependentBeanName + ", " + e.getMessage());
	        }
	    }
	}
	
	private void refreshAllScheduledBeans(ConfigurableApplicationContext context) {
	    String[] beanNames = context.getBeanDefinitionNames();
	    for (String name : beanNames) {
	        try {
	            Object bean = context.getBean(name);
	            // 重启带定时任务的Bean（有startScheduledTask方法）
	            Method method = bean.getClass().getMethod("startScheduledTask");
	            method.invoke(bean);
	        } catch (NoSuchMethodException e) {
	            // 没有定时任务，跳过
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}	
}