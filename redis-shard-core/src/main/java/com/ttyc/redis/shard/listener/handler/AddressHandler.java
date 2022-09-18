package com.ttyc.redis.shard.listener.handler;

import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.enums.ConfigInfoEnum;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yuanzl
 * @date 2021/8/26 9:04 下午
 */
@Slf4j
public class AddressHandler extends AbstractHandler<Object>{

    @Override
    public String getType() {
        return ConfigInfoEnum.ADDRESSES.getType();
    }

    @Override
    public String getName() {
        return "AddressHandler";
    }

    @Override
    public void doBusiness(Sharding sharding,List<PropertyChangeType> changeTypes,Object oldNodes, Object newNodes) {
        log.info("name:{}",getName());
        Map<Integer,ShardNode> newNodesMap = (Map<Integer,ShardNode>) newNodes;
        Map<Integer,ShardNode> oldNodeMap = (Map<Integer,ShardNode>) oldNodes;
        //刷新节点
        this.refreshShardNodes(sharding,oldNodeMap,newNodesMap);
        //迁移
        this.transfer(sharding);
    }
}
