package com.ttyc.redis.shard.enums;

import com.ttyc.redis.shard.ShardBeanFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * redis序列化方式
 */
public enum SerializerTypeEnum {
    STRING("string","org.springframework.data.redis.serializer.StringRedisSerializer"),
    FASTJSON("fastjson","com.ttyc.redis.shard.serializer.FastJsonRedisSerializer"),
    JACKSON("jackson","org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer"),
    JDK("jdk","org.springframework.data.redis.serializer.JdkSerializationRedisSerializer"),
    KRYO("kryo","com.ttyc.redis.shard.serializer.KryoRedisSerializer"),
    PROTOSUFF("protostuff","com.ttyc.redis.shard.serializer.ProtostuffRedisSerializer");

    private String type;
    private String name;

    SerializerTypeEnum(String type, String name){
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static RedisSerializer getSerializer(String type, ShardBeanFactory<RedisSerializer> shardBeanFactory) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (null == type) {
            return null;
        }

        String serializer = Arrays.stream(SerializerTypeEnum.values()).filter(item -> item.getType().equals(type)).findFirst().get().getName();
        RedisSerializer redisSerializer;
        if(type.equals(SerializerTypeEnum.JACKSON.getType())){
            redisSerializer = shardBeanFactory.getBeanConstructor(serializer);
        }else{
            redisSerializer = shardBeanFactory.getBean(serializer);
        }

        return redisSerializer;
    }
}
