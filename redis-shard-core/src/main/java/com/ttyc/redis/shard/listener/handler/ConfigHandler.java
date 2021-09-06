package com.ttyc.redis.shard.listener.handler;

import com.alibaba.fastjson.JSON;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.enums.ConfigInfoEnum;
import com.ttyc.redis.shard.support.ShardConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author yuanzl
 * @date 2021/8/26 9:04 下午
 */
@Slf4j
public class ConfigHandler extends AbstractHandler<ShardConfig>{
    @Override
    public String getType() {
        return ConfigInfoEnum.KEY_REGEX.getType();
    }

    @Override
    public String getName() {
        return "ConfigHandler";
    }

    @Override
    public void doBusiness(Sharding sharding,List<PropertyChangeType> changeTypes,ShardConfig oldConfig,ShardConfig newConfig) {
        //设置配置
        log.info("{},{}",getName(), newConfig.getKeyRegex()==null?null:JSON.toJSONString(oldConfig.getKeyRegex()));
        sharding.setConfig(oldConfig);
        //序列化
        if((StringUtils.isBlank(oldConfig.getSerializer()) && StringUtils.isNotBlank(newConfig.getSerializer()))
            || (StringUtils.isNotBlank(oldConfig.getSerializer()) && StringUtils.isBlank(newConfig.getSerializer()) )
                || !oldConfig.getSerializer().equals(newConfig.getSerializer())){
            this.refreshSerializer(sharding, newConfig.getSerializer());
        }
    }
}
