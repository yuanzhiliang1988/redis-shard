package com.ttyc.redis.shard;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.listener.handler.AbstractHandler;
import com.ttyc.redis.shard.listener.handler.TransferHandler;
import com.ttyc.redis.shard.support.Transfer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@SpringBootTest
class TransferTest {
    @Resource
    private RedisShardClient redisShardClient;
    @Value("${spring.application.name}")
    private String appName;

    static {
        //该方法设置vm启动参数
        System.setProperty("spring.profiles.active", "dev");
        System.setProperty("apollo.cacheDir", "/redis-shard/config-cache");
    }

    /**
     * 测试根据应用名获取分片内所有key
     */
    @Test
    void transfer() {
        AbstractHandler abstractHandler = new TransferHandler();
        abstractHandler.transfer();
    }

    /**
     * 测试根据应用名获取分片内所有key
     */
    @SneakyThrows
    @Test
    void clientTransfer() {
        Transfer transfer = new Transfer();
        transfer.setToIndex(0);
        JSONObject fromNode = new JSONObject();
        fromNode.put("addresses","120.0.0.1:6380");
        transfer.setFromNodes(fromNode.toJSONString());

        redisShardClient.transfer(transfer);
    }

    @SneakyThrows
    @Test
    void clientTransferKeyRegex() {
        Transfer transfer = new Transfer();
        transfer.setToIndex(0);
        List<String> tranKey = new ArrayList<>();
        tranKey.add("test_*");
        tranKey.add("test2_*");
        transfer.setTranKeyRegex(JSON.toJSONString(tranKey));
        JSONObject fromNode = new JSONObject();
        fromNode.put("addresses","120.0.0.1:6379");
        fromNode.put("password","admin");
        fromNode.put("serializer","string");
        transfer.setFromNodes(fromNode.toJSONString());

        redisShardClient.transfer(transfer);
    }
}
