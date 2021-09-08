redis分片项目

技术文档：http://wiki.corp.ttyongche.com:8360/pages/viewpage.action?pageId=50115913

使用说明：

1、引入jar包
 
私服地址：
<repository>
   <id>nexus</id>
   <name>songguo public nexus</name>
   <url>http://maven.corp.songguo7.com:8360/nexus/content/groups/public</url>
</repository>
 
jar包依赖
<dependency>
   <groupId>com.ttyc.redis.shard</groupId>
   <artifactId>redis-shard-spring-boot-starter</artifactId>
   <version>1.0-SNAPSHOT</version>
</dependency>
 

2、配置properties（使用apollo可监听配置变更）

#分片是否可用，默认:true
redis.shard.enabled = true
#apollo配置变更监听是否可用（根据变更动态刷新分片信息），默认:true
redis.shard.listener.enabled = true
#redis分片：单分片支持单机、哨兵、集群，必填，同分片不同属性使用:分隔,无密码可不填,哨兵和集群的多地址使用逗号分隔
redis.shard.nodes[0].addresses=10.9.198.84:6379
#分片节点是否灰度，灰度包含双写，双写不包含灰度，非必填，不配默认：false
redis.shard.nodes[0].gray=true
redis.shard.nodes[1].addresses=10.100.102.27:6379
#分片节点密码，非必填
redis.shard.nodes[1].password=PiC8Ou_mZSU7
redis.shard.nodes[2].addresses=10.9.198.84:6380
redis.shard.nodes[3].addresses=10.9.188.145:6379
#分片节点是否双写，灰度包含双写，双写不包含灰度，非必填，不配默认:false,
redis.shard.nodes[3].double-writer=true
#redis序列化方式：jdk,string、fastjson、jackson、kryo、protostuff，非必填，不配默认：jdk
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
     
    //存入值
    redisShardClient.set(key, user);
    //获取值
    User user1 = (User)redisShardClient.get(key);
    System.out.println("user1:" + JSON.toJSONString(user1));
 
    return "ok";
}
