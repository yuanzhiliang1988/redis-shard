package com.ttyc.redis.shard;

import lombok.SneakyThrows;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author yuanzl
 * @date 2021/8/27 2:03 下午
 */
public class ShardBeanFactory<T> {

    @SneakyThrows
    public T getBean(String className) throws BeansException {
        Class aClass = Class.forName(className);
        return (T)aClass.newInstance();
    }

    @SneakyThrows
    public <T> T getBeanConstructor(String className) throws BeansException {
        Class bclass = ClassLoader.getSystemClassLoader().loadClass(className);
        Constructor[] con =  bclass.getConstructors();
        return (T)con[0].newInstance(Object.class);
    }

    /**
     * 获取接口下所有实现类（不含抽象类）
     * @param tClass
     * @param <T>
     * @return
     * @throws BeansException
     */
    @SneakyThrows
    public <T> List<T> getBeansByInterface(Class<T> tClass) throws BeansException {
        List<T> configHandler = new ArrayList<>();
        Reflections reflections = new Reflections(tClass.getPackage().getName());
        Set<Class<? extends T>> clazzs = reflections.getSubTypesOf(tClass);
        for (Class<? extends T> clazz : clazzs) {
            if(Modifier.isAbstract(clazz.getModifiers())){
                continue;
            }
            configHandler.add((T)clazz.newInstance());
        }

        return configHandler;
    }
}
