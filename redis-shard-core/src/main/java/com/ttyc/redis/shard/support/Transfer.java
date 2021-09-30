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
     * 源分片索引，非必填，第一个分片索引为0，依次类推
     */
    private Integer fromIndex;
    /**
     * 指定从非分片redis外的redis服务迁移,支持多个,JSON格式，如:[{"addresses":127.0.0.1:6379,"password":"123456","serializer":"jdk"}]
     */
    private String fromNodes;
    /**
     * 目标分片索引，必填，第一个分片索引为0，依次类推
     */
    private Integer toIndex;
    /**
     * 迁移状态，详见：TransferStatusEnum
     */
    private Integer status;
    /**
     * 开始时间，记录开始迁移的时间
     */
    private Long startTime;
    /**
     * 已完成的批次索引
     */
    private Integer finishBatchIndex;
    /**
     * 最后的批次索引
     */
    private Integer lastBatchIndex;
    /**
     * scan分页数量
     */
    private Integer scanLimit=5;
    /**
     * 迁移分页数量
     */
    private Integer transLimit=5;
    /**
     * 指定迁移的key前正则，JSON格式(支持多个)：["test","*test"]
     */
    private String tranKeyRegex;
}
