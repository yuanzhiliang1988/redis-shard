package com.ttyc.redis.shard.listener.handler;

import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.enums.ConfigInfoEnum;
import com.ttyc.redis.shard.support.Transfer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author yuanzl
 * @date 2021/8/26 9:04 下午
 */
@Slf4j
public class TransferHandler extends AbstractHandler<Transfer>{
    @Override
    public String getType() {
        return ConfigInfoEnum.TRANSFER.getType();
    }

    @Override
    public String getName() {
        return "TransferHandler";
    }

    @Override
    public void doBusiness(Sharding sharding,List<PropertyChangeType> changeTypes,Transfer oldTran,Transfer newTran) {
        //设置配置
        this.transfer(sharding);
    }
}
