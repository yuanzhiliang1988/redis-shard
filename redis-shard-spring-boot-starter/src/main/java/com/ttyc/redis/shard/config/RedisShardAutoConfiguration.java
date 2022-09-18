package com.ttyc.redis.shard.config;

import com.ttyc.redis.shard.core.*;
import com.ttyc.redis.shard.listener.ApolloRedisChangeListener;
import com.ttyc.redis.shard.properties.RedisShardProperties;
import com.ttyc.redis.shard.ShardBeanFactory;
import com.ttyc.redis.shard.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * redis分片配置配置 默认自动生成：RedisShardClient和StringRedisShardClient操作客户端
 * 如需要实例化多个分片客户端，每个分片客户端对应不同的分片nodeAddresses地址，则可自定义初始化RedisShardClientConfiguration类
 * 参考example中的RedisShardClientConfig类方法
 * 注：自定义分片节点不支持扩容后数据迁移
 *
 * @author yuanzl
 * @date 2021/08/18
 **/
@Configuration
@EnableConfigurationProperties(RedisShardProperties.class)
@ConditionalOnProperty(value = "redis.shard.enabled",matchIfMissing = true)
@ConditionalOnClass(RedisTemplate.class)
@Slf4j
public class RedisShardAutoConfiguration {
    @Value("${spring.application.name}")
    private String appName;

    @Bean("shardBeanFactory")
    @ConditionalOnMissingBean
    public ShardBeanFactory initShardBeanFactory(){
        return new ShardBeanFactory<>();
    }

    @Bean("shardSpringContextUtils")
    @ConditionalOnMissingBean
    public SpringContextUtils initSpringContextUtils(){
        return new SpringContextUtils();
    }


    @Bean("redisShardClientConfiguration")
    @DependsOn({"shardSpringContextUtils"})
    public RedisShardClientConfiguration initRedisShardClientConfiguration(RedisShardProperties redisProperties,ShardBeanFactory shardBeanFactory){
        RedisShardClientConfiguration redisShardClientConfiguration = new RedisShardClientConfiguration(appName,shardBeanFactory);
        redisShardClientConfiguration.setRedisShardProperties(redisProperties);
        redisShardClientConfiguration.afterPropertiesSet();

        return redisShardClientConfiguration;
    }

    @Bean("redisShardClient")
    public RedisShardClient initRedisShardClient(@Qualifier("redisShardClientConfiguration") RedisShardClientConfiguration redisShardClientConfiguration){

        return redisShardClientConfiguration.getRedisShardClient();
    }

    @Bean("stringRedisShardClient")
    public StringRedisShardClient initStringRedisShardClient(@Qualifier("redisShardClientConfiguration") RedisShardClientConfiguration redisShardClientConfiguration){

        return redisShardClientConfiguration.getStringRedisShardClient();
    }

    @Bean("apolloRedisChangeListener")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "redis.shard.listener.enabled",matchIfMissing = true)
    public ApolloRedisChangeListener initApolloChangeListener(@Qualifier("shardBeanFactory") ShardBeanFactory shardBeanFactory, @Qualifier("redisShardClientConfiguration") RedisShardClientConfiguration redisShardClientConfiguration){
        return new ApolloRedisChangeListener(shardBeanFactory,redisShardClientConfiguration.getSharding());
    }
}