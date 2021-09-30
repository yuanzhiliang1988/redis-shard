package com.ttyc.redis.shard.support;

import java.io.Serializable;
import java.util.List;

/**
 * 分片配置信息
 * @author yuanzl
 * @date 2021/8/25 2:19 下午
 */
public class ShardConfig implements Serializable {
    /**
     * 序列化方式: 默认：jdk
     * kryo
     * fastjson
     * jackson
     * jdk
     * protostuff
     * string
     */
    private String serializer = "jdk";
    /**
     * key路由正则表达式，符合正则表达试的key会路由到同一分片，支持多个正则
     * 使用方式：
     * redis.shard.config.key-regex[0]=(test_)
     * redis.shard.config.key-regex[1]=(test2_)
     */
    private List<String> keyRegex;

    public String getSerializer() {
        return serializer;
    }

    public void setSerializer(String serializer) {
        this.serializer = serializer;
    }

    public List<String> getKeyRegex() {
        return keyRegex;
    }

    public void setKeyRegex(List<String> keyRegex) {
        this.keyRegex = keyRegex;
    }
}
