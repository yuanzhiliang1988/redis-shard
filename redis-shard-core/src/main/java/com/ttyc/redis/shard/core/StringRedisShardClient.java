package com.ttyc.redis.shard.core;

import com.ttyc.redis.shard.serializer.StringRedisSerializer;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * String类型Value操客户端
 * @author yuanzl
 * @date 2021/9/8 4:18 下午
 */
@Slf4j
public class StringRedisShardClient extends RedisShardClient<String>{
    private ConcurrentHashMap<Integer,ShardNode> stringShardNodeMap = new ConcurrentHashMap();

    public StringRedisShardClient(String appName,Sharding sharding) {
        super(appName,sharding);
    }

    @Override
    public ShardNode getShardNode(String key){
        return this.getStringShardNode(super.getShardNode(key));
    }

    @Override
    public ShardNode getNextShardNode(String key){
        return this.getStringShardNode(super.getNextShardNode(key));
    }

    @Override
    public List<ShardNode> getAllShardNodes(){
        List<ShardNode> shardNodes = super.getAllShardNodes();
        shardNodes.stream().peek(node -> {
            this.setSerializer(node);
        }).collect(Collectors.toList());
        return shardNodes;
    }

    @Override
    public ShardNode getShardNode(Integer index){
        return this.getStringShardNode(super.getShardNode(index));
    }

    public ShardNode getStringShardNode(ShardNode node){
        ShardNode shardNode= stringShardNodeMap.computeIfAbsent(node.getIndex(),val->{
            ShardNode stringNode = new ShardNode();
            BeanUtils.copyProperties(node,stringNode);
            this.setSerializer(stringNode);

            return stringNode;
        });

        return shardNode;
    }

    private void setSerializer(ShardNode shardNode){
        RedisSerializer redisSerializer = new StringRedisSerializer();
        shardNode.setValueSerializer(redisSerializer);
        shardNode.setHashValueSerializer(redisSerializer);

        shardNode.afterPropertiesSet();
    }

    /**
     * 清空StringShardNode
     */
    public void cleanStringShardNodeMap(){
        stringShardNodeMap.clear();
    }

    /**
     * 根据分片索引删除StringShardNode
     * @param index
     */
    public void cleanStringShardNodeMap(Integer index){
        stringShardNodeMap.remove(index);
    }
}
