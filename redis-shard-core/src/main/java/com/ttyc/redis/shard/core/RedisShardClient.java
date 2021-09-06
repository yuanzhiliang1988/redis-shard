package com.ttyc.redis.shard.core;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ttyc.redis.shard.enums.ExceptionMsgEnum;
import com.ttyc.redis.shard.exception.RedisShardException;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yuanzl
 * @date 2021/8/24 11:15 上午
 */
@Slf4j
public class RedisShardClient<T> {
    @Resource
    private Sharding sharding;
    /**
     * key的前缀，默认为服务应用名
     */
    private static String keyPrefix;

    public RedisShardClient(){

    }

    public RedisShardClient(String appName){
        keyPrefix = appName+"_";
    }

    /**
     * 根据key获取分片节点，业务自定义使用原生方法，为了数据好迁移，建议自定义方法加上前缀
     * @param key
     * @return
     */
    public ShardNode getShardNode(String key){
        ShardNode node = sharding.getShardNode(key);
        return node;
    }

    /**
     * 根据key获取下一分片节点，业务自定义使用原生方法(用途在节点灰度时，获取灰度节点的下一个非灰度节点，灰度节点不对外提供服务)
     * @param key
     * @return
     */
    public ShardNode getNextShardNode(String key){
        ShardNode node = sharding.getNextShardNode(key);
        return node;
    }

    public List<ShardNode> getAllShardNodes(){
        return new ArrayList<>(sharding.getShardNodeMap().values());
    }

    public ShardNode getShardNode(Integer index){
        return sharding.getShardNodeByIndex(index);
    }

    /**
     * 获取key的前缀
     * @return
     */
    public String getKeyPrefix(){
        return keyPrefix;
    }

    /**
     * 获取分片key(已拼上前缀)
     * @return
     */
    public String getShardKey(String key){
        return key.startsWith(getKeyPrefix())?key:getKeyPrefix()+key;
    }
    
    //=============================common============================
    /**
     * 指定缓存失效时间
     * @param key 键
     * @param time 时间(秒)
     * @return
     */
    public boolean expire(String key,long time){
        try {
            if(time>0){
                ShardNode shardNode = this.getShardNode(key);
                if(shardNode.isGray() || shardNode.isDoubleWriter()){
                    shardNode.getNext().expire(getShardKey(key), time, TimeUnit.SECONDS);
                }
                shardNode.expire(getShardKey(key), time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key 获取过期时间
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key){
        ShardNode shardNode = this.getShardNode(key);
        if(shardNode.isGray()){
            return shardNode.getNext().getExpire(getShardKey(key),TimeUnit.SECONDS);
        }
        return shardNode.getExpire(getShardKey(key),TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key){
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray()){
                return shardNode.getNext().hasKey(getShardKey(key));
            }
            return shardNode.hasKey(getShardKey(key));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     * @param key
     */
    public void del(String key){
        if(key!=null){
            ShardNode shardNode = this.getShardNode(key);
            shardNode.delete(getShardKey(key));
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().delete(getShardKey(key));
            }
        }
    }

    /**
     * 删除缓存
     * @param keys 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(List<String> keys){
        if(CollectionUtils.isEmpty(keys)){
            return;
        }

        //删除keys
        getShardNodes(keys).forEach((k, v)->{
            v = v.stream().map(p->getShardKey(p)).collect(Collectors.toList());
            long dels = k.delete(v);
            log.info("del size:{}",dels);
            if(k.isGray() || k.isDoubleWriter()){
                k.getNext().delete(v);
            }
        });
    }

    /**
     * 获取key的数据类型
     * @param key
     * @return
     */
    public DataType getKeyType(String key) {
        ShardNode shardNode = this.getShardNode(key);

        if(shardNode.isGray()){
            return shardNode.getNext().type(getShardKey(key));
        }
        return shardNode.type(getShardKey(key));
    }

    /**
     * 批量key获取对应分片，性能低，不建议使用，如果可接受性能问题可使用
     * @param keys
     * @return
     */
    public Map<ShardNode,List<String>> getShardNodes(List<String> keys){
        Map<ShardNode,List<String>> shardNodeMap = new HashMap();
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            ShardNode shardNode = this.getShardNode(k);
            shardNodeMap.compute(shardNode,(sk,sv)->{
                List<String> stringList = CollectionUtils.isEmpty(sv)?new ArrayList():sv;
                stringList.add(k);

                return stringList;
            });
        }

        return shardNodeMap;
    }

    //============================String=============================
    /**
     * 普通缓存获取
     * @param key 键
     * @return 值
     */
    public T get(String key){
        if(key==null)return null;
        ShardNode shardNode = this.getShardNode(key);
        if(shardNode.isGray()){
            return (T)shardNode.getNext().opsForValue().get(getShardKey(key));
        }
        return (T)shardNode.opsForValue().get(getShardKey(key));
    }

    /**
     * 普通缓存放入
     * @param key 键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key,Object value) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            shardNode.opsForValue().set(getShardKey(key), value);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForValue().set(getShardKey(key), value);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 普通缓存放入并设置时间
     * @param key 键
     * @param value 值
     * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key,Object value,long time){
        try {
            if(time>0){
                ShardNode shardNode = this.getShardNode(key);
                shardNode.opsForValue().set(getShardKey(key), value, time, TimeUnit.SECONDS);
                if(shardNode.isGray() || shardNode.isDoubleWriter()){
                    shardNode.getNext().opsForValue().set(getShardKey(key), value, time, TimeUnit.SECONDS);
                }
            }else{
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     * @param key 键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long incr(String key, long delta){
        if(delta<0){
            throw new RuntimeException("递增因子必须大于0");
        }
        ShardNode shardNode = this.getShardNode(key);
        if(shardNode.isGray() || shardNode.isDoubleWriter()){
            shardNode.getNext().opsForValue().increment(getShardKey(key), delta);
        }

        return shardNode.opsForValue().increment(getShardKey(key), delta);
    }

    /**
     * 递减
     * @param key 键
     * @param delta 要减少几(小于0)
     * @return
     */
    public long decr(String key, long delta){
        if(delta<0){
            throw new RuntimeException("递减因子必须大于0");
        }

        ShardNode shardNode = this.getShardNode(key);
        if(shardNode.isGray() || shardNode.isDoubleWriter()){
            shardNode.getNext().opsForValue().increment(getShardKey(key), -delta);
        }

        return shardNode.opsForValue().increment(getShardKey(key), -delta);
    }

    /**
     * 批量查询，对应mget
     * @param keys
     * @return
     */
    public List<String> mget(List<String> keys) {
        if(CollectionUtils.isEmpty(keys)){
            return null;
        }
        List<String> rs = new ArrayList<>();

        getShardNodes(keys).forEach((k,v)->{
            v = v.stream().map(p->getShardKey(p)).collect(Collectors.toList());
            if(k.isGray()){
                rs.addAll(k.getNext().opsForValue().multiGet(v));
            }else{
                rs.addAll(k.opsForValue().multiGet(v));
            }
        });

        return rs;
    }

    //================================Map=================================
    /**
     * HashGet
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public T hget(String key,String item){
        ShardNode shardNode = this.getShardNode(key);
        if(shardNode.isGray()){
            return (T)shardNode.getNext().opsForHash().get(getShardKey(key), item);
        }
        return (T)shardNode.opsForHash().get(getShardKey(key), item);
    }

    /**
     * 获取hashKey对应的所有键值
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object,Object> hGetAll(String key){
        ShardNode shardNode = this.getShardNode(key);
        if(shardNode.isGray()){
            shardNode = shardNode.getNext();
        }
        return shardNode.opsForHash().entries(getShardKey(key));
    }

    /**
     * HashSet
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String,Object> map){
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForHash().putAll(getShardKey(key), map);
            }
            shardNode.opsForHash().putAll(getShardKey(key), map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     * @param key 键
     * @param map 对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String,Object> map, long time){
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForHash().putAll(getShardKey(key), map);
            }
            shardNode.opsForHash().putAll(getShardKey(key), map);
            if(time>0){
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key,String item,Object value) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForHash().put(getShardKey(key), item, value);
            }
            shardNode.opsForHash().put(getShardKey(key), item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @param time 时间(秒)  注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key,String item,Object value,long time) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForHash().put(getShardKey(key), item, value);
            }
            shardNode.opsForHash().put(getShardKey(key), item, value);
            if(time>0){
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     * @param key 键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item){
        ShardNode shardNode = this.getShardNode(key);
        if(shardNode.isGray() || shardNode.isDoubleWriter()){
            shardNode.getNext().opsForHash().delete(getShardKey(key),item);
        }
        shardNode.opsForHash().delete(getShardKey(key),item);
    }

    /**
     * 判断hash表中是否有该项的值
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item){
        ShardNode shardNode = this.getShardNode(key);
        if(shardNode.isGray()){
            return shardNode.getNext().opsForHash().hasKey(getShardKey(key), item);
        }
        return shardNode.opsForHash().hasKey(getShardKey(key), item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     * @param key 键
     * @param item 项
     * @param by 要增加几(大于0)
     * @return
     */
    public double hincr(String key, String item,double by){
        ShardNode shardNode = this.getShardNode(key);

        double incr = shardNode.opsForHash().increment(getShardKey(key), item, by);
        if(shardNode.isGray() || shardNode.isDoubleWriter()){
            incr = shardNode.getNext().opsForHash().increment(getShardKey(key), item, by);
        }
        return incr;
    }

    /**
     * hash递减
     * @param key 键
     * @param item 项
     * @param by 要减少记(小于0)
     * @return
     */
    public double hdecr(String key, String item,double by){
        ShardNode shardNode = this.getShardNode(key);
        double incr = shardNode.opsForHash().increment(getShardKey(key), item, -by);
        if(shardNode.isGray() || shardNode.isDoubleWriter()){
            incr = shardNode.getNext().opsForHash().increment(getShardKey(key), item, -by);
        }
        return incr;
    }

    //============================set=============================
    /**
     * 根据key获取Set中的所有值
     * @param key 键
     * @return
     */
    public Set<Object> sget(String key){
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray()){
                return shardNode.getNext().opsForSet().members(getShardKey(key));
            }
            return shardNode.opsForSet().members(getShardKey(key));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     * @param key 键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key,Object value){
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray()){
                return shardNode.getNext().opsForSet().isMember(getShardKey(key), value);
            }
            return shardNode.opsForSet().isMember(getShardKey(key), value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     * @param key 键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sset(String key, Object...values) {
        try {
            ShardNode shardNode = this.getShardNode(key);

            long val = shardNode.opsForSet().add(getShardKey(key), values);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                val = shardNode.getNext().opsForSet().add(getShardKey(key), values);
            }
            return val;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     * @param key 键
     * @param time 时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sset(String key,long time,Object...values) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            Long count = shardNode.opsForSet().add(getShardKey(key), values);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                count = shardNode.getNext().opsForSet().add(getShardKey(key), values);
            }

            if(time>0) expire(key, time);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     * @param key 键
     * @return
     */
    public long sSize(String key){
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray()){
                return shardNode.getNext().opsForSet().size(getShardKey(key));
            }
            return shardNode.opsForSet().size(getShardKey(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除值为value的
     * @param key 键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long sRemove(String key, Object ...values) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            Long count = shardNode.opsForSet().remove(getShardKey(key), values);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                count = shardNode.getNext().opsForSet().remove(getShardKey(key), values);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    //===============================list=================================

    /**
     * 获取list缓存的内容
     * @param key 键
     * @param start 开始
     * @param end 结束  0 到 -1代表所有值
     * @return
     */
    public List<Object> lgetRange(String key,long start, long end){
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray()){
                return shardNode.getNext().opsForList().range(getShardKey(key), start, end);
            }
            return shardNode.opsForList().range(getShardKey(key), start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的所有内容
     * @param key
     * @return
     */
    public List<Object> lgetAll(String key) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray()){
                return shardNode.getNext().opsForList().range(getShardKey(key), 0,-1);
            }
            return shardNode.opsForList().range(getShardKey(key), 0,-1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     * @param key 键
     * @return
     */
    public long lSize(String key){
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray()){
                return shardNode.getNext().opsForList().size(getShardKey(key));
            }
            return shardNode.opsForList().size(getShardKey(key));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     * @param key 键
     * @param index 索引  index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return
     */
    public T lGetIndex(String key,long index){
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray()){
                return (T)shardNode.getNext().opsForList().index(getShardKey(key), index);
            }
            return (T)shardNode.opsForList().index(getShardKey(key), index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @return
     */
    public boolean lset(String key, Object value) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForList().rightPush(getShardKey(key), value);
            }
            shardNode.opsForList().rightPush(getShardKey(key), value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     * @return
     */
    public boolean lset(String key, Object value, long time) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForList().rightPush(getShardKey(key), value);
            }
            shardNode.opsForList().rightPush(getShardKey(key), value);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @return
     */
    public boolean lset(String key, List<Object> value) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForList().rightPushAll(getShardKey(key), value);
            }
            shardNode.opsForList().rightPushAll(getShardKey(key), value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     * @return
     */
    public boolean lset(String key, List<Object> value, long time) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForList().rightPushAll(getShardKey(key), value);
            }
            shardNode.opsForList().rightPushAll(getShardKey(key), value);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     * @param key 键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key, long index,Object value) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                shardNode.getNext().opsForList().set(getShardKey(key), index, value);
            }
            shardNode.opsForList().set(getShardKey(key), index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除N个值为value
     * @param key 键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key,long count,Object value) {
        try {
            ShardNode shardNode = this.getShardNode(key);
            Long remove = shardNode.opsForList().remove(getShardKey(key), count, value);
            if(shardNode.isGray() || shardNode.isDoubleWriter()){
                remove = shardNode.getNext().opsForList().remove(getShardKey(key), count, value);
            }

            return remove;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * scan 实现 execute已实现释放连接，不需要手动释放
     * @param pattern 表达式
     * @param consumer 对迭代到的key进行操作
     */
    public void scan(RedisTemplate redisTemplate,String pattern,Long limit,Consumer<byte[]> consumer) {
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().count(limit==null?Long.MAX_VALUE:limit).match(pattern).build())) {
                cursor.forEachRemaining(consumer);
                return null;
            } catch (Exception e) {
                throw new RedisShardException(ExceptionMsgEnum.SCAN_EXCEPTION,e);
            }
        });
    }

    /**
     * 获取分片上符合条件的key
     * @param pattern 表达式
     * @return
     */
    public Set<String> keys(ShardNode shardNode,String pattern,Long limit) {
        log.info("keys pattern:{}",pattern);
        Set<String> keys = new HashSet<>();
        this.scan(shardNode,pattern,limit,item -> {
            //符合条件的key
            String key = shardNode.getStringSerializer().deserialize(item).toString();
            keys.add(key.startsWith(getKeyPrefix())?StringUtils.substringAfter(key,getKeyPrefix()):key);
        });
        return keys;
    }

    //===============================lua=================================

    /**
     * lua脚本 单key，多值
     * @param redisScript
     * @param key
     * @param params
     * @return
     */
    public List<Object> lua(RedisScript redisScript, String key, String... params) {
        List<Object> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        ShardNode shardNode = this.getShardNode(key);
        Object object = shardNode.execute(redisScript, shardNode.getStringSerializer(),shardNode.getStringSerializer(),Lists.newArrayList(getShardKey(key)) , params);
        list.add(object);
        if(shardNode.isGray() || shardNode.isDoubleWriter()) {
            shardNode.getNext().execute(redisScript, shardNode.getStringSerializer(),shardNode.getStringSerializer(),Lists.newArrayList(getShardKey(key)), params);
        }

        log.info("runLua: {},time: {}",redisScript.getResultType(), (System.currentTimeMillis() - start));

        return list;
    }

    /**
     * lua脚本：支持同前缀不同后缀多值
     * 如：
     * ebike_latest_1
     * ebike_latest_2
     * ebike_latest_3
     *
     * @param redisScript
     * @param vals Map<String,Object> map的key为redis的key，Object为value
     * @return
     */
    public List<Object> lua(RedisScript redisScript, Map<String,Object> vals) {
        long start = System.currentTimeMillis();
        List<Object> list = new ArrayList<>();
        List<String> keys = vals.keySet().stream().collect(Collectors.toList());
        getShardNodes(keys).forEach((gnode,gkeys)->{
            log.info("gnode addresses:{},gkeys:{}",gnode.getAddresses(),JSON.toJSONString(gkeys));
            List<Object> groupList = new ArrayList<>(gkeys.size());
            gkeys.forEach(gkey->{
                groupList.add(vals.get(gkey));
            });

            gkeys = gkeys.stream().map(p->getShardKey(p)).collect(Collectors.toList());
            String params = JSON.toJSONString(groupList);
            log.info("gkeys:{},params:{}",JSON.toJSONString(gkeys),params);
            Object object = gnode.execute(redisScript, gnode.getStringSerializer(),gnode.getStringSerializer(),gkeys, params);
            if(gnode.isGray() || gnode.isDoubleWriter()){
                gnode.getNext().execute(redisScript, gnode.getStringSerializer(),gnode.getStringSerializer(),gkeys, params);
            }
            list.add(object);
        });
        log.info("runLua: {},time: {}",redisScript.getResultType(), (System.currentTimeMillis() - start));

        return list;
    }

    /**
     * lua脚本：支持一个lua执行多个不同前缀key的情况
     * 如：
     * ebike_latest_1
     * ebike_geo_1
     * ebike_latest_2
     * ebike_geo_2
     * ebike_geo_1
     *
     * lua脚本中使用了cjson.decode则需要使用fastjson或jackson需要化方式，也可以把params转成string，再使用string序例化
     *
     * @param redisScript
     * @param vals Map<String,Object> map的key为redis的key，Object为value
     * @return
     */
    public List<Object> lua(RedisScript redisScript, LinkedList<String> preKeys,Map<String,List<Object>> vals) {
        long start = System.currentTimeMillis();
        List<Object> list = new ArrayList<>();
        List<String> keys = Stream.of(vals).map(p->p.keySet()).flatMap(p->p.stream()).collect(Collectors.toList());
        List<String> preKeysNew = preKeys.stream().map(p->getShardKey(p)).collect(Collectors.toList());

        getShardNodes(keys).forEach((gnode,gkeys)->{
//            log.info("gnode addresses:{},gkeys:{}",gnode.getAddresses(),JSON.toJSONString(gkeys));
            //有序的map
            LinkedHashMap<String,List<Object>> groupListMap = new LinkedHashMap<>();
            for(String preKey:preKeys){
                groupListMap.put(preKey,new ArrayList<>(1));
            }
            gkeys.forEach(gkey->{
                groupListMap.compute(preKeys.stream().filter(p->gkey.startsWith(p)).findFirst().get(),(k,v)->{
                    v.addAll(vals.get(gkey));
                    return v;
                });
            });

//            log.info("groupListMap:{}",JSON.toJSONString(groupListMap));
            //LinkedHashMap在转换数组后也确保params是有序
            Object[] params = groupListMap.values().toArray();
            Object object = gnode.execute(redisScript, gnode.getValueSerializer(),gnode.getStringSerializer(),preKeysNew, params);
            if(gnode.isGray() || gnode.isDoubleWriter()){
                gnode.getNext().execute(redisScript, gnode.getValueSerializer(),gnode.getStringSerializer(),preKeysNew, params);
            }
            groupListMap.clear();
            list.add(object);
        });
        log.info("runLua: {},time: {}",redisScript.getResultType(), (System.currentTimeMillis() - start));

        return list;
    }

    /**
     * lua脚本 指定分片，多key，多值
     * @param redisScript
     * @param keys
     * @param params
     * @return
     */
    public List<Object> lua(ShardNode shardNode,RedisScript redisScript, List<String> keys, String... params) {
        List<Object> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        Object object = shardNode.execute(redisScript, shardNode.getStringSerializer(),shardNode.getStringSerializer(),keys , params);
        list.add(object);
        if(shardNode.isGray() || shardNode.isDoubleWriter()) {
            shardNode.getNext().execute(redisScript, shardNode.getStringSerializer(),shardNode.getStringSerializer(),keys, params);
        }

        log.info("runLua: {},time: {}",redisScript.getResultType(), (System.currentTimeMillis() - start));

        return list;
    }
}
