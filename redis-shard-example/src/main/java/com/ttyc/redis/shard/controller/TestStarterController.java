package com.ttyc.redis.shard.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ttyc.redis.shard.support.Node;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * @author yuanzl
 * @date 2021/8/18 5:04 下午
 */
@RestController
@RequestMapping("/testStarter")
@Slf4j
public class TestStarterController {
    @Resource
    private RedisShardClient redisShardClient;
    @Resource
    private RedisShardClient myRedisShardClient1;
    @Resource
    private RedisShardClient myRedisShardClient2;

    @GetMapping(value = "/test")
    public String test() {
        return "i am ok!";
    }

    @RequestMapping(value = "/testRedisShard",method = {RequestMethod.POST})
    public String testRedisShard(@RequestBody JSONObject jsonObject) {
        String key = jsonObject.getString("key");
        Node node = new Node();
        node.setAddresses("10.9.198.84:6379");
        node.setDatabase(0);
        node.setPassword("aaaaaaaaaaa");
        node.setType(1);

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
        Object o1 = myRedisShardClient1.get(key);
        Object o2 = myRedisShardClient2.get(key);

        log.info("key:{},data:{},{},{}",key,o,o1,02);
        return o;
    }

    @RequestMapping(value = "/testRedisShardRegex",method = {RequestMethod.POST})
    public String testRedisShardRegex(@RequestBody JSONObject jsonObject) {
        String key = jsonObject.getString("key");
        redisShardClient.set(key,"node");
        return "ok";
    }

    @RequestMapping(value = "/testHashDel",method = {RequestMethod.POST})
    public String testHashDel(@RequestBody JSONObject jsonObject) {
        String key = jsonObject.getString("key");
        ShardNode shardNode = redisShardClient.getShardNode(0);
        log.info("address:{}",shardNode.getAddresses());
//        shardNode.opsForHash().put(key,"11aaaa",0);
//        shardNode.opsForHash().put(key,"11bbb",0);
//        shardNode.opsForHash().put(key,"11ccc",0);
//        shardNode.opsForHash().put(key,"22aaaa",0);
//        shardNode.opsForHash().put(key,"22bbbb",0);
//        shardNode.opsForHash().put(key,"22cccc",0);
//        shardNode.opsForHash().put(key,"22dddd",0);
//        shardNode.opsForHash().put(key,"33aaaa",0);

        // 查询以22开头的Hash key
        ScanOptions scanOptions = ScanOptions.scanOptions().count(2).match("22*").build();
        Cursor<Map.Entry<String, Object>> cursor = shardNode.opsForHash().scan(key,scanOptions);
        List<String> itemKeys = new ArrayList<>();
        cursor.forEachRemaining(v->{
            log.info("key:{},value:{}",v.getKey(),v.getValue());
            itemKeys.add(v.getKey());
        });
        if(!itemKeys.isEmpty()){
            shardNode.opsForHash().delete(key,itemKeys.toArray());
        }
        try {
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "ok";
    }
}
