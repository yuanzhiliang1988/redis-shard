package com.ttyc.redis.shard.core;

import com.ttyc.redis.shard.constant.ShardConstants;
import com.ttyc.redis.shard.enums.ExceptionMsgEnum;
import com.ttyc.redis.shard.enums.NodeTypeEnum;
import com.ttyc.redis.shard.exception.RedisShardException;
import com.ttyc.redis.shard.support.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.HashSet;

/**
 * @author yuanzl
 * @date 2021/8/23 4:56 下午
 */
@Slf4j
public class XRedisConnectionFactory {
    /**
     * 获取redis连接
     * @param nodeTypeEnum
     * @param node
     * @param pool
     * @return
     */
    public JedisConnectionFactory jedisConnectionFactory(NodeTypeEnum nodeTypeEnum, Node node, com.ttyc.redis.shard.support.Pool pool) {
        JedisConnectionFactory jedisConnectionFactory = null;

        //连接池设置
        JedisClientConfiguration jedisClientConfiguration = jedisClientConfiguration(pool);

        switch (nodeTypeEnum){
            case SENTINEL:
                jedisConnectionFactory = this.sentinelConnectionFactory(node,jedisClientConfiguration);
                break;
            case CLUSTER:
                jedisConnectionFactory = this.clusterConnectionFactory(node,jedisClientConfiguration);
                break;
            default:
                jedisConnectionFactory = this.singleConnectionFactory(node,jedisClientConfiguration);
                break;
        }

        if(jedisConnectionFactory==null){
            log.error("get JedisConnectionFactory fail.");
            throw new RedisShardException(ExceptionMsgEnum.GET_JEDIS_FACTORY_FAIL);
        }

        return jedisConnectionFactory;
    }

    public JedisConnectionFactory singleConnectionFactory(Node node,JedisClientConfiguration jedisClientConfiguration) {
        HashSet<String> hashSet = node.getHostAndPorts();
        String[] hostAndPorts =StringUtils.split(hashSet.stream().findFirst().get(), ShardConstants.HOST_PORT_SPLIT);

        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setHostName(hostAndPorts[0]);
        standaloneConfiguration.setPort(Integer.parseInt(hostAndPorts[1]));
        if (org.apache.commons.lang3.StringUtils.isNotBlank(node.getPassword())) {
            standaloneConfiguration.setPassword(RedisPassword.of(node.getPassword()));
        }
        if (node.getDatabase() != 0) {
            standaloneConfiguration.setDatabase(node.getDatabase());
        }

        return new JedisConnectionFactory(standaloneConfiguration,jedisClientConfiguration);
    }

    public JedisConnectionFactory sentinelConnectionFactory(Node node,JedisClientConfiguration jedisClientConfiguration) {
        RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration(node.getSentinel().getMaster(), node.getHostAndPorts());
        if (org.apache.commons.lang3.StringUtils.isNotBlank(node.getPassword())) {
            sentinelConfiguration.setPassword(RedisPassword.of(node.getPassword()));
        }
        if (node.getDatabase() != 0) {
            sentinelConfiguration.setDatabase(node.getDatabase());
        }

        return new JedisConnectionFactory(sentinelConfiguration,jedisClientConfiguration);
    }

    public JedisConnectionFactory clusterConnectionFactory(Node node,JedisClientConfiguration jedisClientConfiguration) {
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(node.getHostAndPorts());
        if (org.apache.commons.lang3.StringUtils.isNotBlank(node.getPassword())) {
            clusterConfiguration.setPassword(RedisPassword.of(node.getPassword()));
        }

        return new JedisConnectionFactory(clusterConfiguration,jedisClientConfiguration);
    }

    private JedisClientConfiguration jedisClientConfiguration(com.ttyc.redis.shard.support.Pool pool){
        JedisClientConfiguration.JedisClientConfigurationBuilder builder = JedisClientConfiguration.builder();
        if(pool!=null){
            if(pool.getReadTimeout()!=null){
                builder.readTimeout(Duration.ofMillis(pool.getReadTimeout()));
            }
            if(pool.getConnectTimeout()!=null){
                builder.connectTimeout(Duration.ofMillis(pool.getConnectTimeout()));
            }
            builder.usePooling().poolConfig(jedisPoolConfig(pool));
        }

        return builder.build();
    }

    private JedisPoolConfig jedisPoolConfig(com.ttyc.redis.shard.support.Pool pool) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(pool.getMaxTotal());
        config.setMaxIdle(pool.getMaxIdle());
        config.setMinIdle(pool.getMinIdle());
        if (pool.getMaxWait() != null) {
            config.setMaxWaitMillis(pool.getMaxWait());
        }
        if (pool.getTestOnBorrow() != null) {
            config.setTestOnBorrow(pool.getTestOnBorrow());
        }
        if (pool.getTestOnReturn() != null) {
            config.setTestOnReturn(pool.getTestOnReturn());
        }
        if (pool.getTestWhileIdle() != null) {
            config.setTestWhileIdle(pool.getTestWhileIdle());
        }
        return config;
    }
}
