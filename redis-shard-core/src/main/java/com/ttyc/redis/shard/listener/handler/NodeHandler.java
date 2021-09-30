package com.ttyc.redis.shard.listener.handler;

import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.enums.ConfigInfoEnum;
import com.ttyc.redis.shard.support.ShardNode;

import java.util.List;

/**
 * @author yuanzl
 * @date 2021/8/26 9:04 下午
 */
public class NodeHandler extends AbstractHandler<Object> {

    @Override
    public String getType() {
        return ConfigInfoEnum.GRAY.getType();
    }

    @Override
    public String getName() {
        return "NodeHandler";
    }

    @Override
    public void doBusiness(Sharding sharding, List<PropertyChangeType> changeTypes, Object oldNodes,Object newNodes) {
        java.util.Map<Integer, ShardNode> newNodesMap = (java.util.Map<Integer,ShardNode>) newNodes;
        java.util.Map<Integer,ShardNode> oldNodeMap = (java.util.Map<Integer,ShardNode>) oldNodes;
        //刷新节点
        this.refreshShardNodes(sharding,oldNodeMap,newNodesMap);
    }
}
