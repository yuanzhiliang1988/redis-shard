package com.ttyc.redis.shard.transfer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttyc.redis.shard.ShardBeanFactory;
import com.ttyc.redis.shard.constant.ShardConstants;
import com.ttyc.redis.shard.core.RedisShardClient;
import com.ttyc.redis.shard.core.ShardNodeFactory;
import com.ttyc.redis.shard.core.Sharding;
import com.ttyc.redis.shard.core.XRedisConnectionFactory;
import com.ttyc.redis.shard.enums.ExceptionMsgEnum;
import com.ttyc.redis.shard.enums.SerializerTypeEnum;
import com.ttyc.redis.shard.enums.TransferStatusEnum;
import com.ttyc.redis.shard.exception.RedisShardException;
import com.ttyc.redis.shard.support.Node;
import com.ttyc.redis.shard.support.ShardNode;
import com.ttyc.redis.shard.support.Transfer;
import com.ttyc.redis.shard.utils.IpUtils;
import com.ttyc.redis.shard.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author yuanzl
 * @date 2021/9/9 7:40 下午
 */
@Slf4j
public class TransferAction{
    private static  final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 业务端调用迁移
     * @param transfers
     */
    public void client(List<Transfer> transfers) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        //暂支持迁一个目标分片，扩容多个分片可指定LocalIp多个目标服务器处理，充分利用多服务器迁移，否则在一台机器上迁移多个扩容分片会出现性能慢的问题
        Transfer transfer = transfers.stream().findFirst().get();
        log.info("transfer info:{}", JSON.toJSONString(transfer));
        RedisShardClient redisShardClient = (RedisShardClient) SpringContextUtils.getBean("redisShardClient");
        Sharding sharding = (Sharding) SpringContextUtils.getBean("sharding");
        ShardNode toShardNode = redisShardClient.getShardNode(transfer.getToIndex());
        if(toShardNode==null){
            throw new RedisShardException(ExceptionMsgEnum.NODE_NOT_EXISTS);
        }
        //获取迁移目标节点
        List<ShardNode> fromNodes = this.getFromNodes(transfer,redisShardClient,toShardNode);
        if(CollectionUtils.isEmpty(fromNodes)){
            return;
        }
        //存储要迁移的key
        this.dumpKeys(redisShardClient,transfer,toShardNode,fromNodes,sharding);
        //迁移数据
        this.dataTransfer(redisShardClient,transfer,toShardNode,fromNodes);

        //使用完后回收
        redisShardClient = null;
        transfer = null;
        toShardNode = null;
        fromNodes.clear();
    }

    /**
     * 监听apollo配置变更迁移
     *
     * @param transfers
     */
    public void exec(List<Transfer> transfers) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        log.info("start transfer.....");
        if(CollectionUtils.isEmpty(transfers)){
            return;
        }
        String localIp = IpUtils.getLocalIp();
        transfers = transfers.stream().filter(p->p.getDoServerIp().equals(localIp)).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(transfers)){
            log.info("end transfer.....本服务不进行迁移操作");
            return;
        }
        this.client(transfers);

        log.info("end transfer.....");
    }

    /**
     * 获取迁移目标节点
     * @param transfer
     * @param redisShardClient
     * @param toShardNode
     * @return
     */
    private List<ShardNode> getFromNodes(Transfer transfer,RedisShardClient redisShardClient,ShardNode toShardNode) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<ShardNode> fromNodes = new ArrayList<>();

        List<String> tranKeyRegexs = null;
        if(StringUtils.isNotBlank(transfer.getTranKeyRegex())){
            tranKeyRegexs = JSONArray.parseArray(transfer.getTranKeyRegex(),String.class);
        }else{
            tranKeyRegexs = new ArrayList<>(1);
            //默认前缀
            tranKeyRegexs.add(redisShardClient.getKeyPrefix()+"*");
        }
        if(transfer.getFromIndex()!=null){
            ShardNode shardNode = redisShardClient.getShardNode(transfer.getFromIndex());
            if(shardNode==null){
                throw new RedisShardException(ExceptionMsgEnum.NODE_NOT_EXISTS);
            }
            shardNode.setTranKeyRegex(tranKeyRegexs);
            fromNodes.add(shardNode);
        }else if(StringUtils.isNotBlank(transfer.getFromNodes())){
            List<Node> nodes;
            if(transfer.getFromNodes().startsWith("[{") && transfer.getFromNodes().endsWith("}]")){
                nodes = JSONArray.parseArray(transfer.getFromNodes(),Node.class);
            }else{
                nodes = new ArrayList<>(1);
                nodes.add(JSONObject.parseObject(transfer.getFromNodes(),Node.class));
            }
            ShardNodeFactory shardNodeFactory = new ShardNodeFactory();
            XRedisConnectionFactory xRedisConnectionFactory = new XRedisConnectionFactory();
            ShardBeanFactory shardBeanFactory = (ShardBeanFactory)SpringContextUtils.getBean("shardBeanFactory");
            for (int i = 0; i < nodes.size(); i++) {
                ShardNode shardNode = shardNodeFactory.createShardNode(nodes.get(i),null,i,xRedisConnectionFactory);
                String serializerType = transfer.getFromNodes().contains("serializer")?nodes.get(i).getSerializer(): SerializerTypeEnum.STRING.getType();
                shardNodeFactory.setSerializer(serializerType,shardNode,shardBeanFactory);

                //外部redis
                shardNode.setOuter(true);
                shardNode.setTranKeyRegex(tranKeyRegexs);
                shardNode.afterPropertiesSet();

                fromNodes.add(shardNode);
            }
        }else{
            Map<String, ShardNode> nextShardNodeMap = redisShardClient.getAllNextShardNode(toShardNode);
            if(nextShardNodeMap.isEmpty()){
                log.info("shardNodeIndex:{},没有下个节点，不迁移",transfer.getToIndex());
                return null;
            }
            fromNodes.addAll(nextShardNodeMap.values());
            List<String> tranKeyRegexs1 = new ArrayList<>(tranKeyRegexs);
            fromNodes.stream().peek(p->{
                p.setTranKeyRegex(tranKeyRegexs1);
            }).collect(Collectors.toList());
        }

        return fromNodes;
    }

    /**
     * 存储要迁移的key
     * @param transfer
     */
    private void dumpKeys(RedisShardClient redisShardClient,Transfer transfer,ShardNode toShardNode,List<ShardNode> fromNodes,Sharding sharding){
        int scanLimit = transfer.getScanLimit();
        String keyPrefix = redisShardClient.getKeyPrefix();
        for(ShardNode shardNode:fromNodes){
            Object tranInfoObj = toShardNode.opsForHash().get(redisShardClient.getKeyPrefix()+ShardConstants.REDIS_TRANSFER_INFO,shardNode.getIndex().toString());
            if(tranInfoObj!=null){
                Transfer transferFromRedis = tranInfoObj instanceof LinkedHashMap?objectMapper.convertValue(tranInfoObj, new TypeReference<Transfer>(){}):(Transfer)tranInfoObj;
                if(!transferFromRedis.getStatus().equals(TransferStatusEnum.NO_NEED_TRANSFER.getType())
                        && !transferFromRedis.getStatus().equals(TransferStatusEnum.DATA_TRANSFERED.getType())){
                    continue;
                }
            }

            List<String> batchKeys = new ArrayList<>(scanLimit);
            Integer scanIndex = Integer.parseInt(shardNode.getIndex()+ShardConstants.REDIS_TRANSFER_KEYS_SCORE_FILL);
            AtomicInteger atomicInteger = new AtomicInteger(scanIndex);

            List<String> tranKeyRegexs = shardNode.getTranKeyRegex();
            for (String tranKeyRegex:tranKeyRegexs) {
                redisShardClient.scan(shardNode, tranKeyRegex, Long.parseLong(scanLimit+""),item -> {
                    //符合条件的key
                    String key = toShardNode.getStringSerializer().deserialize((byte[])item).toString();
                    String nodeAddress = sharding.getShardNodeAddress(key);
                    log.info("fromAddress:{},key:{},toAddress:{}",shardNode.getAddresses(),key,nodeAddress);
                    //计算key的分片地址，如果是分片到目标分片，则保存，否则不存储
                    if(nodeAddress.equals(toShardNode.getAddresses())){
                        batchKeys.add(key);
                    }
                    if(batchKeys.size()>=scanLimit){
                        log.info("batchKeys:{}",JSON.toJSONString(batchKeys));
                        //分批插入
                        toShardNode.opsForZSet().add(keyPrefix + ShardConstants.REDIS_TRANSFER_KEYS,batchKeys,atomicInteger.incrementAndGet());
                        batchKeys.clear();
                    }
                });
            }
            if(!CollectionUtils.isEmpty(batchKeys)){
                log.info("batchKeys:{}",JSON.toJSONString(batchKeys));
                //分批插入
                toShardNode.opsForZSet().add(keyPrefix + ShardConstants.REDIS_TRANSFER_KEYS,batchKeys,atomicInteger.incrementAndGet());
                batchKeys.clear();
            }

            //存入迁移信息
            transfer.setStartTime(System.nanoTime());
            transfer.setStatus(atomicInteger.get()>scanIndex?TransferStatusEnum.KEYS_DUMPED.getType():TransferStatusEnum.NO_NEED_TRANSFER.getType());
            transfer.setLastBatchIndex(atomicInteger.get());
            transfer.setFinishBatchIndex(scanIndex+1);
            transfer.setFromIndex(shardNode.getIndex());
            toShardNode.opsForHash().put(keyPrefix + ShardConstants.REDIS_TRANSFER_INFO,shardNode.getIndex().toString(),transfer);
        }
    }

    private void dataTransfer(RedisShardClient redisShardClient,Transfer transfer,ShardNode toShardNode,List<ShardNode> fromNodes){
        String keyPrefix = redisShardClient.getKeyPrefix();
        Map<String, Transfer> transferMap = toShardNode.opsForHash().entries(keyPrefix + ShardConstants.REDIS_TRANSFER_INFO);
        if(transferMap==null || transferMap.isEmpty()){
            log.info("没有需要迁移的数据");
            return;
        }

        Map<Integer,ShardNode> fromNodeMap = fromNodes.stream().collect(Collectors.toMap(p->p.getIndex(),p->p));
        //计数器
        CountDownLatch latch = new CountDownLatch(transferMap.size());
        //最多开4个线程迁移
        ExecutorService executorService = Executors.newFixedThreadPool(transferMap.size()>4?4: transferMap.size());
        //遍历迁移map
        Iterator<Map.Entry<String, Transfer>> iterator = transferMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, Transfer> entry = iterator.next();
            String fromNodeIndex = entry.getKey();
            ShardNode fromShardNode = fromNodeMap.get(Integer.parseInt(fromNodeIndex));
            Transfer fromNodeTransfer = JSONObject.parseObject(JSON.toJSONString(entry.getValue()),Transfer.class);
            if(fromNodeTransfer.getStatus()<TransferStatusEnum.KEYS_DUMPED.getType() || fromNodeTransfer.getStatus()>=TransferStatusEnum.DATA_TRANSFERED.getType()){
                log.info("节点:{},迁移状态不正常,状态:{}",fromShardNode.getAddresses(),TransferStatusEnum.getNameByType(fromNodeTransfer.getStatus()));
                latch.countDown();
                continue;
            }
            Integer finishBatchIndex = fromNodeTransfer.getFinishBatchIndex();
            Integer to = fromNodeTransfer.getLastBatchIndex();
            //开启线程迁移
            executorService.execute(()->{
                Boolean isFinish = true;
                for (int from = finishBatchIndex; from <= to; ++from) {
                    //每次只取一页处理,防止一次查出的数据太多导致内存溢出及代码卡死
                    List<String> tranKeys = this.getTrasferKeys(keyPrefix,toShardNode,from,from);
                    if(CollectionUtils.isEmpty(tranKeys)){
                        log.info("{}没有获取到数据",from);
                        continue;
                    }
                    //处理迁移
                    isFinish = this.doTransfer(tranKeys,fromShardNode,toShardNode, keyPrefix,from,redisShardClient)?isFinish:false;

                    fromNodeTransfer.setFinishBatchIndex(from);
                    fromNodeTransfer.setStatus(TransferStatusEnum.DATA_TRANSFERING.getType());
                    //迁移中的更新
                    if(from<to){
                        //更新迁移状态
                        toShardNode.opsForHash().put(keyPrefix+ShardConstants.REDIS_TRANSFER_INFO,fromNodeIndex,fromNodeTransfer);
                    }
                }
                if(isFinish) {
                    fromNodeTransfer.setStatus(TransferStatusEnum.DATA_TRANSFERED.getType());
                }
                //更新最终迁移状态
                toShardNode.opsForHash().put(keyPrefix+ShardConstants.REDIS_TRANSFER_INFO,fromNodeIndex,fromNodeTransfer);
                log.info("fromShardNode:{},处理完成,状态：{}",fromShardNode.getAddresses(),TransferStatusEnum.getNameByType(fromNodeTransfer.getStatus()));
                latch.countDown();
            });
        }
        try {
            //等待线程结束
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //关闭线程
        executorService.shutdown();
    }

    private List<String> getTrasferKeys(String keyPrefix,ShardNode toShardNode,Integer from,Integer to){
        Set<ZSetOperations.TypedTuple<Object>> typedTuples = toShardNode.opsForZSet().rangeByScoreWithScores(keyPrefix + ShardConstants.REDIS_TRANSFER_KEYS,from,to);
        if(CollectionUtils.isEmpty(typedTuples)){
            return null;
        }
        ZSetOperations.TypedTuple<Object> typedTuple = typedTuples.stream().findFirst().get();
        log.info("from:{},to:{},getTrasferKeys:{}",from,to,JSON.toJSONString(typedTuple));

        List<String> keys = (List)typedTuple.getValue();

        return keys;
    }

    private Boolean doTransfer(List<String> tranKeys,ShardNode fromShardNode,ShardNode toShardNode,String keyPrefix,Integer from,RedisShardClient redisShardClient){
        Set<String> transferExpSet = new HashSet<>();
        //由于需要判断value是否被变更了，因此不能批量处理，只能一条条实时校验
        for(String key:tranKeys){
            try{
                DataType dataType = fromShardNode.type(key);
                /**
                 * 存储的key，迁移后的key需要拼上项目名+@@前缀
                 *
                 * 1.如果key以本项目的key前缀开关，则直接使用原key存储
                 * 2、否则，如果key中包含@@则把前缀删除后再拼上本项目key前缀
                 * 3、否则，直接用项目key前缀拼接key
                 */
                String toKey = key.startsWith(keyPrefix)?key:keyPrefix+(redisShardClient.getShortKey(key));

                //1、原节点数据查询不到，则不迁移
                //2、
                switch (dataType){
                    case STRING:
                        Object string = fromShardNode.opsForValue().get(key);
                        if(string!=null){
                            toShardNode.opsForValue().setIfAbsent(toKey,string);
                        }
                        break;
                    case HASH:
                        Map<Object,Object> hashMap = fromShardNode.opsForHash().entries(key);
                        //此处会有原子性问题，可能会导致新值被覆盖
                        if(hashMap!=null && !hashMap.isEmpty() && !toShardNode.hasKey(toKey)){
                            toShardNode.opsForHash().putAll(toKey,hashMap);
                        }
                        break;
                    case LIST:
                        List<Object> list = fromShardNode.opsForList().range(key,0,-1);
                        if(!CollectionUtils.isEmpty(list) && !toShardNode.hasKey(toKey)){
                            toShardNode.opsForList().rightPushAll(toKey,list);
                        }
                        break;
                    case SET:
                        Set<Object> set = fromShardNode.opsForSet().members(key);
                        if(!CollectionUtils.isEmpty(set) && !toShardNode.hasKey(toKey)){
                            toShardNode.opsForSet().add(toKey,set);
                        }
                        break;
                    case ZSET:
                        Set zset = fromShardNode.opsForZSet().rangeByScoreWithScores(key,0,-1);
                        if(!CollectionUtils.isEmpty(zset) && !toShardNode.hasKey(toKey)){
                            toShardNode.opsForZSet().add(toKey,zset);
                        }
                        break;
                    default:
                        log.warn("key:{},数据类型错误:{}",key,dataType);
                        break;
                }

                //删除原节点上的key
                if(!toShardNode.isGray() && !toShardNode.isDoubleWriter()){
                    fromShardNode.delete(key);
                }
            }catch (Exception e){
                transferExpSet.add(key);
                log.error(key+"迁移失败",e);
            }
        }

        //删除原来的
        toShardNode.opsForZSet().removeRangeByScore(keyPrefix + ShardConstants.REDIS_TRANSFER_KEYS,from,from);
        if(!CollectionUtils.isEmpty(transferExpSet)){
            //把没有迁移完成的key重新增加进去
            toShardNode.opsForZSet().add(keyPrefix + ShardConstants.REDIS_TRANSFER_KEYS,transferExpSet,from);
            return false;
        }

        return true;
    }
}
