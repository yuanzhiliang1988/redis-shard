package com.ttyc.redis.shard.listener;

import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.ttyc.redis.shard.constant.ShardConstants;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.core.StringRedisShardClient;
import com.ttyc.redis.shard.enums.ConfigInfoEnum;
import com.ttyc.redis.shard.listener.handler.AbstractHandler;
import com.ttyc.redis.shard.listener.handler.Handler;
import com.ttyc.redis.shard.ShardBeanFactory;
import com.ttyc.redis.shard.support.ShardConfig;
import com.ttyc.redis.shard.support.ShardNode;
import com.ttyc.redis.shard.utils.SpringContextUtils;
import com.ttyc.redis.shard.utils.StringUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * apollo配置变更监听
 * @author yuanzl
 * @date 2021/8/26 8:41 下午
 */
@Slf4j
public class ApolloRedisChangeListener {
    private Map<String, Handler> handlerMap;
    private ShardBeanFactory shardBeanFactory;
    private Sharding sharding;

    public ApolloRedisChangeListener(ShardBeanFactory shardBeanFactory, Sharding sharding){
        this.shardBeanFactory = shardBeanFactory;
        this.sharding = sharding;
    }

    @PostConstruct
    public void initListener(){
        List<Handler> handlers = this.shardBeanFactory.getBeansByInterface(AbstractHandler.class);

        if(!CollectionUtils.isEmpty(handlers)){
            handlerMap = new HashMap<>();
            for (Handler handler:handlers) {
                handlerMap.put(handler.getType(),handler);
            }
        }
        log.info("ApolloRedisChangeListener init success handler size:{}",handlers.size());
    }

    @ApolloConfigChangeListener(interestedKeyPrefixes = ShardConstants.CONFIG_PREFIX)
    private void configChangeListener(ConfigChangeEvent changeEvent) {
        Iterator iterator = changeEvent.changedKeys().iterator();
        //分片配置
        ShardConfig oldConfig = this.sharding.getConfig();
        ShardConfig newConfig = new ShardConfig();
        BeanUtils.copyProperties(oldConfig,newConfig);
        Map<String,MutableTriple<ArrayList<PropertyChangeType>,Object,Object>> handlerTypes = new HashMap<>();
        //新分片节点
        Map<Integer,ShardNode> newNodesMap = new HashMap<>();
        //原分片节点
        Map<Integer, ShardNode> oldNodeMap = sharding.getShardNodeList().stream().collect(Collectors.toMap(p -> p.getIndex(), (p) -> p));

        while(iterator.hasNext()) {
            String changedKey = (String)iterator.next();
            ConfigChange configChange = changeEvent.getChange(changedKey);
            String propertyName = configChange.getPropertyName();
            String newValue = configChange.getNewValue();
            String oldValue = configChange.getOldValue();
            PropertyChangeType changeType = configChange.getChangeType();
            log.info("apollo config change - key: {}, oldValue: {}, newValue: {}, changeType: {}", propertyName, oldValue, newValue, changeType);

            if(!changedKey.startsWith(ShardConstants.CONFIG_PREFIX)){
                continue;
            }

            if(changedKey.startsWith(ShardConstants.CONFIG_NODE_PREFIX)){
                String nodeIndexStr = StringUtils.substringBetween(changedKey,"[","]");
                if(StringUtils.isBlank(nodeIndexStr)){
                    continue;
                }
                //构建变更节点
                this.buildChangeNodes(nodeIndexStr,changedKey,newValue,newNodesMap,oldNodeMap,changeType);
            }else if(changedKey.contains(ShardConstants.CONFIG_SHARD_CONFIG_PREFIX)){
                if(changedKey.contains(ConfigInfoEnum.SERIALIZER.getCode())){
                    newConfig.setSerializer(newValue);
                    handlerTypes.put(ConfigInfoEnum.SERIALIZER.getType(),MutableTriple.of(new ArrayList(1){{add(changeType);}},oldConfig,newConfig));
                }else if(changedKey.contains(ConfigInfoEnum.KEY_REGEX.getCode())){
                    this.buildNewConfig(changeType,newConfig.getKeyRegex(),oldValue,newValue);
                    handlerTypes.put(ConfigInfoEnum.KEY_REGEX.getType(),MutableTriple.of(new ArrayList(1){{add(changeType);}},oldConfig,newConfig));
                }
            }else if(changedKey.contains(ShardConstants.CONFIG_JEDIS_POOL_PREFIX)){
                handlerTypes.put(ConfigInfoEnum.POOL.getType(),MutableTriple.of(new ArrayList(0),null,null));
            }else if(changedKey.contains(ShardConstants.CONFIG_SHARD_TRANSFER_PREFIX)){
                handlerTypes.put(ConfigInfoEnum.TRANSFER.getType(),MutableTriple.of(new ArrayList(0),null,null));
            }
        }

        if(!newNodesMap.isEmpty()){
            String handlerType = changeEvent.changedKeys().stream().anyMatch(p->p.endsWith(ConfigInfoEnum.ADDRESSES.getCode()))?ConfigInfoEnum.ADDRESSES.getType():ConfigInfoEnum.GRAY.getType();
            handlerTypes.put(handlerType,MutableTriple.of(null,oldNodeMap,newNodesMap));
        }

        if(!handlerTypes.isEmpty()){
            //删除StringRedisShardClient中缓存的值
            Object stringRedisShardClientObj = SpringContextUtils.getBean("stringRedisShardClient");
            if(stringRedisShardClientObj!=null){
                StringRedisShardClient stringRedisShardClient = (StringRedisShardClient)stringRedisShardClientObj;
                stringRedisShardClient.cleanStringShardNodeMap();
            }
            handlerTypes.forEach((k,v)->{
                this.routerHandler(k,v);
            });
        }
    }

    private void buildChangeNodes(String nodeIndexStr,String changedKey,String newValue,Map<Integer,ShardNode> newNodesMap,Map<Integer, ShardNode> oldNodeMap,PropertyChangeType changeType){
        Integer nodeIndex = Integer.parseInt(nodeIndexStr);
        String configFieldKey = StringUtil.convertStr(StringUtils.substringAfterLast(changedKey,"]."));
        newNodesMap.compute(nodeIndex,(k,v)->{
            if(v==null){
                if(oldNodeMap.containsKey(nodeIndex)){
                    v = oldNodeMap.get(nodeIndex);
                    v.setChangeType(PropertyChangeType.MODIFIED);
                }else{
                    v = new ShardNode();
                    //默认新增
                    v.setChangeType(PropertyChangeType.ADDED);
                }
                v.setIndex(nodeIndex);
            }

            if(changeType.equals(PropertyChangeType.DELETED) && changedKey.contains(ConfigInfoEnum.ADDRESSES.getCode())){
                v.setChangeType(PropertyChangeType.DELETED);
            }
            if(changeType.equals(PropertyChangeType.DELETED)){
                StringUtil.setFieldNull(v,configFieldKey);
            }else{
                StringUtil.setField(v,configFieldKey,newValue);
            }

            return v;
        });
    }

    private void routerHandler(String handlerType,MutableTriple<ArrayList<PropertyChangeType>,Object,Object> mutableTriple){
        if(StringUtils.isBlank(handlerType) || !handlerMap.containsKey(handlerType)){
            log.info("{} is null or {} is not exists in handlerMap",handlerType,handlerType);
            return;
        }
        handlerMap.get(handlerType).doBusiness(this.sharding,mutableTriple.getLeft(),mutableTriple.getMiddle(),mutableTriple.getRight());
    }

    private void buildNewConfig(PropertyChangeType changeType,List<String> keyRegexList,String oldValue,String newValue){
        if(changeType.equals(PropertyChangeType.MODIFIED)){
            keyRegexList.remove(oldValue);
            keyRegexList.add(newValue);
        }else if(changeType.equals(PropertyChangeType.DELETED)){
            keyRegexList.remove(oldValue);
        }else{
            keyRegexList.add(newValue);
        }
    }

    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("time:"+Duration.ofMillis(2).toMinutes());
        System.out.println(StringUtils.substringBetween("redis.shard.nodes[1].password","[","]"));
        System.out.println(StringUtils.substringAfterLast("redis.shard.nodes[1].password","]."));
        ShardNode shardNode = new ShardNode();
        Field field = shardNode.getClass().getDeclaredField("gray");
        System.out.println(field.getType()+","+field.getType());
        field.setAccessible(true);
        field.set(shardNode,false);
        ShardBeanFactory shardBeanFactory = new ShardBeanFactory();
        List<Handler> handlers = shardBeanFactory.getBeansByInterface(AbstractHandler.class);
        for (Handler handler:handlers) {
            System.out.println("handler name:"+handler.getName());
        }
    }
}
