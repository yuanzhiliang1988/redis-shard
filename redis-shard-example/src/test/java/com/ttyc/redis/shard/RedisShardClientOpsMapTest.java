package com.ttyc.redis.shard;

import com.alibaba.fastjson.JSON;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest
class RedisShardClientOpsMapTest {
    @Resource
    private RedisShardClient redisShardClient;
    @Value("${spring.application.name}")
    private String appName;
    private static final String test_key = "test_map";
    private static final String test_item_key = "test_";

    static {
        //该方法设置vm启动参数
        System.setProperty("spring.profiles.active", "local");
    }

    @Data
    private static class User{
        private String name;
        private Integer age;
        private BigDecimal score;
        private List<String> classes;
    }

    @Test
    void set() {
        for (int i = 0; i < 20; i++) {
            Integer val = (int)(Math.random() * 10000);
            int y = i;
            Stream.of(RandomUtils.nextDouble(1,20)).limit(10).forEach(x->{
                User user = new User();
                user.setName("同学"+x);
                user.setAge(val);
                user.setScore(BigDecimal.valueOf(x).setScale(4, RoundingMode.HALF_UP));
                user.setClasses(new ArrayList(){{add("aaaaa");}});
                redisShardClient.hset(test_key+x.intValue(),test_item_key+x, user);
            });
        }
    }

    @Test
    void get() {
        User val = (User) redisShardClient.hget(test_key+"8",test_item_key+"8.246638810097568");
        log.info("get val:{}",JSON.toJSONString(val));
    }

    @Test
    void gets() {
        Map<String,User> val = (Map<String,User>) redisShardClient.hGetAll(test_key+"8");
        log.info("gets val:{}",JSON.toJSONString(val));
    }

    @Test
    void del() {
        redisShardClient.hdel(test_key+"8","test_8.599364191312976");
    }

    @Test
    void hasKey() {
        String key = test_key+"8";
        Boolean hasKey = redisShardClient.hHasKey(key,"test_8.599364191312976");
        log.info("key:{},hasKey:{}",key,hasKey);
        Boolean hasKey2 = redisShardClient.hHasKey(key,"test_8.246638810097568");
        log.info("key:{},hasKey2:{}",key,hasKey2);
    }

    @Test
    void setAndExpire() {
        for (int i = 0; i < 20; i++) {
            Integer val = (int)(Math.random() * 10000);
            int y = i;
            Stream.of(RandomUtils.nextDouble(1,20)).limit(10).forEach(x->{
                User user = new User();
                user.setName("同学"+x);
                user.setAge(val);
                user.setScore(BigDecimal.valueOf(x).setScale(4, RoundingMode.HALF_UP));
                user.setClasses(new ArrayList(){{add("bbbb");}});
                redisShardClient.hset(test_key+x.intValue(),test_item_key+x, user,RandomUtils.nextInt(0,20));
            });
        }
    }
}
