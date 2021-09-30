package com.ttyc.redis.shard.serializer;

import com.alibaba.fastjson.JSON;
import com.ttyc.redis.shard.serializer.base.AbstractRedisSerializer;
import com.ttyc.redis.shard.utils.SerializationUtils;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.runtime.RuntimeSchema;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.Arrays;

/**
 * Protostuff 序列化方式
 *
 * @author yuhao.wang
 */
public class ProtostuffRedisSerializer<T> extends AbstractRedisSerializer<T> {

    RuntimeSchema<Wrapper> schema = RuntimeSchema.createFrom(Wrapper.class);

    static {
        System.getProperties().setProperty("protostuff.runtime.always_use_sun_reflection_factory", "true");
        System.getProperties().setProperty("protostuff.runtime.preserve_null_elements", "true");
        System.getProperties().setProperty("protostuff.runtime.morph_collection_interfaces", "true");
        System.getProperties().setProperty("protostuff.runtime.morph_map_interfaces", "true");
        System.getProperties().setProperty("protostuff.runtime.morph_non_final_pojos", "true");
    }

    @Override
    public byte[] serialize(Object value) throws org.springframework.data.redis.serializer.SerializationException {
        if (value == null) {
            return SerializationUtils.EMPTY_ARRAY;
        }

        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            return ProtostuffIOUtil.toByteArray(new Wrapper<>(value), schema, buffer);
        } catch (Exception e) {
            throw new SerializationException(String.format("ProtostuffRedisSerializer 序列化异常: %s, 【%s】", e.getMessage(), JSON.toJSONString(value)), e);
        } finally {
            buffer.clear();
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
            Wrapper<T> wrapper = new Wrapper<>(null);
            ProtostuffIOUtil.mergeFrom(bytes, wrapper, schema);
            return wrapper.getData();
        } catch (Exception e) {
            throw new SerializationException(String.format("ProtostuffRedisSerializer 反序列化异常: %s, 【%s】", e.getMessage(), JSON.toJSONString(bytes)), e);
        }
    }

    /**
     * protobuff只能序列化pojo类，不能直接序列化List 或者Map,如果要序列化list或者map，需要用一个wrapper类包装一下
     *
     * @param <T> T
     */
    static class Wrapper<T> {
        T data;

        public Wrapper(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }
    }

}