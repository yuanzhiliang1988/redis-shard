package com.ttyc.redis.shard.listener.handler;

import com.alibaba.fastjson.JSON;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.enums.ConfigInfoEnum;
import com.ttyc.redis.shard.support.Pool;
import com.ttyc.redis.shard.support.ShardConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author yuanzl
 * @date 2021/8/26 9:04 下午
 */
@Slf4j
public class PoolHandler extends AbstractHandler<Pool>{
    @Override
    public String getType() {
        return ConfigInfoEnum.POOL.getType();
    }

    @Override
    public String getName() {
        return "PoolHandler";
    }

    @Override
    public void doBusiness(Sharding sharding,List<PropertyChangeType> changeTypes,Pool oldPool,Pool newPool) {
        //设置配置
        this.refreshPool(sharding);
    }
}
