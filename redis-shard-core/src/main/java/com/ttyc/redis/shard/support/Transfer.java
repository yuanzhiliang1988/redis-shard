package com.ttyc.redis.shard.support;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yuanzl
 * @date 2021/9/9 7:13 下午
 */
@Data
public class Transfer implements Serializable {
    /**
     * 执行服务器IP，非必填，使用手动调用迁移时可不填，暂支持迁一个目标分片，扩容多个分片可指定多个目标服务器处理，充分利用多服务器迁移，否则在一台机器上迁移多个扩容分片会出现性能慢的问题
     */
    private String doServerIp;
    /**
     * 指定源分片索引，非必填，第一个分片索引为0，依次类推
     * 一般用于分片内部节点从一个迁到另一个，待迁移节点很多，对服务器压力大时，或者其中个别节点迁移失败后，可配置该参数指定迁移源节点
     */
    private Integer fromIndex;
    /**
     * 指定从非分片redis外的redis服务迁移,支持多个,JSON格式，如:[{"addresses":127.0.0.1:6379,"password":"123456","serializer":"jdk"}]
     * 一般用于从外部redis迁移到分片上
     */
    private String fromNodes;
    /**
     * 指定目标分片索引，必填，第一个分片索引为0，依次类推
     */
    private Integer toIndex;
    /**
     * 迁移状态，详见：TransferStatusEnum
     * apollo指定迁移状态：redis.shard.transfers[0].status = 3
     * 指定时用于在部分key迁移失败后重复迁移，否则存储的状态为已完成，程序不再迁移
     */
    private Integer status;
    /**
     * 开始时间，记录开始迁移的时间
     */
    private Long startTime;
    /**
     * 已完成的批次索引
     * apollo指定已完成的批次索引：redis.shard.transfers[0].finish-batch-index = 2000003
     */
    private Integer finishBatchIndex;
    /**
     * 最后的批次索引
     * apollo指定最后的批次索引：redis.shard.transfers[0].last-batch-index = 2000003
     */
    private Integer lastBatchIndex;
    /**
     * scan分页数量
     * redis.shard.transfers[0].scan-limit = 5000
     */
    private Integer scanLimit=5000;
    /**
     * 迁移分页数量，暂无用
     * redis.shard.transfers[0].from-index = 3000
     */
    private Integer transLimit=3000;
    /**
     * 指定迁移的key前正则，JSON格式(支持多个)：["test*","*test"]
     * apollo config:redis.shard.transfers[0].tran-key-regex = ["test_*","redis-shard2@@test_*","*_test","*test*"]
     * test_*：代表只dump以test_开头的key，组件默认在key加上应用名前缀，最终匹配key：redis-shard@@test_*
     * redis-shard2@@test_*：表示dump以redis-shard2@@test_*开头的key，由于用户输入了前缀，组件不会默认加前缀，最终匹配key：redis-shard2@@test_*
     * *_test：表示dump以_test结尾的key，最终匹配key：redis-shard@@*_test
     * *test*：表示dump包含test的key，最终匹配key：redis-shard@@*test*
     * redis-shard是引入组件的spring.application.name名
     */
    private String tranKeyRegex;
    /**
     * 迁移时CountDownLatch等待超时时间，防止产生死锁，单位：分，默认：60分钟
     * apollo config:redis.shard.transfers[0].latch-time-out=60
     */
    private Integer latchTimeOut=60;
    /**
     * 分页迁移后休眠时间，防止操作redis太频繁把服务拖死，单位：秒，默认：5秒
     * apollo config:redis.shard.transfers[0].tran-page-sleep-time=5
     */
    private Integer tranPageSleepTime=5;
    /**
     * 是否能dumpkey，默认:true，false时不执行dumpkey
     * apollo config:redis.shard.transfers[0].enable-dump=false/true
     */
    private Boolean enableDump=true;
    /**
     * 是否能迁移，默认:true, false时不执行迁移
     * apollo config:redis.shard.transfers[0].enable-tran=false/true
     */
    private Boolean enableTran=true;

    private Integer queryInfo=0;
    private Integer queryKeys=0;
}
