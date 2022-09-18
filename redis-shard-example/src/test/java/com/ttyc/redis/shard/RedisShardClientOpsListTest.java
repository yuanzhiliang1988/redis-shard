package com.ttyc.redis.shard;

import com.alibaba.fastjson.JSON;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.core.StringRedisShardClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest
class RedisShardClientOpsListTest {
    @Resource
    private RedisShardClient redisShardClient;
    @Resource
    private StringRedisShardClient stringRedisShardClient;
    @Value("${spring.application.name}")
    private String appName;
    private static final String test_key = "test_list";
    private static final String test_item_key = "test_list_str_";
    @Resource
    private StringRedisShardClient myStringRedisShardClient1;
    @Resource
    private StringRedisShardClient myStringRedisShardClient2;
    @Resource
    private RedisShardClient myRedisShardClient2;

    static {
        //该方法设置vm启动参数
        System.setProperty("spring.profiles.active", "dev");
        System.setProperty("Denv", "dev");
        System.setProperty("Dapollo.meta", "http://dev-apollo.in.songguo7.com:8080,http://dev-apollo.in.songguo7.com:8080");
        System.setProperty("Dapollo.cacheDir", "/redis-shard/config-cache");
    }

    @Data
    private static class User{
        private String name;
        private Integer age;
        private BigDecimal score;
        private List<String> classes;

        @Override
        public String toString(){
            StringBuilder stringBuilder=new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append("|");
            stringBuilder.append(age);
            stringBuilder.append("|");
            stringBuilder.append(score);
            stringBuilder.append("|");
            stringBuilder.append(classes);
            stringBuilder.append("|");

            return stringBuilder.toString();
        }
    }

    @Test
    void set() {
        List<String> users = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Integer val = (int)(Math.random() * 10000);
            int y = i;
            Stream.of(RandomUtils.nextDouble(1,20)).limit(10).forEach(x->{
                User user = new User();
                user.setName("同学"+x);
                user.setAge(val);
                user.setScore(BigDecimal.valueOf(x).setScale(4, RoundingMode.HALF_UP));
                user.setClasses(new ArrayList(){{add("bbbbbb");}});
//                redisShardClient.lset(test_key+x.intValue(),user);

//                stringRedisShardClient.lset(test_item_key+x.intValue(),user.toString());
                users.add(user.toString());
            });
        }

        stringRedisShardClient.lsetAll(test_item_key+19,users);
        stringRedisShardClient.lsetAll(test_item_key+18,users);
        myStringRedisShardClient1.lsetAll(test_item_key+17,users);
        myStringRedisShardClient2.lsetAll(test_item_key+16,users);
    }

    @Test
    void lgetAll() {
//        String key = "geo-reporter@@test_set_string";
//        myStringRedisShardClient2.sset(key,"张三","18");
//        Set<Object> val = myStringRedisShardClient2.sget(key);
//        val.add("男");
//        myStringRedisShardClient2.sset(key,val.toArray());
//        log.info("get val:{},set:{}",JSON.toJSONString(val));
//        String key = "geo-reporter@@test_map_string";
//        myStringRedisShardClient2.hset(key,"name","张三");
//        myStringRedisShardClient2.hset(key,"age","18");
//        myStringRedisShardClient2.hset(key,"sex","男");
//        String key = "geo-reporter@@test_list_string";
//        myStringRedisShardClient2.lset(key,"test1");
//        myStringRedisShardClient2.lset(key,"你是谁");
        String key = "geo-reporter@@test_string";
        myStringRedisShardClient2.set(key,"test2_string");
    }

    @Test
    void lgetRange() {
        List<User> val = redisShardClient.lgetRange(test_key+"6",1,2);
        log.info("get val:{}",JSON.toJSONString(val));
    }
}
