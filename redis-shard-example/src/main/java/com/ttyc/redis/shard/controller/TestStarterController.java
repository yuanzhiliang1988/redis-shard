package com.ttyc.redis.shard.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ttyc.redis.shard.support.Node;
import com.ttyc.redis.shard.core.RedisShardClient;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yuanzl
 * @date 2021/8/18 5:04 下午
 */
@RestController
@RequestMapping("/testStarter")
public class TestStarterController {
    @Resource
    private RedisShardClient redisShardClient;

    @GetMapping(value = "/test")
    public String test() {
        return "i am ok!";
    }

    @RequestMapping(value = "/testRedisShard",method = {RequestMethod.POST})
    public String testRedisShard(@RequestBody JSONObject jsonObject) {
        String key = jsonObject.getString("key");
        Node node = new Node();
        node.setAddresses("127.0.0.1:6379");
        node.setDatabase(0);
        node.setPassword("aaaaaaaaaaa");
        node.setType(1);
        node.setUrl("127.0.0.1:6379127.0.0.1:6379127.0.0.1:6379127.0.0.1:6379127.0.0.1:6379");

        long begin = System.currentTimeMillis();
//        for (int i = 0; i < 200; i++) {
            redisShardClient.set(key,node);
            Node node1 = (Node) redisShardClient.get(key);
            System.out.println("node1:"+ JSON.toJSONString(node1));
//        }
        long time = System.currentTimeMillis() - begin;
        System.out.println("time:"+time);

        Node node2 = new Node();
        BeanUtils.copyProperties(node,node2);
        node2.setPassword("bbbbbbbbbbbbb");
        List<Node> list = new ArrayList<>();
        list.add(node);
        list.add(node2);
        redisShardClient.lset("test_start_list",list);

        List<Node> list1 = redisShardClient.lgetAll("test_start_list");
        System.out.println("list1:"+JSON.toJSONString(list1));

        Set<Node> nodeSet = new HashSet<>();
        nodeSet.add(node);
        nodeSet.add(node2);
        redisShardClient.sset("test_start_set",nodeSet);
        Set<Node> nodeSet1 = redisShardClient.sget("test_start_set");
        System.out.println("nodeSet1:"+JSON.toJSONString(nodeSet1));

        return "ok";
    }

    @RequestMapping(value = "/testRedisShardGrayWriter",method = {RequestMethod.POST})
    public String testRedisShardGrayWriter(@RequestBody JSONObject jsonObject) {
        String key = jsonObject.getString("key");
        String value = jsonObject.getString("value");
        redisShardClient.set(key,value);

        return "ok";
    }

    @RequestMapping(value = "/testRedisShardGrayRead",method = {RequestMethod.POST})
    public Object testRedisShardGrayRead(@RequestBody JSONObject jsonObject) {
        String key = jsonObject.getString("key");
        Object o = redisShardClient.get(key);

        return o;
    }

    @RequestMapping(value = "/testRedisShardRegex",method = {RequestMethod.POST})
    public String testRedisShardRegex(@RequestBody JSONObject jsonObject) {
        String key = jsonObject.getString("key");
        redisShardClient.set(key,"node");

        return "ok";
    }
}
