package com.ttyc.redis.shard.config;

import com.alibaba.fastjson.JSON;
import com.ttyc.redis.shard.ShardBeanFactory;
import com.ttyc.redis.shard.core.*;
import com.ttyc.redis.shard.enums.ExceptionMsgEnum;
import com.ttyc.redis.shard.exception.RedisShardException;
import com.ttyc.redis.shard.properties.RedisShardProperties;
import com.ttyc.redis.shard.support.Node;
import com.ttyc.redis.shard.support.Pool;
import com.ttyc.redis.shard.support.ShardNode;
import com.ttyc.redis.shard.utils.SpringContextUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

/**
 * redis分片客户端配置类，可支持多RedisShardClient自定义实例
 * @author yuanzl
 * @date 2021/08/18
 **/
@Slf4j
public class RedisShardClientConfiguration {
    private String appName;
    private RedisShardProperties redisShardProperties;
    private Sharding sharding;
    private ShardBeanFactory shardBeanFactory;
    /**
     * shardBeanFactory不需要自己注入，使用组件默认的即可
     * 使用方法：
     *     @Bean("myRedisShardClient2Config")
     *     public RedisShardClientConfiguration initMyRedisShardClient2Config(My2RedisShardProperties my2RedisShardProperties, ShardBeanFactory shardBeanFactory){
     *         RedisShardClientConfiguration redisShardClientConfiguration = new RedisShardClientConfiguration("${spring.application.name}",shardBeanFactory);
     *         RedisShardProperties redisShardPropertie = new RedisShardProperties();
     *         BeanUtils.copyProperties(my2RedisShardProperties,redisShardPropertie);
     *         redisShardClientConfiguration.setRedisShardProperties(redisShardPropertie);
     *         redisShardClientConfiguration.afterPropertiesSet();
     *
     *         return redisShardClientConfiguration;
     *     }
     *
     * @param appName 项目名
     * @param shardBeanFactory
     */
    public RedisShardClientConfiguration(String appName,ShardBeanFactory shardBeanFactory){
        this.appName = appName;
        this.shardBeanFactory = shardBeanFactory;
    }

    /**
     * 需要依赖SpringContextUtils的提前注入，否则会无法初始化ApplicationContext值
     * 使用方法：
     * @Bean("myRedisShardClient1Config")
     * @DependsOn({"shardSpringContextUtils"})
     * public RedisShardClientConfiguration initMyRedisShardClient1Config(MyRedisShardProperties myRedisShardProperties){
     *      略。。。。。
     * }
     * @param appName 项目名
     */
    public RedisShardClientConfiguration(String appName){
        this.appName = appName;
        this.shardBeanFactory = (ShardBeanFactory) SpringContextUtils.getBean("shardBeanFactory");
    }

    public void setRedisShardProperties(RedisShardProperties redisShardProperties) {
        this.redisShardProperties = redisShardProperties;
    }

    public void afterPropertiesSet(){
        sharding = this.createSharding();
    }

    public Sharding getSharding() {
        return sharding;
    }

    /**
     * 创建RedisShardClient对象
     */
    public RedisShardClient getRedisShardClient(){
        RedisShardClient redisShardClient = new RedisShardClient(appName,sharding);

        return redisShardClient;
    }

    /**
     * 获取StringRedisShardClient对象
     */
    public StringRedisShardClient getStringRedisShardClient(){
        StringRedisShardClient redisShardClient = new StringRedisShardClient(appName,sharding);

        return redisShardClient;
    }

    private Sharding createSharding() {
        Set<ShardNode> shardNodeSet = this.createShardNode(redisShardProperties);
        return new Sharding(shardNodeSet,redisShardProperties.getConfig());
    }

    @SneakyThrows
    private Set<ShardNode> createShardNode(RedisShardProperties redisShardProperties) {
        if(StringUtils.isBlank(appName)){
            throw new RedisShardException(ExceptionMsgEnum.APP_NAME_NULL);
        }
        if(Objects.isNull(shardBeanFactory)){
            throw new RedisShardException(ExceptionMsgEnum.APP_NAME_NULL);
        }
        Set<ShardNode> templateSet = new HashSet<>();
        LinkedList<Node> nodes = redisShardProperties.getNodes();
        log.info("nodes:{}", JSON.toJSONString(nodes));
        if(CollectionUtils.isEmpty(nodes)){
            log.error("redis.shard.nodes is not empty");
            return null;
        }
        Pool pool = redisShardProperties.getJedis().getPool();
        String serializerType = redisShardProperties.getConfig()==null?null:redisShardProperties.getConfig().getSerializer();
        Integer index = 0;
        XRedisConnectionFactory xRedisConnectionFactory = new XRedisConnectionFactory();
        ShardNodeFactory shardNodeFactory = new ShardNodeFactory();
        for(Node node:nodes){
            ShardNode shardNode = shardNodeFactory.createShardNode(node,pool,index,xRedisConnectionFactory);
            shardNodeFactory.setSerializer(serializerType,shardNode,shardBeanFactory);
            shardNode.afterPropertiesSet();

            templateSet.add(shardNode);
            index++;
        }
        return templateSet;
    }
}