package com.ttyc.redis.shard;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.script.RedisScript;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest
class RedisShardClientMethodTest {
    @Resource
    private RedisShardClient redisShardClient;
    @Value("${spring.application.name}")
    private String appName;
    @Resource
    private Sharding sharding;

    static {
        //该方法设置vm启动参数
        System.setProperty("spring.profiles.active", "local");
    }

    /**
     * 测试根据应用名获取分片内所有key
     */
    @Test
    void keys() {
        List<String> allKeys = new ArrayList<>();
        redisShardClient.getAllShardNodes().forEach(node->{
            Set<String> keys = redisShardClient.keys((ShardNode) node,appName+"*",10000L);
            log.info("node:{},keys size:{},keys:{}",((ShardNode) node).getAddresses(),keys.size(),JSON.toJSONString(keys));
            allKeys.addAll(keys.stream().collect(Collectors.toList()));
        });
        List<String> list = redisShardClient.mget(allKeys);
        log.info("allKeys size:{},value size:{},list:{}",allKeys.size(),list.size(),JSON.toJSONString(list));
    }

    @Test
    void getNextShardNodes() {
        ShardNode shardNode = redisShardClient.getShardNode(3);
        Map<String, ShardNode> shardNodeMap = sharding.getAllNextShardNode(shardNode);
        shardNodeMap.forEach((k,v)->{
            log.info("k:{},v:{}",k,v.getAddresses());
        });
    }

    @Test
    void getKeyType() {
        DataType dataType = redisShardClient.getKeyType("redis-shard_test_set8");
        log.info("code:{},name:{}",dataType.code(),dataType.name());
    }

    @Test
    void del() {
        redisShardClient.del("test_ebike_geo_88884");
    }

    @Test
    void dels() {
        List<String> allKeys = new ArrayList<>();
        redisShardClient.getAllShardNodes().forEach(node->{
            Set<String> keysn = redisShardClient.keys((ShardNode) node,appName+"*",10000L);
            log.info("node:{},keys size:{}",((ShardNode) node).getAddresses(),keysn.size());
            allKeys.addAll(keysn.stream().collect(Collectors.toList()));
        });

        log.info("allKeys:{}",JSON.toJSONString(allKeys));
        redisShardClient.del(allKeys);
    }

    @SneakyThrows
    @Test
    void lua() {
        String scriptText = "local bikes = cjson.decode(ARGV[1])\n" +
                "for _, bike in ipairs(bikes) do\n" +
                "    local key1 = KEYS[1]..bike.bikeSn;\n" +
                "    redis.call('set', key1, bike.serializeValue) -- 车辆最新坐标\n" +
                "    redis.call('geoadd', KEYS[2], bike.lon, bike.lat, bike.bikeSn)  -- 车辆geo\n" +
                "end\n" +
                "return #bikes;";

        RedisScript redisScript = RedisScript.of(scriptText, Integer.class);
        List<String> keys = Lists.newArrayListWithCapacity(2);
        keys.add("test_ebike_latest_");
        keys.add("test_ebike_geo");

        List<BikeGpsInfo> bikeGpsInfos = new ArrayList<>();
        for (int i = 0; i < RandomUtils.nextInt(1,10); i++) {
            BikeGpsInfo bikeGpsInfo = new BikeGpsInfo();
            bikeGpsInfo.setBikeSn("9999"+i);
            bikeGpsInfo.setLat(35.071566D);
            bikeGpsInfo.setLon(113.966104D);
            bikeGpsInfo.setSerializeValue(bikeGpsInfo.formatReidsVal());
            bikeGpsInfo.setTs(1630393589923L);

            bikeGpsInfos.add(bikeGpsInfo);
        }
        String data = JSONObject.toJSONString(bikeGpsInfos);

        List<Object> result = redisShardClient.lua(redisShardClient.getShardNode(0),redisScript,keys,data);
        for(Object o:result){
            log.info("result:{}",o);
        }
    }

    @SneakyThrows
    @Test
    void lua2() {
        String scriptText = "local bikes = cjson.decode(ARGV[1])\n" +
                "for _, bike in ipairs(bikes) do\n" +
                "    local key1 = KEYS[1]..bike.bikeSn;\n" +
                "    redis.call('set', key1, bike.serializeValue) -- 车辆最新坐标\n" +
//                "    redis.call('geoadd', KEYS[2], bike.lon, bike.lat, bike.bikeSn)  -- 车辆geo\n" +
                "end\n" +
                "return #bikes;";

        RedisScript redisScript = RedisScript.of(scriptText, Integer.class);

        Map<String,BikeGpsInfo> bikeGpsInfoMap = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            BikeGpsInfo bikeGpsInfo = new BikeGpsInfo();
            bikeGpsInfo.setBikeSn("9999"+i);
            bikeGpsInfo.setLat(35.071566D);
            bikeGpsInfo.setLon(113.966104D);
            bikeGpsInfo.setSerializeValue(bikeGpsInfo.formatReidsVal());
            bikeGpsInfo.setTs(System.currentTimeMillis());

            bikeGpsInfoMap.put("test_ebike_latest_"+bikeGpsInfo.getBikeSn(),bikeGpsInfo);
        }

        List<Object> result = redisShardClient.lua(redisScript,bikeGpsInfoMap);
        for(Object o:result){
            log.info("result:{}",o);
        }
    }

    /**
     * 测试一个lua执行多个key的情况
     * 支持多key分片
     */
    @SneakyThrows
    @Test
    void lua3() {
        String scriptText = "local bikes = cjson.decode(ARGV[1])\n" +
                "for _, bike in ipairs(bikes) do\n" +
                "    local key1 = KEYS[1]..bike.bikeSn;\n" +
                "    redis.call('set', key1, bike.serializeValue) -- 车辆最新坐标\n" +
                "end\n" +
                "local bikes2 = cjson.decode(ARGV[2])\n" +
                "for _, bike in ipairs(bikes2) do\n" +
                "    local key2 = KEYS[2]..bike.cityId;\n" +
                "    redis.call('geoadd', key2, bike.lon, bike.lat, bike.bikeSn)  -- 车辆geo\n" +
                "end\n" +
                "return #bikes+#bikes2;";

        RedisScript redisScript = RedisScript.of(scriptText, Integer.class);
        LinkedList<String> keys = Lists.newLinkedList();
        keys.add("test_ebike_latest_");
        keys.add("test_ebike_geo_");

        Map<String,List<BikeGpsInfo>> bikeGpsInfoMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            BikeGpsInfo bikeGpsInfo = new BikeGpsInfo();
            bikeGpsInfo.setBikeSn("9999"+i);
            bikeGpsInfo.setLat(35.071566D);
            bikeGpsInfo.setLon(113.966104D);
            bikeGpsInfo.setSerializeValue(bikeGpsInfo.formatReidsVal());
            bikeGpsInfo.setTs(System.currentTimeMillis());
            bikeGpsInfo.setCityId(Integer.parseInt("8888"+RandomUtils.nextInt(1,8)));

            bikeGpsInfoMap.put("test_ebike_latest_"+bikeGpsInfo.getBikeSn(),new ArrayList(1){{add(bikeGpsInfo);}});
            bikeGpsInfoMap.compute("test_ebike_geo_"+bikeGpsInfo.getCityId(),(k,v)->{
                if(v==null){
                    v = new ArrayList();
                }
                v.add(bikeGpsInfo);

                return v;
            });
        }

        List<Object> result = redisShardClient.lua(redisScript,keys,bikeGpsInfoMap);
        for(Object o:result){
            log.info("result:{}",o);
        }
    }

    @Data
    public static class BikeGpsInfo {
        private Integer cityId;
        private String bikeSn;
        private long ts;
        //原始经纬度
        private double lon;
        private double lat;

        private String serializeValue;

        /**
         * 存到redis 序列化
         * 经度｜维度｜方位角｜海拔｜gps速度｜霍尔速度｜状态｜时间｜新经度｜新纬度
         * @return
         */
        public String formatReidsVal() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(lon);
            stringBuilder.append("|");
            stringBuilder.append(lat);
            stringBuilder.append("|");
            stringBuilder.append(ts);
            stringBuilder.append("|");
            stringBuilder.append(cityId);

            return stringBuilder.toString();
        }
    }

    public static void main(String[] args) {
        LinkedHashMap<String,Object> val1 = new LinkedHashMap();
        val1.put("test_ebike_latest_1",4);
        val1.put("test_ebike_latest_2",2);
        val1.put("test_ebike_latest_3",3);

        Object[] params = val1.values().toArray();

        System.out.println(JSON.toJSONString(params));
    }
}
