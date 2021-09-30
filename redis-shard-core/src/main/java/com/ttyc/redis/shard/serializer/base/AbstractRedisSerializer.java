package com.ttyc.redis.shard.serializer.base;

import com.ttyc.redis.shard.support.NullValue;
import org.springframework.data.redis.serializer.RedisSerializer;
import java.util.Objects;

/**
 * 序列化方式的抽象实现
 *
 * @author yuhao.wang
 */
public abstract class AbstractRedisSerializer<T> implements RedisSerializer<T> {
    private byte[] nullValueBytes;

    /**
     * 获取空值的序列化值
     *
     * @return byte[]
     */
    public byte[] getNullValueBytes() {
        if (Objects.isNull(nullValueBytes)) {
            synchronized (this) {
                nullValueBytes = serialize((T)NullValue.INSTANCE);
            }
        }
        return nullValueBytes;
    }
}