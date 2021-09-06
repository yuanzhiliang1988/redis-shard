package com.ttyc.redis.shard;

import com.alibaba.fastjson.JSON;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
class RedisShardClientOpsValueTest {
    @Resource
    private RedisShardClient redisShardClient;
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
            redisShardClient.set(test_key+i, UUID.randomUUID()+"-"+val);
        }
        mget();
    }

    @Test
    void get() {
        String val = (String) redisShardClient.get("test_ebike_latest_99991");
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
}
