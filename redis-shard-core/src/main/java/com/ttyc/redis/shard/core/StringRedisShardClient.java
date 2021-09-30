package com.ttyc.redis.shard.core;

import com.ttyc.redis.shard.serializer.StringRedisSerializer;

/**
 * @author yuanzl
 * @date 2021/9/8 4:18 下午
 */
public class StringRedisShardClient extends RedisShardClient<String>{

    public StringRedisShardClient(String appName) {
        super(appName);
    }

    @Override
    protected StringRedisSerializer stringRedisSerializer(){
        return new StringRedisSerializer();
    }
}
