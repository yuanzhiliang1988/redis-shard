package com.ttyc.redis.shard;

import com.alibaba.fastjson.JSON;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.core.StringRedisShardClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ZSetOperations;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@SpringBootTest
class RedisShardClientOpsZsetTest {
    @Resource
    private RedisShardClient redisShardClient;
    @Resource
    private StringRedisShardClient stringRedisShardClient;
    @Value("${spring.application.name}")
    private String appName;

    static {
        //该方法设置vm启动参数
        System.setProperty("spring.profiles.active", "local");
    }

    @Test
    void rangeByScore() {
        Set<Object> objects = redisShardClient.rangeByScore("transfer",1000002,1000003);
        log.info("objects:{}",JSON.toJSONString(objects));
        for(Object obj:objects){
            List<String> keys = (List)obj;
            log.info("keys:{}",JSON.toJSONString(keys));
            log.info("key:{}",keys.get(0));
        }
    }

    @Test
    void rangeByScoreWithScores() {
        String redisKey = "redis-shard@@track_zset_811624100";
        log.info("aaa:"+JSON.toJSONString(redisShardClient.getShardNode(redisKey).opsForZSet().rangeWithScores(redisKey,0,0)));
        log.info("aaa:"+JSON.toJSONString(redisShardClient.getShardNode(redisKey).opsForZSet().range(redisKey,0,0)));
        Set<ZSetOperations.TypedTuple<Object>> objects = redisShardClient.rangeByScoreWithScores("track_zset_811624100",0,0);
        log.info("objects:{}",JSON.toJSONString(objects));
        for(ZSetOperations.TypedTuple<Object> typedTuple:objects){
            List<String> keys = (List)typedTuple.getValue();
            log.info("keys:{}",JSON.toJSONString(keys));
            log.info("key:{}",keys.get(0));
            log.info("scores:{}",typedTuple.getScore());
        }
    }

    @Test
    void zRemoveRangeByScore(){
        long removeNum = stringRedisShardClient.zRemoveRangeByScore("geo-reporter@@track_zset_811624100",-1,Double.parseDouble("1634192237977"));
        log.info("removeNum:{}",removeNum);
    }
}
