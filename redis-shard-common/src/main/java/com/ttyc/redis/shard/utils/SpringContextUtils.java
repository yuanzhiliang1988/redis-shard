package com.ttyc.redis.shard.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author yuanzl
 * @date 2021/9/9 9:29 下午
 */
public class SpringContextUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static Object getBean(Class c){
        return applicationContext.getBean(c);
    }

    public static Object getBean(String beanName){
        return applicationContext.getBean(beanName);
    }
}
