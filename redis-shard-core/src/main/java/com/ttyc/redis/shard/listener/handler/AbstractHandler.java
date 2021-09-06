package com.ttyc.redis.shard.listener.handler;

import com.alibaba.fastjson.JSON;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ttyc.redis.shard.ShardBeanFactory;
import com.ttyc.redis.shard.constant.ShardConstants;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.core.XRedisConnectionFactory;
import com.ttyc.redis.shard.enums.NodeTypeEnum;
import com.ttyc.redis.shard.enums.SerializerTypeEnum;
import com.ttyc.redis.shard.support.Node;
import com.ttyc.redis.shard.support.Pool;
import com.ttyc.redis.shard.support.ShardConfig;
import com.ttyc.redis.shard.support.ShardNode;
import com.ttyc.redis.shard.utils.StringUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yuanzl
 * @date 2021/8/27 10:09 上午
 */
@Slf4j
public abstract class AbstractHandler<T> implements Handler<T> {

    public void doBusiness(Sharding sharding,List<PropertyChangeType> changeTypes, T o, T n){

    }

    public void doBusiness(Sharding sharding,List<PropertyChangeType> changeTypes, List<T> o, List<T> n){

    }

    /**
     * 公共方法：刷新分片节点
     */
    @SneakyThrows
    public void refreshShardNodes(Sharding sharding, Map<Integer, ShardNode> oldNodeMap, Map<Integer, ShardNode> newNodeMap){
        log.info("start refreshShardNodes.....");
        Iterator<Map.Entry<Integer,ShardNode>> iterator = newNodeMap.entrySet().iterator();
        XRedisConnectionFactory xRedisConnectionFactory = new XRedisConnectionFactory();
        Config configService = ConfigService.getAppConfig();
        ShardConfig shardConfig = this.getShardConfig(configService);
        Pool pool = getPool(configService);
        ShardBeanFactory shardBeanFactory = new ShardBeanFactory();
        //设置配置
        if(shardConfig!=null) {
            sharding.setConfig(shardConfig);
        }
        while (iterator.hasNext()){
            Map.Entry<Integer,ShardNode> entry = iterator.next();
            Integer k = entry.getKey();
            ShardNode v = entry.getValue();
            log.info("name:{},newNodesMap:{},index:{},changeType:{},addresses:{},gray:{},doubleWriter:{},transfer:{}",getName(),k,v.getIndex(),v.getChangeType(),v.getAddresses(),v.isGray(),v.isDoubleWriter(),v.isTransfer());
            if(v.getChangeType().equals(PropertyChangeType.DELETED)){
                oldNodeMap.remove(k);
            }else{
                this.refreshOldNodes(k,v,oldNodeMap,xRedisConnectionFactory,pool,shardConfig, shardBeanFactory);
            }
        }

        sharding.cleanShardNode();
        sharding.setShardNodes(oldNodeMap.values().stream().collect(Collectors.toSet()));
        log.info("end refreshShardNodes.....");
    }

    private void refreshOldNodes(Integer k, ShardNode v, Map<Integer, ShardNode> oldNodeMap, XRedisConnectionFactory xRedisConnectionFactory, Pool pool, ShardConfig shardConfig, ShardBeanFactory shardBeanFactory) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Node node = new Node();
        BeanUtils.copyProperties(v,node);
        v.setConnectionFactory(xRedisConnectionFactory.jedisConnectionFactory(NodeTypeEnum.getEnum(v.getNodeType()),node,pool));
        v.setName("SHARD-NODE-"+NodeTypeEnum.getCodeByType(v.getNodeType())+"-"+v.getAddresses());
        if(shardConfig!=null && StringUtils.isNotBlank(shardConfig.getSerializer())){
            //序列化
            this.setSerializer(shardConfig.getSerializer(),v, shardBeanFactory);
        }
        oldNodeMap.compute(k,(ok,ov)->{
            if(ov==null) {
                ov = v;
            }else{
                BeanUtils.copyProperties(v,ov);
            }
            ov.afterPropertiesSet();
            return ov;
        });
    }

    @SneakyThrows
    public void refreshSerializer(Sharding sharding, String serializer){
        log.info("start refreshSerializer.....");
        Map<String, ShardNode> shardNodeMap = sharding.getShardNodeMap();
        Iterator<Map.Entry<String,ShardNode>> iterator = shardNodeMap.entrySet().iterator();
        ShardBeanFactory shardBeanFactory = new ShardBeanFactory();
        if(StringUtils.isBlank(serializer)){//默认jdk
            serializer = SerializerTypeEnum.JDK.getType();
        }

        while (iterator.hasNext()){
            Map.Entry<String,ShardNode> entry = iterator.next();
            ShardNode shardNode = entry.getValue();
            this.setSerializer(serializer,shardNode , shardBeanFactory);
            shardNode.afterPropertiesSet();
            //替换原节点
            sharding.setShardNodesMap(entry.getKey(),shardNode);
        }
        log.info("end refreshSerializer.....");
    }

    @SneakyThrows
    public void refreshPool(Sharding sharding){
        log.info("start refreshPool.....");
        Pool pool = getPool(ConfigService.getAppConfig());

        Map<String, ShardNode> shardNodeMap = sharding.getShardNodeMap();
        Iterator<Map.Entry<String,ShardNode>> iterator = shardNodeMap.entrySet().iterator();
        XRedisConnectionFactory xRedisConnectionFactory = new XRedisConnectionFactory();
        while (iterator.hasNext()){
            Map.Entry<String,ShardNode> entry = iterator.next();
            ShardNode shardNode = entry.getValue();
            Node node = new Node();
            BeanUtils.copyProperties(shardNode,node);
            shardNode.setConnectionFactory(xRedisConnectionFactory.jedisConnectionFactory(NodeTypeEnum.getEnum(shardNode.getNodeType()),node,pool));
            shardNode.afterPropertiesSet();
            //替换原节点
            sharding.setShardNodesMap(entry.getKey(),shardNode);
        }
        log.info("end refreshPool.....");
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
        shardNode.setKeySerializer(shardNode.getStringSerializer());
        shardNode.setHashKeySerializer(shardNode.getStringSerializer());
        shardNode.setValueSerializer(serializer);
        shardNode.setHashValueSerializer(serializer);
    }

    private ShardConfig getShardConfig(Config configService){
        ShardConfig shardConfig = null;
        Set<String> poolConfigSet = configService.getPropertyNames().stream().filter(f->f.startsWith(ShardConstants.CONFIG_SHARD_CONFIG_PREFIX)).collect(Collectors.toSet());
        if(!poolConfigSet.isEmpty()){
            shardConfig = new ShardConfig();
            for(String configKey:poolConfigSet){
                String configFieldKey = StringUtil.convertStr(configKey.contains("[")?StringUtils.substringBetween(configKey,ShardConstants.CONFIG_SHARD_CONFIG_PREFIX,"["):StringUtils.substringAfter(configKey,ShardConstants.CONFIG_SHARD_CONFIG_PREFIX));
                String newValue = configService.getProperty(configKey, null);
                StringUtil.setField(shardConfig,configFieldKey,newValue);
            }
        }
        return shardConfig;
    }

    private Pool getPool(Config configService){
        Set<String> poolConfigSet = configService.getPropertyNames().stream().filter(f->f.startsWith(ShardConstants.CONFIG_JEDIS_POOL_PREFIX)).collect(Collectors.toSet());
        if(!poolConfigSet.isEmpty()){
            Pool pool = new Pool();
            for(String poolConfig:poolConfigSet){
                String newValue = configService.getProperty(poolConfig,null);
                String configFieldKey = StringUtil.convertStr(StringUtils.substringAfter(poolConfig,ShardConstants.CONFIG_JEDIS_POOL_PREFIX));
                StringUtil.setField(pool,configFieldKey,newValue);
            }
            log.info("pool:{}", JSON.toJSONString(pool));

            return pool;
        }

        return null;
    }

    /**
     * 公共方法：迁移
     */
    public void transfer(){
        log.info("start transfer.....");

        log.info("end transfer.....");
    }
}
