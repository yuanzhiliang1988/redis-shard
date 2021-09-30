package com.ttyc.redis.shard;

import com.ttyc.redis.shard.enums.SerializerTypeEnum;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author yuanzl
 * @date 2021/8/27 2:03 下午
 */
@Slf4j
public class ShardBeanFactory<T> {
    private final ConcurrentHashMap<String, T> beanMap = new ConcurrentHashMap();

    @SneakyThrows
    public T getBean(String className) throws BeansException {
        return beanMap.computeIfAbsent(className,k->{
            Class aClass = null;
            try {
                aClass = Class.forName(className);
                return (T)aClass.newInstance();
            } catch (Exception e) {
                log.error("{}加载Bean异常,{}",className,e);
                return null;
            }
        });
    }

    @SneakyThrows
    public T getBeanConstructor(String className) {
        return beanMap.computeIfAbsent(className,k->{
            try {
                Class bclass = Class.forName(className);
                Constructor con =  bclass.getConstructor(Class.class);

                return (T)con.newInstance(Object.class);
            } catch (Exception e) {
                log.error("{}加载Bean异常,{}",className,e);
                return null;
            }
        });
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

    public static void main(String[] args) {
        ShardBeanFactory factory = new ShardBeanFactory();
        System.out.println(factory.getBean(SerializerTypeEnum.STRING.getName()));
        System.out.println(factory.getBean(SerializerTypeEnum.JACKSON.getName()));
        System.out.println(factory.getBean(SerializerTypeEnum.STRING.getName()));
    }
}
