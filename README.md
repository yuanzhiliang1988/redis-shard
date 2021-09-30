一、redis-shard解决原redis集群的问题
1、支持按key自由分片、redis模式可支持多种模式
2、可无限扩展分片
3、解决主从、集群不能无限大QPS问题，通过加分片或者增加应用节点把QPS分散到各分片，理论上可支持无限大的QPS
4、支持分片扩容时在线迁移数据，且不影响访问性能，redis集群在线扩容数据冲突是会报错，此中间件不会报错


二、Redis分片组件使用说明

1、引入jar包
<dependency>
    <groupId>com.ttyc.redis.shard</groupId>
    <artifactId>redis-shard-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

2、配置properties（apollo,非必选，如无apollo则默认使用本地配置）
#分片是否可用，默认:true
redis.shard.enabled = true
#apollo配置变更监听是否可用（根据变更动态刷新分片信息），默认:true
redis.shard.listener.enabled = true
#redis分片：单分片支持单机、哨兵、集群，必填，同分片不同属性使用:分隔,无密码可不填,哨兵和集群的多地址使用逗号分隔
redis.shard.nodes[0].addresses=127.0.0.1:6379
#分片节点是否灰度，灰度包含双写，双写不包含灰度，非必填，不配默认：false
redis.shard.nodes[0].gray=true
redis.shard.nodes[1].addresses=127.0.0.1:6380
#分片节点密码，非必填
redis.shard.nodes[1].password=admin
redis.shard.nodes[2].addresses=127.0.0.1:6381
redis.shard.nodes[3].addresses=127.0.0.1:6382
#分片节点是否双写，灰度包含双写，双写不包含灰度，非必填，不配默认:false,
redis.shard.nodes[3].double-writer=true
#redis序列化方式：jdk,string、fastjson、jackson、kryo、protostuff，非必填，不配默认：jdk(使用jdk序列化的对象都要实现Serializable接口，否则会报序列化错误)
redis.shard.config.serializer=jdk
#存储key分片正则表达式，正则匹配上的key分配在同一个分片，支持多正则匹配，非必填
redis.shard.config.key-regex[0]=(test_)
redis.shard.config.key-regex[1]=(test2_)
#jedis连接池配置
#最多可以建立10个连接了
redis.shard.jedis.pool.max-total=10
#10s获取不到连接池的连接，直接报错Could not get a resource from the pool
redis.shard.jedis.pool.max-wait=10000
#最小空闲连接为5个
redis.shard.jedis.pool.min-idle=5
#获取连接前是否测试连接可用状态，默认false，不建议配置成true
redis.shard.jedis.pool.test-on-borrow=false
#返回数据时是否测试连接可用状态，默认false，不建议配置成true
redis.shard.jedis.pool.test-on-return=false
#测试空闲连接是否可用，默认true
redis.shard.jedis.pool.test-while-idle=true
#读超时时间,默认2000ms
redis.shard.jedis.pool.read-timeout=2000
#连接超时时间,默认2000ms
redis.shard.jedis.pool.connect-timeout=2000


3、使用
@Resource
private RedisShardClient redisShardClient;

@RequestMapping(value = "/testRedisShard",method = {RequestMethod.POST})
public String testRedisShard(@RequestBody JSONObject jsonObject) {
    String key = jsonObject.getString("key");
    User user = new User();
    user.setName("张三");
    user.setAge(18);

    redisShardClient.set(key, user);

    User user1 = (User)redisShardClient.get(key);
    System.out.println("user1:" + JSON.toJSONString(user1));
 
    return "ok";
}