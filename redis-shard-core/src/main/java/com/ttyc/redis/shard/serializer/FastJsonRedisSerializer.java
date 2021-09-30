package com.ttyc.redis.shard.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.IOUtils;
import com.ttyc.redis.shard.serializer.base.AbstractRedisSerializer;
import com.ttyc.redis.shard.utils.SerializationUtils;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.Arrays;

/**
 * FastJson 序列化方式
 *
 * @author yuhao.wang
 */
public class FastJsonRedisSerializer<T> extends AbstractRedisSerializer<T> {
    private static final ParserConfig DEFAULT_REDIS_CONFIG = new ParserConfig();
    private Class<T> clazz;

    static {
        DEFAULT_REDIS_CONFIG.setAutoTypeSupport(true);
    }

    public FastJsonRedisSerializer(){
        this.clazz = (Class<T>) Object.class;
    }

    @Override
    public byte[] serialize(T value) throws org.springframework.data.redis.serializer.SerializationException {
        if (value == null) {
            return SerializationUtils.EMPTY_ARRAY;
        }

        try {
            return JSON.toJSONBytes(value, SerializerFeature.WriteClassName);
        } catch (Exception e) {
            throw new SerializationException(String.format("FastJsonRedisSerializer 序列化异常: %s, 【%s】", e.getMessage(), JSON.toJSONString(value)), e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws org.springframework.data.redis.serializer.SerializationException {
        if (SerializationUtils.isEmpty(bytes)) {
            return null;
        }

        if (Arrays.equals(getNullValueBytes(), bytes)) {
            return null;
        }

        try {
            return JSON.parseObject(new String(bytes, IOUtils.UTF8),this.clazz, DEFAULT_REDIS_CONFIG, new Feature[0]);
        } catch (Exception e) {
            throw new SerializationException(String.format("FastJsonRedisSerializer 反序列化异常: %s, 【%s】", e.getMessage(), JSON.toJSONString(bytes)), e);
        }
    }
}