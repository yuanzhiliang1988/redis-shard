package com.ttyc.redis.shard.config;

import com.ttyc.redis.shard.ShardBeanFactory;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.core.StringRedisShardClient;
import com.ttyc.redis.shard.properties.RedisShardProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;

/**
 * demo：多分片实例配置类
 * @author yuanzl
 * @date 2021/10/12 7:18 下午
 */
@Configuration
@EnableConfigurationProperties({MyRedisShardProperties.class,My2RedisShardProperties.class})
public class RedisShardClientConfig {

    @Bean("myRedisShardClient1Config")
    @DependsOn({"shardSpringContextUtils"})
    public RedisShardClientConfiguration initMyRedisShardClient1Config(MyRedisShardProperties myRedisShardProperties){
        RedisShardClientConfiguration redisShardClientConfiguration = new RedisShardClientConfiguration("my1");
        RedisShardProperties redisShardPropertie = new RedisShardProperties();
        BeanUtils.copyProperties(myRedisShardProperties,redisShardPropertie);
        redisShardClientConfiguration.setRedisShardProperties(redisShardPropertie);
        redisShardClientConfiguration.afterPropertiesSet();

        return redisShardClientConfiguration;
    }

    @Bean("myRedisShardClient1")
    public RedisShardClient initMyRedisShardClient1(@Qualifier("myRedisShardClient1Config") RedisShardClientConfiguration myRedisShardClient1Config){
        return myRedisShardClient1Config.getRedisShardClient();
    }

    @Bean("myStringRedisShardClient1")
    public StringRedisShardClient initMyStringRedisShardClient1(@Qualifier("myRedisShardClient1Config")RedisShardClientConfiguration myRedisShardClient1Config){
        return myRedisShardClient1Config.getStringRedisShardClient();
    }

    @Bean("myRedisShardClient2Config")
    public RedisShardClientConfiguration initMyRedisShardClient2Config(My2RedisShardProperties my2RedisShardProperties, ShardBeanFactory shardBeanFactory){
        RedisShardClientConfiguration redisShardClientConfiguration = new RedisShardClientConfiguration("my2",shardBeanFactory);
        RedisShardProperties redisShardPropertie = new RedisShardProperties();
        BeanUtils.copyProperties(my2RedisShardProperties,redisShardPropertie);
        redisShardClientConfiguration.setRedisShardProperties(redisShardPropertie);
        redisShardClientConfiguration.afterPropertiesSet();

        return redisShardClientConfiguration;
    }

    @Bean("myRedisShardClient2")
    public RedisShardClient initMyRedisShardClient2(@Qualifier("myRedisShardClient2Config")RedisShardClientConfiguration myRedisShardClient2Config){
        return myRedisShardClient2Config.getRedisShardClient();
    }

    @Bean("myStringRedisShardClient2")
    public StringRedisShardClient initMyStringRedisShardClient2(@Qualifier("myRedisShardClient2Config")RedisShardClientConfiguration myRedisShardClient2Config){
        return myRedisShardClient2Config.getStringRedisShardClient();
    }
}
