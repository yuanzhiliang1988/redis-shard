package com.ttyc.redis.shard.constant;

/**
 * @author yuanzl
 * @date 2021/8/23 3:09 下午
 */
public class ShardConstants {
    /**
     * 哨兵和集群节点分隔符
     */
    public final static String HOST_SPLIT = ",";
    /**
     * 哨兵和集群节点中host和port的分隔符
     */
    public final static String HOST_PORT_SPLIT = ":";
    /**
     * 配置前缀
     */
    public final static String CONFIG_PREFIX = "redis.shard.";
    /**
     * 分片节点配置前缀
     */
    public final static String CONFIG_NODE_PREFIX = "redis.shard.nodes";
    public final static String CONFIG_JEDIS_POOL_PREFIX = "redis.shard.jedis.pool.";
    public final static String CONFIG_SHARD_CONFIG_PREFIX = "redis.shard.config.";
    /**
     * 虚拟节点数
     */
    public final static int VIR_NOTE_NUM = 160;
}
