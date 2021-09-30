package com.ttyc.redis.shard.listener.handler;

import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ttyc.redis.shard.core.Sharding;

import java.util.List;

/**
 * @author yuanzl
 * @date 2021/8/26 8:59 下午
 */
public interface Handler<T> {
    /**
     * 类型
     * @return
     */
    String getType();

    String getName();

    /**
     * 业务处理
     * @param changeTypes
     * @param o 老数据
     * @param n 新数据
     */
    void doBusiness(Sharding sharding,List<PropertyChangeType> changeTypes, T o, T n);
    /**
     * 业务处理
     * @param changeTypes
     * @param o 老数据
     * @param n 新数据
     */
    void doBusiness(Sharding sharding,List<PropertyChangeType> changeTypes, List<T> o, List<T> n);
}
