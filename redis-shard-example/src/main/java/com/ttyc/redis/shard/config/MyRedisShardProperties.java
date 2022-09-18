package com.ttyc.redis.shard.config;

import com.ttyc.redis.shard.properties.RedisShardProperties;
import com.ttyc.redis.shard.support.Node;
import com.ttyc.redis.shard.support.Pool;
import com.ttyc.redis.shard.support.ShardConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedList;

/**
 * 自定义生成myRedisShardClient1对象配置文件
 * @author yuanzl
 * @date 2020/08/24
 **/
@ConfigurationProperties(prefix = "redis.my1.shard")
public class MyRedisShardProperties {
    /**
     * 分片节点
     * 使用方式：
     * redis.shard.nodes[0].addresses=10.9.198.84:6379
     * redis.shard.nodes[1].addresses=10.100.102.27:6379
     * redis.shard.nodes[1].password=PiC8Ou_mZSU7
     * redis.shard.nodes[2].addresses=10.9.198.84:6380
     * redis.shard.nodes[3].addresses=10.9.188.145:6379
     */
    private LinkedList<Node> nodes;
    /**
     * 分片配置信息
     */
    private ShardConfig config;
    /**
     * jedis
     */
    private final RedisShardProperties.Jedis jedis = new RedisShardProperties.Jedis();

    public LinkedList<Node> getNodes() {
        return nodes;
    }

    public void setNodes(LinkedList<Node> nodes) {
        this.nodes = nodes;
    }

    public RedisShardProperties.Jedis getJedis() {
        return this.jedis;
    }

    public ShardConfig getConfig() {
        return config;
    }

    public void setConfig(ShardConfig config) {
        this.config = config;
    }

    /**
     * Jedis client properties.
     */
    public static class Jedis {

        /**
         * Jedis pool configuration.
         */
        private Pool pool;

        public Pool getPool() {
            return this.pool;
        }

        public void setPool(Pool pool) {
            this.pool = pool;
        }

    }
}
