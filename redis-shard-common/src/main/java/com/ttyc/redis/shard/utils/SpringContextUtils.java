package com.ttyc.redis.shard.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * @author yuanzl
 * @date 2021/9/9 9:29 下午
 */
public class SpringContextUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(this.applicationContext==null){
            this.applicationContext = applicationContext;
        }
    }

    public static void setContext(ApplicationContext applicationContext) {
        if(SpringContextUtils.applicationContext==null) {
            SpringContextUtils.applicationContext = applicationContext;
        }
    }

    public static Object getBean(Class c){
        check();
        return applicationContext.getBean(c);
    }

    public static Object getBean(String beanName){
        check();
        return applicationContext.getBean(beanName);
    }

    private static void check(){
        Assert.notNull(applicationContext,"applicationContext不能为空");
    }
}
