package com.ttyc.redis.shard;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ttyc.redis.shard.constant.ShardConstants;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.core.StringRedisShardClient;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
class RedisShardClientOpsValueTest {
    @Resource
    private RedisShardClient redisShardClient;
    @Resource
    private StringRedisShardClient stringRedisShardClient;
    @Value("${spring.application.name}")
    private String appName;
    private static final String test_key = "test_";

    static {
        //该方法设置vm启动参数
        System.setProperty("spring.profiles.active", "local");
    }

    @Test
    void set() {
        for (int i = 0; i < 20; i++) {
            Integer val = (int)(Math.random() * 10000);
            redisShardClient.set(test_key+i, i);
        }
        mget();
    }

    @Test
    void setCustomKey() {
        for (int i = 0; i < 20; i++) {
            Integer val = (int)(Math.random() * 10000);
            redisShardClient.set("reddis-shard"+ ShardConstants.REDIS_KEY_SPLIT +test_key+i, UUID.randomUUID()+"-"+val);
        }
    }

    @Test
    void get() {
        String val = stringRedisShardClient.get("test_ebike_latest_99991");
        log.info("get val:{}",val);
        Set<Object> set = stringRedisShardClient.sget("test_set");
        log.info("set:{}",JSON.toJSONString(set));
    }

    @Test
    void radius() {
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = stringRedisShardClient.radius("test_ebike_geo_88883",113.966104,35.071566,500.0);
        log.info("get val:{}",geoResults);
        List<EbikeNearby> nearbys = Lists.newArrayList();
        List<String> ebikeSnList = Lists.newArrayList();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult : geoResults) {
            EbikeNearby ebikeNearbyResult = new EbikeNearby();
            ebikeNearbyResult.setEbikeSn(geoResult.getContent().getName());
            ebikeNearbyResult.setLat(formatDouble(geoResult.getContent().getPoint().getY()));
            ebikeNearbyResult.setLon(formatDouble(geoResult.getContent().getPoint().getX()));
            ebikeNearbyResult.setDistace(geoResult.getDistance().getValue());
            ebikeNearbyResult.setLocalStationId(-1);

            ebikeSnList.add(geoResult.getContent().getName());
            nearbys.add(ebikeNearbyResult);
        }
        log.info("nearbys:{}",JSON.toJSONString(nearbys));
        log.info("ebikeSnList:{}",JSON.toJSONString(ebikeSnList));
    }

    private double formatDouble(double n) {
        return BigDecimal.valueOf(n).setScale(6, BigDecimal.ROUND_DOWN).doubleValue();
    }

    @Setter
    @Getter
    public static class EbikeNearby {
        private String ebikeSn;
        private double lat;
        private double lon;
        private double distace;
        private int localStationId;
    }

    @Test
    void getCustomKey() {
        String val = (String) redisShardClient.get("reddis-shard"+ ShardConstants.REDIS_KEY_SPLIT +"test_11");
        log.info("get val:{}",val);
    }

    @Test
    void mget() {
        List<String> allKeys = new ArrayList<>();
        redisShardClient.getAllShardNodes().forEach(node->{
            Set<String> keysn = redisShardClient.keys((ShardNode) node,appName+"*",10000L);
            log.info("node:{},keys size:{}",((ShardNode) node).getAddresses(),keysn.size());
                allKeys.addAll(keysn.stream().collect(Collectors.toList()));
        });

        List<String> list = redisShardClient.mget(allKeys);
        log.info("size:{},list:{}",list.size(),JSON.toJSONString(list));
    }

    @Test
    void mgetMap() {
        List<String> allKeys = new ArrayList<>();
        allKeys.add("test_0");
        allKeys.add("test_5");
        allKeys.add("test_14");
        allKeys.add("test_22");
        allKeys.add("test_7");
        allKeys.add("test_3");
        allKeys.add("test_23");

        Map<String,Object> list = redisShardClient.mgetMap(allKeys);
        log.info("size:{},list:{}",list.size(),JSON.toJSONString(list));
    }

    @Test
    void hasKey() {
        String key = test_key+"0";
        Boolean hasKey = redisShardClient.hasKey(key);
        log.info("key:{},hasKey:{}",key,hasKey);
    }

    @Test
    void setAndExpire() {
        for (int i = 0; i < 20; i++) {
            Integer val = (int)(Math.random() * 10000);
            redisShardClient.set(test_key+i, UUID.randomUUID()+"-"+val, RandomUtils.nextInt(0,20));
        }
        mget();
    }

    @Test
    void incr() {
        String key = test_key+"incr";
        long num = redisShardClient.incr(key,1);
        log.info("key:{},num:{}",key,num);
    }

    @Test
    void decr() {
        String key = test_key+"incr";
        long num = redisShardClient.decr(key,2);
        log.info("key:{},num:{}",key,num);
    }

    public static void main(String[] args) {
        System.out.println(StringUtils.substringAfter("ebike_latest_700116116","ebike_latest_"));
    }
}
