package com.ttyc.redis.shard;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.core.StringRedisShardClient;
import com.ttyc.redis.shard.support.ShardNode;
import com.ttyc.redis.shard.utils.SpringContextUtils;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
class RedisShardClientMethodTest {
    @Resource
    private RedisShardClient redisShardClient;
    @Resource
    private StringRedisShardClient stringRedisShardClient;
    @Value("${spring.application.name}")
    private String appName;
//    @Resource
    private static Sharding sharding;

    static {
        //该方法设置vm启动参数
        System.setProperty("spring.profiles.active", "local");
//        sharding = (Sharding) SpringContextUtils.getBean("sharding");
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
        redisShardClient.del("test_1");
    }

    @Test
    void dels() {
        List<String> allKeys = new ArrayList<>();
        redisShardClient.getAllShardNodes().forEach(node->{
            Set<String> keysn = redisShardClient.keys((ShardNode) node,"reddis-shard@@"+"*",10000L);
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

        for (int i = 0; i < 5; i++) {
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

    @Test
    void lua4() {
        String scriptText = "local fromNode = cjson.decode(ARGV[1])\n" +
                "local key = (KEYS[1])\n" +
                "return 1;";

        RedisScript redisScript = RedisScript.of(scriptText, Integer.class);
        LinkedList<String> keys = Lists.newLinkedList();
        keys.add("redis-shard@@test_set18");
        ShardNode toShardNode = redisShardClient.getShardNode(0);
        ShardNode fromShardNode = redisShardClient.getShardNode(1);
        List<Object> result = redisShardClient.lua(toShardNode,redisScript,keys,fromShardNode);
        for(Object o:result){
            log.info("result:{}",o);
        }
    }

    @Data
    public static class BikeGpsInfo {
        private int cityId;
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

    @SneakyThrows
    @Test
    void lua5() {
        String scriptText = "local key = KEYS[1]\n" +
                "local gpsVals = cjson.decode(ARGV[1])\n" +
                "local trackExpireTime = ARGV[2]\n" +
//                "local trackCleanKey = ARGV[3]\n" +
//                "local expireCleanTime = ARGV[4]\n" +
//                "local trackLastCleanExpireTime = ARGV[5]\n" +
                "local expireTime = ARGV[3]\n" +
                "local redisTrackSize = ARGV[4]\n"+
                "--删除过期的gps\n"+
                "local function exprieByTime(key,expireTime)\n" +
                "    return redis.call('zRemRangeByScore', key,0,expireTime)\n" +
                "end\n"+
                "local function needExprieClean(key,size,redisTrackSize,expireTime)\n" +
                "    if size > redisTrackSize then\n" +
                "        local firstGpses = redis.call('zrange',key,0,0, 'WITHSCORES')\n" +
                "        local gpsScore"+
                "        for gpsKey,gpsVal in pairs(firstGpses) do"+
                "            if (gpsKey%2==0) then gpsScore=gpsVal end"+
                "        end\n"+
//                "            return gpsScore\n" +
                "        if gpsScore and expireCleanTime >= gpsScore then\n" +
                "            return exprieByTime(key,expireTime)\n" +
                "        end\n" +
                "    end\n" +
                "      return 1\n" +
                "end\n"+
                "for _, gpsVal in ipairs(cjson.decode(gpsVals)) do\n"+
                "redis.call('zadd', key,gpsVal.score,gpsVal.value)\n" +
                "end\n"+
                "if trackExpireTime then\n" +
                "redis.call('expire', key, trackExpireTime)\n" +
                "end\n"+
                "local size = redis.call('zcard', key)\n"+
                "local removedNum = needExprieClean(key,size,tonumber(redisTrackSize),tonumber(expireTime))\n"+
                "return removedNum;";

        RedisScript redisScript = RedisScript.of(scriptText, Integer.class);
        log.info(redisScript.getScriptAsString());
        Long trackExpireTime = Long.parseLong(720*60*1000+"");
//        Long expireCleanTime = Long.parseLong((System.currentTimeMillis()-1*60*1000)+"");
//        Long trackLastCleanExpireTime =Long.parseLong( (2*60)+"");
        Long expireTime = Long.parseLong((System.currentTimeMillis()-60*1000)+"");

        Set<ZSetOperations.TypedTuple<String>> tuples2 = new HashSet<>();
        ZSetOperations.TypedTuple<String> tupleTy1 = new DefaultTypedTuple<String>("|1634196171623|null|null|null||||||ver47",Double.parseDouble(System.currentTimeMillis()+""));
        tuples2.add(tupleTy1);
        ZSetOperations.TypedTuple<String> tupleTy2 = new DefaultTypedTuple<String>("|1634196161211|null|null|null||||||ver52",Double.parseDouble((System.currentTimeMillis()+2)+""));
        tuples2.add(tupleTy2);

        String val = JSON.toJSONString(tuples2);
        log.info("val:{}",val);
        List<Object> result = redisShardClient.lua(redisScript,"track_zset_811624100",val,trackExpireTime,expireTime,3);
        for(Object o:result){
            log.info("result:{}",o);
        }
    }

    @Data
    public static class GpsInfo {
        private Long ts;
        private String val;
    }

    /**
     * 解决StringRedisShardClient和RedisShardClient共用时在多线程情况下对象序列化会串的问题
     * 如RedisShardClient原本使用jackson序列化，但由于StringRedisShardClient使用string序列化
     * 导致RedisShardClient的序列化为string，并抛出如下异常：
     * java.lang.ClassCastException: java.util.ArrayList cannot be cast to java.lang.String
     * 	at com.ttyc.redis.shard.serializer.StringRedisSerializer.serialize(StringRedisSerializer.java:37)
     * 	at org.springframework.data.redis.core.script.DefaultScriptExecutor.keysAndArgs(DefaultScriptExecutor.java:112
     *
     * 	问题原因就是lua函数中的gnode.getValueSerializer()，由于两个对象使用的ShardNode使用的同一个，被一个修改后，另一个获取到了被修改后的对象，
     * 	从原来的jackson被StringRedisShardClient修改成了string
     *
     */
    @SneakyThrows
    @Test
    void lua6() {
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

        List<BikeGpsInfo> bikeGpsInfos = JSONArray.parseArray("[{\"alt\":27.3,\"azi\":33,\"bikeSn\":\"811111660\",\"cityId\":393,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":20,\"hoarSpeed\":22,\"lat\":24.573038198486643,\"lon\":112.1029316168375,\"serializeValue\":\"112.1029316168375|24.573038198486643|33|27.3|20|22|0|1634227333000|0.0|0.0|393\",\"status\":0,\"ts\":1634227333000},{\"alt\":3480.8,\"azi\":48,\"bikeSn\":\"810123047\",\"cityId\":433,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":4,\"hoarSpeed\":5,\"lat\":34.003232383712906,\"lon\":102.09017136940284,\"serializeValue\":\"102.09017136940284|34.003232383712906|48|3480.8|4|5|0|1634227333000|0.0|0.0|433\",\"status\":0,\"ts\":1634227333000},{\"alt\":2.9,\"azi\":114,\"bikeSn\":\"815753564\",\"cityId\":401,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":7,\"hoarSpeed\":15,\"lat\":21.91264282739409,\"lon\":110.84218574743309,\"serializeValue\":\"110.84218574743309|21.91264282739409|114|2.9|7|15|0|1634227330000|0.0|0.0|401\",\"status\":0,\"ts\":1634227330000},{\"alt\":11.6,\"azi\":152,\"bikeSn\":\"815314656\",\"cityId\":168,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":0,\"hoarSpeed\":0,\"lat\":29.79810040119166,\"lon\":118.22416556010633,\"serializeValue\":\"118.22416556010633|29.79810040119166|152|11.6|0|0|0|1634227333000|0.0|0.0|168\",\"status\":0,\"ts\":1634227333000},{\"alt\":16.8,\"azi\":122,\"bikeSn\":\"808521765\",\"cityId\":416,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":0,\"hoarSpeed\":0,\"lat\":19.25874987542802,\"lon\":110.47712501736275,\"serializeValue\":\"110.47712501736275|19.25874987542802|122|16.8|0|0|0|1634227334000|0.0|0.0|416\",\"status\":0,\"ts\":1634227334000},{\"alt\":53.4,\"azi\":85,\"bikeSn\":\"811642949\",\"cityId\":585,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":18,\"hoarSpeed\":19,\"lat\":21.386182852881202,\"lon\":110.2666696777701,\"serializeValue\":\"110.2666696777701|21.386182852881202|85|53.4|18|19|0|1634227333000|0.0|0.0|585\",\"status\":0,\"ts\":1634227333000},{\"alt\":621.4,\"azi\":31,\"bikeSn\":\"816331972\",\"cityId\":291,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":23,\"hoarSpeed\":24,\"lat\":21.472238465112845,\"lon\":101.5804552574237,\"serializeValue\":\"101.5804552574237|21.472238465112845|31|621.4|23|24|0|1634227318000|0.0|0.0|291\",\"status\":0,\"ts\":1634227318000},{\"alt\":623.9,\"azi\":21,\"bikeSn\":\"816331972\",\"cityId\":291,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":24,\"hoarSpeed\":25,\"lat\":21.472750444040763,\"lon\":101.57963795373489,\"serializeValue\":\"101.57963795373489|21.472750444040763|21|623.9|24|25|0|1634227333000|0.0|0.0|291\",\"status\":0,\"ts\":1634227333000},{\"alt\":2.4,\"azi\":7,\"bikeSn\":\"814814474\",\"cityId\":585,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":1,\"hoarSpeed\":0,\"lat\":21.3843383612137,\"lon\":110.25858160015143,\"serializeValue\":\"110.25858160015143|21.3843383612137|7|2.4|1|0|0|1634227332000|0.0|0.0|585\",\"status\":0,\"ts\":1634227332000},{\"alt\":21.4,\"azi\":123,\"bikeSn\":\"811130620\",\"cityId\":240,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":0,\"hoarSpeed\":0,\"lat\":22.148944917396967,\"lon\":107.98373671923197,\"serializeValue\":\"107.98373671923197|22.148944917396967|123|21.4|0|0|0|1634227333000|0.0|0.0|240\",\"status\":0,\"ts\":1634227333000},{\"alt\":2.9,\"azi\":138,\"bikeSn\":\"810420271\",\"cityId\":398,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":0,\"hoarSpeed\":0,\"lat\":34.83479810289384,\"lon\":117.12217456722058,\"serializeValue\":\"117.12217456722058|34.83479810289384|138|2.9|0|0|0|1634227333000|0.0|0.0|398\",\"status\":0,\"ts\":1634227333000},{\"alt\":17.0,\"azi\":34,\"bikeSn\":\"804722074\",\"cityId\":273,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":18,\"hoarSpeed\":19,\"lat\":30.75524061967154,\"lon\":116.83884665739478,\"serializeValue\":\"116.83884665739478|30.75524061967154|34|17.0|18|19|0|1634227333000|0.0|0.0|273\",\"status\":0,\"ts\":1634227333000},{\"alt\":1461.0,\"azi\":112,\"bikeSn\":\"806110596\",\"cityId\":152,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":0,\"hoarSpeed\":0,\"lat\":24.07389672036578,\"lon\":102.00651493384001,\"serializeValue\":\"102.00651493384001|24.07389672036578|112|1461.0|0|0|0|1634227333000|0.0|0.0|152\",\"status\":0,\"ts\":1634227333000},{\"alt\":22.4,\"azi\":87,\"bikeSn\":\"815615056\",\"cityId\":560,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":0,\"hoarSpeed\":0,\"lat\":38.04856335712258,\"lon\":114.14721838572257,\"serializeValue\":\"114.14721838572257|38.04856335712258|87|22.4|0|0|0|1634227333000|0.0|0.0|560\",\"status\":0,\"ts\":1634227333000},{\"alt\":6.1,\"azi\":163,\"bikeSn\":\"810911716\",\"cityId\":462,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":0,\"hoarSpeed\":0,\"lat\":32.49104854710539,\"lon\":114.04172877721662,\"serializeValue\":\"114.04172877721662|32.49104854710539|163|6.1|0|0|0|1634227332000|0.0|0.0|462\",\"status\":0,\"ts\":1634227332000},{\"alt\":5.5,\"azi\":130,\"bikeSn\":\"810111701\",\"cityId\":530,\"covLat\":0.0,\"covLon\":0.0,\"gpsSpeed\":0,\"hoarSpeed\":0,\"lat\":37.61987351794427,\"lon\":114.61032488528055,\"serializeValue\":\"114.61032488528055|37.61987351794427|130|5.5|0|0|0|1634227333000|0.0|0.0|530\",\"status\":0,\"ts\":1634227333000}]",BikeGpsInfo.class);
        for(BikeGpsInfo bikeGpsInfo:bikeGpsInfos){
            bikeGpsInfoMap.put("test_ebike_latest_"+bikeGpsInfo.getBikeSn(),new ArrayList(1){{add(bikeGpsInfo);}});
            bikeGpsInfoMap.compute("test_ebike_geo_"+bikeGpsInfo.getCityId(),(k,v)->{
                if(v==null){
                    v = new ArrayList();
                }
                v.add(bikeGpsInfo);

                return v;
            });
        }

        CountDownLatch latch = new CountDownLatch(900);
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        for (int i = 0; i < 1000; i++) {
            int j = i;
            executorService.submit(()->{
                try {
                    TimeUnit.SECONDS.sleep(RandomUtils.nextInt(0,5));
                    Object obj = stringRedisShardClient.get("test_ebike_latest_804722074");
                    List<Object> result = redisShardClient.lua(redisScript,keys,bikeGpsInfoMap);
                    for(Object o:result){
                        log.info("result:{}",o);
                    }
                    Object obj3 = stringRedisShardClient.get("test_ebike_latest_77777777");
                    Object obj4 = stringRedisShardClient.get("test_ebike_latest_99999999");
                    latch.countDown();
                } catch (InterruptedException e) {
                    latch.countDown();
                    log.error(e.getMessage(),e);
                }
            });
        }
        latch.await();
        executorService.shutdown();
    }
}
