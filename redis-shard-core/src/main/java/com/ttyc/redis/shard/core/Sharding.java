package com.ttyc.redis.shard.core;

import com.alibaba.fastjson.JSON;
import com.ttyc.redis.shard.constant.ShardConstants;
import com.ttyc.redis.shard.enums.ExceptionMsgEnum;
import com.ttyc.redis.shard.enums.NodeTypeEnum;
import com.ttyc.redis.shard.exception.RedisShardException;
import com.ttyc.redis.shard.support.ShardConfig;
import com.ttyc.redis.shard.support.ShardNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.util.Hashing;
import redis.clients.jedis.util.SafeEncoder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 分片
 * @author yuanzl
 * @date 2021/8/24 11:12 上午
 */
@Slf4j
public class Sharding {
    private TreeMap<Long, String> hashShardNodes;
    private Map<String, ShardNode> shardNodesMap;
    private Hashing hashing;
    private ShardConfig config;

    public Sharding(Set<ShardNode> shardNodes, ShardConfig config){
        this(shardNodes,config,Hashing.MURMUR_HASH);
    }

    public Sharding(Set<ShardNode> shardNodes, ShardConfig config,Hashing hashing){
        this.hashing = hashing;
        this.config = config;
        this.initialize(shardNodes);
    }

    private void initialize(Set<ShardNode> shardNodes) {
        this.hashShardNodes = new TreeMap();
        this.shardNodesMap = new HashMap<>();

        this.setShardNodes(shardNodes);
        log.info("redis shard init success");
    }

    public void cleanShardNode(){
        this.hashShardNodes.clear();
        this.shardNodesMap.clear();
    }

    public void setShardNodes(Set<ShardNode> shardNodes){
        List<ShardNode> shardNodeList = shardNodes.stream().collect(Collectors.toList());
        for(int i = 0; i != shardNodeList.size(); ++i) {
            ShardNode shardNode = shardNodeList.get(i);
            String nodeName = this.getNodeName(shardNode);
            this.shardNodesMap.put(nodeName,shardNode);
            int n;
            for(n = 0; n < (shardNode.getWeight()<=0?1: ShardConstants.VIR_NOTE_NUM * shardNode.getWeight()); ++n) {
                long keyHashIndex = this.getKeyHashIndex(shardNode,n);
                this.hashShardNodes.put(keyHashIndex,nodeName);
            }
        }
//        this.hashShardNodes.forEach((k,v)->{
//            log.info("hashShardNodes,k:{},v:{}",k,v);
//            System.out.println("hashShardNodes,k:"+k+",v:"+v);
//        });
    }

    public void setConfig(ShardConfig config){
        this.config = config;
    }

    public ShardConfig getConfig(){
        return this.config;
    }

    public ShardNode getShardNode(byte[] key) {
        ShardNode shardNode = this.getShard(SafeEncoder.encode(key));
//        log.info("nodeName:{},nextNodeName:{}",shardNode.getName(),shardNode.getNext()==null?"":shardNode.getNext().getName());
        return shardNode;
    }

    public ShardNode getShardNode(String key) {
        ShardNode shardNode = this.getShard(key);
//        log.info("key:{},nodeName:{},nextNodeName:{}",key,shardNode.getName(),shardNode.getNext()==null?"":shardNode.getNext().getName());

        return shardNode;
    }

    public ShardNode getNextShardNode(String key) {
        ShardNode shardNode = this.getNextShard(key);
//        log.info("nextNodeName:{}",shardNode.getName());
        return shardNode;
    }

    private SortedMap<Long, String> getKeyTailMap(byte[] key){
        Long keyIndex = this.hashing.hash(key);
        SortedMap<Long, String> tail = this.hashShardNodes.tailMap(keyIndex);
        if(tail.isEmpty()){
            tail = this.hashShardNodes;
        }
        SortedMap<Long, String> tailMap = new TreeMap<>();
        String nodeName = tail.get(tail.firstKey());
        tailMap.put(tail.firstKey(),nodeName);

        boolean hasNext = this.forEachTailMap(tail,tailMap,nodeName);
        if(!hasNext){
            this.forEachTailMap(this.hashShardNodes,tailMap,nodeName);
        }

//        log.info("key:{},keyIndex:{},tailMap:{}",SafeEncoder.encode(key),keyIndex, JSON.toJSONString(tailMap));
//        System.out.println("key:"+SafeEncoder.encode(key)+",keyIndex:"+keyIndex+",tailMap:"+JSON.toJSONString(tailMap));
        return tailMap;
    }

    private boolean forEachTailMap(SortedMap<Long, String> tail,SortedMap<Long, String> tailMap,String nodeName){
        Iterator<Map.Entry<Long, String>> entries = tail.entrySet().iterator();
        boolean hasNext=false;
        while (entries.hasNext()) {
            Map.Entry<Long, String> entry = entries.next();
            if(!entry.getValue().equals(nodeName) && !this.shardNodesMap.get(entry.getValue()).isGray()){
                tailMap.put(entry.getKey(), entry.getValue());
                hasNext=true;
                break;
            }
        }

        return hasNext;
    }

    private ShardNode getShard(String key) {
        SortedMap<Long, String> nodeMap = this.getKeyTailMap(SafeEncoder.encode(this.getKeyTag(key)));
        String shardName = nodeMap.get(nodeMap.firstKey());
        ShardNode shardNode = this.shardNodesMap.get(shardName);
        if(shardNode.isGray() || shardNode.isDoubleWriter()){
            nodeMap.remove(nodeMap.firstKey());
            ShardNode nextShardNode = this.shardNodesMap.get(nodeMap.get(nodeMap.firstKey()));
            shardNode.setNext(nextShardNode);
        }else{
            shardNode.setNext(null);
        }
        nodeMap.clear();

        return shardNode;
    }

    private ShardNode getNextShard(String key) {
        String shardName = this.getNextShard(SafeEncoder.encode(this.getKeyTag(key)));
        ShardNode shardNode = this.shardNodesMap.get(shardName);
        return shardNode;
    }

    /**
     * 获取下一个分片
     * @param key
     * @return
     */
    private String getNextShard(byte[] key) {
        SortedMap<Long, String> tail = this.getKeyTailMap(key);
        tail.remove(tail.firstKey());

        String shardName = tail.get(tail.firstKey());
        tail.clear();

        return shardName;
    }

    /**
     * 配置正则key的表达，暂不支持在客户端自由定义正则，
     * 客户端自定义正则在扩容迁移时获取不到正则表达式，导致迁移数据不准确，因此不支持
     * @param key
     * @return
     */
    private String getKeyTag(String key) {
        if (this.config != null && !CollectionUtils.isEmpty(this.config.getKeyRegex())) {
            for(String regex:this.config.getKeyRegex()){
                Pattern pattern = Pattern.compile(regex);
                Matcher m = pattern.matcher(key);
                if (m.find()) {
                    key = m.group(1);
                    break;
                }
            }
//            log.info("getKeyTag:k:{}",key);
        }

        return key;
    }

    public static void main(String[] args) {
//        getPatternTest("test_807310902");
        ShardConfig shardConfig = new ShardConfig();
        Set<ShardNode> shardNodes = new HashSet<>();
        shardNodes.add(new ShardNode(){{
            setName("SHARD-NODE-SINGLE-127.0.0.1:6379");
//            setGray(true);
//            setWeight(0);
        }});
        shardNodes.add(new ShardNode(){{
            setName("SHARD-NODE-SINGLE-127.0.0.1:6380");
//            setWeight(0);
        }});
        shardNodes.add(new ShardNode(){{
            setName("SHARD-NODE-SINGLE-127.0.0.1:6381");
//            setDoubleWriter(true);
//            setWeight(0);
        }});
        shardNodes.add(new ShardNode(){{
            setName("SHARD-NODE-SINGLE-127.0.0.1:6379");
//            setWeight(0);
        }});
        shardNodes.add(new ShardNode(){{
            setName("SHARD-NODE-SINGLE-127.0.0.1:6380");
//            setWeight(0);
        }});
        Sharding sharding = new Sharding(shardNodes,shardConfig);
        /*sharding.getShardNode(getPatternTest("test_807310902"));
        sharding.getNextShardNode("807310902");
        sharding.getShardNode(getPatternTest("test_909510049"));
        sharding.getShardNode(getPatternTest("test_190220007"));
        /*sharding.getShardNode("808818406");
        sharding.getShardNode("809510047");
        sharding.getShardNode("909510049");*/

        //测试分布均匀
        // 生成随机数进行测试
//        testLoadBalance(sharding);

        //注：hashShardNodes是按key升序排序
        log.info("shardNodes:{}",JSON.toJSONString(sharding.hashShardNodes));
        //获取子集合，左闭(>=)，右开(<)区间
        SortedMap subMap= sharding.hashShardNodes.subMap(-3389891726082727930L,-3260630043081295657L);
        System.out.println("subMap:"+JSON.toJSONString(subMap));
        //取到的当前key的上个节点 <
        Map.Entry<Long,String> lowerEntry = sharding.hashShardNodes.lowerEntry(-3389891726082727930L);
        System.out.println("lowerEntry:"+JSON.toJSONString(lowerEntry));
        //取到的当前key的下个节点 >
        Map.Entry<Long,String> higherEntry = sharding.hashShardNodes.higherEntry(-3389891726082727930L);
        System.out.println("higherEntry:"+JSON.toJSONString(higherEntry));
        //返回与大于或等于给定键的最大键关联的键值映射，如果没有这样的键，则返回null。 返回：-3389891726082727930
        Map.Entry<Long,String> ceilingEntry = sharding.hashShardNodes.ceilingEntry(9222106596588260126L);
        System.out.println("ceilingEntry:"+JSON.toJSONString(ceilingEntry));
        //返回与小于或等于给定键的最大键关联的键值映射，如果没有这样的键，则返回null。返回：-3447372829050858745
        Map.Entry<Long,String> floorEntry = sharding.hashShardNodes.floorEntry(-3389891726082727931L);
        System.out.println("floorEntry:"+JSON.toJSONString(floorEntry));
    }

    private static void testLoadBalance(Sharding sharding){
        Map<String, Integer> resMap = new HashMap<>();
        for (int i = 0; i < 1000000; i++) {
            Integer widgetId = (int)(Math.random() * 10000);
            ShardNode server = sharding.getShardNode(widgetId.toString());
            if (resMap.containsKey(server.getName())) {
                resMap.put(server.getName(), resMap.get(server.getName()) + 1);
            } else {
                resMap.put(server.getName(), 1);
            }
        }

        resMap.forEach(
            (k, v) -> {
                System.out.println("shard " + k + ": " + v + "(" + v/10000.0D +"%)");
            }
        );
    }

    private static String getPatternTest(String key){
        //测试以test_为前缀的key,把key的前缀截下来，作为新key，使得该key可以固定hash到分片，一般用途对一批key路由到同一分片，方便批量查询，不需要跨分片查询
        Matcher m = Pattern.compile("test_").matcher(key);
        if (m.find()) {
            key = m.group(1);
        }
        System.out.println("Sharding.main:"+key);

        return key;
    }

    public Map<String, ShardNode> getShardNodeMap(){
        return this.shardNodesMap;
    }

    public ShardNode getShardNodeByIndex(Integer index){
        return this.shardNodesMap.values().stream().filter(n->n.getIndex().equals(index)).findFirst().orElseThrow(()->new RedisShardException(ExceptionMsgEnum.NODE_NOT_EXISTS));
    }

    public void setShardNodesMap(String key,ShardNode val){
        this.shardNodesMap.put(key,val);
    }

    public LinkedList<ShardNode> getShardNodeList(){
        Collection<ShardNode> shardNodes = this.shardNodesMap.values().stream().sorted(
                Comparator.comparingInt(ShardNode::getIndex)
        ).collect(Collectors.toList());
        LinkedList<ShardNode> nodes = new LinkedList(shardNodes);

        return nodes;
    }

    /**
     * 获取指定节点下所有下一个非灰度节点
     * @param shardNode
     * @return
     */
    public Map<String, ShardNode> getAllNextShardNode(ShardNode shardNode) {
        String nodeName = this.getNodeName(shardNode);

        Map<String, ShardNode> nextShardNodeMap = new HashMap<>();

        int n;
        for(n = 0; n < (shardNode.getWeight()<=0?1:ShardConstants.VIR_NOTE_NUM * shardNode.getWeight()); ++n) {
            long keyHashIndex = this.getKeyHashIndex(shardNode,n);
            //取下一个节点
            ShardNode nextNode = getNextShardNode(nodeName,keyHashIndex+1);
            if(nextNode!=null){
                nextShardNodeMap.putIfAbsent(nextNode.getName(),nextNode);
            }
        }

        return nextShardNodeMap;
    }

    private ShardNode getNextShardNode(String currNodeName,long keyHashIndex){
        SortedMap<Long, String> tail = this.hashShardNodes.tailMap(keyHashIndex);
        if(tail.isEmpty()){
            tail = this.hashShardNodes;
        }
        String nodeName = tail.get(tail.firstKey());
        ShardNode shardNode = null;
        if(!currNodeName.equals(nodeName) && !this.shardNodesMap.get(nodeName).isGray()){
            return this.shardNodesMap.get(nodeName);
        }
        shardNode = forEachTailMap(tail,currNodeName);
        if(shardNode==null){
            shardNode = forEachTailMap(this.hashShardNodes,currNodeName);
        }

        return shardNode;
    }

    private ShardNode forEachTailMap(SortedMap<Long, String> tail,String nodeName){
        Iterator<Map.Entry<Long, String>> entries = tail.entrySet().iterator();

        while (entries.hasNext()) {
            Map.Entry<Long, String> entry = entries.next();
            if(!entry.getValue().equals(nodeName) && !this.shardNodesMap.get(entry.getValue()).isGray()){
                return this.shardNodesMap.get(entry.getValue());
            }
        }

        return null;
    }

    private long getKeyHashIndex(ShardNode shardNode,Integer n){
        String nodeName = this.getNodeName(shardNode);
        String hashKey = "";
        if (shardNode.getName() == null) {
            hashKey = nodeName + "-" + n;
        }else{
            hashKey = nodeName + "*" + shardNode.getWeight() + n;
        }

        return this.hashing.hash(hashKey);
    }

    private String getNodeName(ShardNode shardNode){
        return shardNode.getName()==null?("SHARD-NODE-"+ NodeTypeEnum.getCodeByType(shardNode.getNodeType())+"-"+shardNode.getAddresses()):shardNode.getName();
    }

    public String getShardNodeAddress(String key){
        long hashIndex = this.hashing.hash(key);
        Map.Entry<Long,String> ceilingEntry = this.hashShardNodes.ceilingEntry(hashIndex);
        if(ceilingEntry==null){
            //没有获取到值说明已经到了最大值，按hash是环的原理，取第一个
            ceilingEntry = this.hashShardNodes.firstEntry();
        }
        String[] nodeAddresses = StringUtils.split(ceilingEntry.getValue(),"-");
        String nodeAddress = nodeAddresses[nodeAddresses.length-1];

        return nodeAddress;
    }

    private Map<String,List<String>> getShardNodeMap(List<String> keys){
        return this.getShardNodeMap(keys,null);
    }

    private Map<String,List<String>> getShardNodeMap(List<String> keys,List<String> excludeNodes){
        Map<String,List<String>> shardNodeMap = new HashMap<>();
        for(String key:keys){
            String nodeAddress = this.getShardNodeAddress(key);
            if(!CollectionUtils.isEmpty(excludeNodes) && excludeNodes.contains(nodeAddress)){
                //过滤掉要排队的node
                continue;
            }
            shardNodeMap.compute(nodeAddress,(k,v)->{
                if(v==null){
                    v = new ArrayList<>();
                }
                v.add(key);

                return v;
            });
        }

        return shardNodeMap;
    }
}
