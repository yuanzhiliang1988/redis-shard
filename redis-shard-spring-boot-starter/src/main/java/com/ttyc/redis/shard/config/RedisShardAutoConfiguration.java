package com.ttyc.redis.shard.config;

import com.ttyc.redis.shard.core.*;
import com.ttyc.redis.shard.enums.ExceptionMsgEnum;
import com.ttyc.redis.shard.enums.NodeTypeEnum;
import com.ttyc.redis.shard.enums.SerializerTypeEnum;
import com.ttyc.redis.shard.exception.RedisShardException;
import com.ttyc.redis.shard.listener.ApolloChangeListener;
import com.ttyc.redis.shard.properties.RedisShardProperties;
import com.ttyc.redis.shard.ShardBeanFactory;
import com.ttyc.redis.shard.support.Node;
import com.ttyc.redis.shard.support.Pool;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * redis 配置
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

    /**
     * redis分片节点初始化
     * @param redisProperties
     * @return
     * @throws Exception
     */
    @Bean("redisShardTemplateSet")
    public Set<ShardNode> redisShardTemplateSet(RedisShardProperties redisProperties,@Qualifier("shardBeanFactory") ShardBeanFactory shardBeanFactory) throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        if(StringUtils.isBlank(appName)){
            throw new RedisShardException(ExceptionMsgEnum.APP_NAME_NULL);
        }
        Set<ShardNode> templateSet = new HashSet<>();
        LinkedList<Node> nodes = redisProperties.getNodes();

        if(CollectionUtils.isEmpty(nodes)){
            log.error("redis.shard.nodes is not empty");
            return null;
        }
        Pool pool = redisProperties.getJedis().getPool();
        String serializerType = redisProperties.getConfig()==null?null:redisProperties.getConfig().getSerializer();
        Integer index = 0;
        XRedisConnectionFactory xRedisConnectionFactory = new XRedisConnectionFactory();
        for(Node node:nodes){
            ShardNode shardNode = this.createShardNode(node,pool,index,xRedisConnectionFactory);
            this.setSerializer(serializerType,shardNode,shardBeanFactory);
            shardNode.afterPropertiesSet();

            templateSet.add(shardNode);
            index++;
        }
        return templateSet;
    }

    @Bean("sharding")
    @ConditionalOnBean(name = "redisShardTemplateSet")
    public Sharding sharding(@Qualifier("redisShardTemplateSet") Set<ShardNode> redisShardTemplateSet,RedisShardProperties redisProperties){
        return new Sharding(redisShardTemplateSet,redisProperties.getConfig());
    }

    @Bean("redisShardClient")
    public RedisShardClient initRedisShardClient(){
        return new RedisShardClient(appName);
    }

    @Bean
    @ConditionalOnBean(name = "sharding")
    @ConditionalOnProperty(value = "redis.shard.listener.enabled",matchIfMissing = true)
    public ApolloChangeListener initApolloChangeListener(@Qualifier("shardBeanFactory") ShardBeanFactory shardBeanFactory, @Qualifier("sharding")Sharding sharding){
        return new ApolloChangeListener(shardBeanFactory,sharding);
    }

    @Bean("shardBeanFactory")
    public ShardBeanFactory init(){
        return new ShardBeanFactory<>();
    }

    /**
     * 设置redis序列化方式
     * @param serializerType
     * @param shardNode
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    private void setSerializer(String serializerType, ShardNode shardNode, ShardBeanFactory shardBeanFactory) throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        if(StringUtils.isBlank(serializerType)){
            return;
        }
        RedisSerializer serializer = SerializerTypeEnum.getSerializer(serializerType,shardBeanFactory);
        //key默认使用string序列化
        shardNode.setKeySerializer(shardNode.getStringSerializer());
        shardNode.setHashKeySerializer(shardNode.getStringSerializer());
        shardNode.setValueSerializer(serializer);
        shardNode.setHashValueSerializer(serializer);
    }

    /**
     * 创建分片节点
     * @param node
     * @param pool
     * @return
     */
    private ShardNode createShardNode(Node node, Pool pool,Integer index,XRedisConnectionFactory xRedisConnectionFactory) {
        ShardNode shardNode = new ShardNode();
        shardNode.setConnectionFactory(xRedisConnectionFactory.jedisConnectionFactory(NodeTypeEnum.getEnum(node.getType()),node,pool));
        shardNode.setName("SHARD-NODE-"+NodeTypeEnum.getCodeByType(node.getType())+"-"+node.getAddresses());
        shardNode.setAddresses(node.getAddresses());
        shardNode.setNodeType(node.getType());
        shardNode.setGray(node.isGray());
        shardNode.setDoubleWriter(node.isDoubleWriter());
        shardNode.setTransfer(node.isTransfer());
        shardNode.setIndex(index);

        return shardNode;
    }
}