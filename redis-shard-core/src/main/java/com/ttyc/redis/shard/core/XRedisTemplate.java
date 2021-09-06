package com.ttyc.redis.shard.core;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author yuanzl
 * @date 2021/8/23 4:14 下午
 */
public class XRedisTemplate extends RedisTemplate {

    public XRedisTemplate() {
    }

    protected RedisConnection preProcessConnection(RedisConnection connection, boolean existingConnection) {
        return super.preProcessConnection(connection, existingConnection);
    }
}
