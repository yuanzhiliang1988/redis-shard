package com.ttyc.redis.shard.core;

//import com.perf.redis.PerfRedisConnectionFactory;
import com.ttyc.redis.shard.ShardBeanFactory;
import com.ttyc.redis.shard.enums.NodeTypeEnum;
import com.ttyc.redis.shard.enums.SerializerTypeEnum;
import com.ttyc.redis.shard.support.Node;
import com.ttyc.redis.shard.support.Pool;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.InvocationTargetException;

/**
 * @author yuanzl
 * @date 2021/8/23 4:56 下午
 */
@Slf4j
public class ShardNodeFactory {
    /**
     * 创建分片节点
     * @param node
     * @param pool
     * @return
     */
    public ShardNode createShardNode(Node node, Pool pool, Integer index, XRedisConnectionFactory xRedisConnectionFactory) {
        ShardNode shardNode = new ShardNode();
        //获取connection对象
        JedisConnectionFactory jedisConnectionFactory = xRedisConnectionFactory.jedisConnectionFactory(NodeTypeEnum.getEnum(node.getType()),node,pool);
        //接入perf监控组件
//        PerfRedisConnectionFactory perfRedisConnectionFactory = new PerfRedisConnectionFactory(jedisConnectionFactory);
        shardNode.setConnectionFactory(jedisConnectionFactory);
        shardNode.setName("SHARD-NODE-"+NodeTypeEnum.getCodeByType(node.getType())+"-"+node.getAddresses());
        shardNode.setAddresses(node.getAddresses());
        shardNode.setType(node.getType());
        shardNode.setDatabase(node.getDatabase());
        shardNode.setSsl(node.isSsl());
        shardNode.setGray(node.isGray());
        shardNode.setDoubleWriter(node.isDoubleWriter());
        shardNode.setIndex(index);
        if (StringUtils.isNotBlank(node.getPassword())) {
            shardNode.setPassword(node.getPassword());
        }

        return shardNode;
    }

    /**
     * 设置redis序列化方式
     * @param serializerType
     * @param shardNode
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public void setSerializer(String serializerType, ShardNode shardNode, ShardBeanFactory shardBeanFactory) throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        //key默认使用string序列化
        shardNode.setKeySerializer(shardNode.getStringSerializer());
        shardNode.setHashKeySerializer(shardNode.getStringSerializer());
        if(StringUtils.isBlank(serializerType)){
            return;
        }
        RedisSerializer serializer = SerializerTypeEnum.getSerializer(serializerType,shardBeanFactory);
        shardNode.setValueSerializer(serializer);
        shardNode.setHashValueSerializer(serializer);
        shardNode.setDefaultSerializer(serializer);
    }
}
