package com.ttyc.redis.shard;

import com.alibaba.fastjson.JSON;
import com.ttyc.redis.shard.core.RedisShardClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest
class RedisShardClientOpsListTest {
    @Resource
    private RedisShardClient redisShardClient;
    @Value("${spring.application.name}")
    private String appName;
    private static final String test_key = "test_list";
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
                redisShardClient.lset(test_key+x.intValue(),user);
            });
        }
    }

    @Test
    void lgetAll() {
        List<Object> val = redisShardClient.lgetAll(test_key+"6");
        log.info("get val:{}",JSON.toJSONString(val));
    }

    @Test
    void lgetRange() {
        List<User> val = redisShardClient.lgetRange(test_key+"6",1,2);
        log.info("get val:{}",JSON.toJSONString(val));
    }
}
