package com.ttyc.redis.shard.properties;

import com.ttyc.redis.shard.support.Node;
import com.ttyc.redis.shard.support.Pool;
import com.ttyc.redis.shard.support.ShardConfig;
import com.ttyc.redis.shard.support.Transfer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

/**
 * redis 配置文件
 * @author yanghaiyu
 * @date 2020/08/24
 **/
@ConfigurationProperties(prefix = "redis.shard")
public class RedisShardProperties {
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
    private final Jedis jedis = new Jedis();
    /**
     * lettuce
     */
    private final Lettuce lettuce = new Lettuce();
    /**
     * 迁移信息
     */
    private List<Transfer> transfers;

    public LinkedList<Node> getNodes() {
        return nodes;
    }

    public void setNodes(LinkedList<Node> nodes) {
        this.nodes = nodes;
    }

    public Jedis getJedis() {
        return this.jedis;
    }

    public Lettuce getLettuce() {
        return this.lettuce;
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

    /**
     * Lettuce client properties.
     */
    public static class Lettuce {

        /**
         * Shutdown timeout.
         */
        private Duration shutdownTimeout = Duration.ofMillis(100);

        /**
         * Lettuce pool configuration.
         */
        private Pool pool;

        public Duration getShutdownTimeout() {
            return this.shutdownTimeout;
        }

        public void setShutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
        }

        public Pool getPool() {
            return this.pool;
        }

        public void setPool(Pool pool) {
            this.pool = pool;
        }
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public void setTransfers(List<Transfer> transfers) {
        this.transfers = transfers;
    }
}
