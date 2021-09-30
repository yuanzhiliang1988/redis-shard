package com.ttyc.redis.shard.utils;

/**
 * 序列化工具类
 *
 * @author yuhao.wang3
 */
public abstract class SerializationUtils {

    public static final byte[] EMPTY_ARRAY = new byte[0];

    public static boolean isEmpty(byte[] data) {
        return (data == null || data.length == 0);
    }
}
