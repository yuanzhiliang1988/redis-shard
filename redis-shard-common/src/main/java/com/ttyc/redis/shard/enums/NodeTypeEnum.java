package com.ttyc.redis.shard.enums;

import java.util.Arrays;

/**
 * 分片节点类型
 */
public enum NodeTypeEnum {
    SINGLE(1,"SINGLE","单例"),
    SENTINEL(2,"SENTINEL","哨兵"),
    CLUSTER(3,"CLUSTER","集群");

    private Integer type;
    private String code;
    private String name;

    NodeTypeEnum(Integer type,String code,String name){
        this.type = type;
        this.code = code;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public static NodeTypeEnum getEnum(Integer type) {
        if (null == type) {
            return null;
        }
        return Arrays.stream(NodeTypeEnum.values()).filter(item -> item.getType().equals(type)).findFirst().get();
    }

    public static String getCodeByType(Integer type) {
        if (null == type) {
            return null;
        }
        return Arrays.stream(NodeTypeEnum.values()).filter(item -> item.getType().equals(type)).findFirst().get().getCode();
    }
}
