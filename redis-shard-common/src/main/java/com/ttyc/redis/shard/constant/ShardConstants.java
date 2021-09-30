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
    public final static String CONFIG_SHARD_TRANSFER_PREFIX = "redis.shard.transfers";
    /**
     * 虚拟节点数
     */
    public final static int VIR_NOTE_NUM = 160;
    /**
     * redis应用名与key分隔符
     */
    public final static String REDIS_KEY_SPLIT = "@@";
    /**
     * 迁移的keys
     */
    public final static String REDIS_TRANSFER_KEYS = "transfer_keys";
    /**
     * 迁移信息
     */
    public final static String REDIS_TRANSFER_INFO = "transfer_info";
    /**
     * 被迁移节点索引后ZSET score补0
     */
    public final static String REDIS_TRANSFER_KEYS_SCORE_FILL = "000000";
}
