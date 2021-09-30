package com.ttyc.redis.shard.enums;

import java.util.Arrays;

/**
 * 迁移状态
 */
public enum TransferStatusEnum {
    NO_NEED_TRANSFER(0,"不需要迁移"),
    KEYS_DUMPING(1,"keys存储中"),
    KEYS_DUMPED(2,"keys存储完成"),
    DATA_TRANSFERING(3,"数据迁移中"),
    DATA_TRANSFERED(4,"数据迁移完成");

    private Integer type;
    private String name;

    TransferStatusEnum(Integer type, String name){
        this.type = type;
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

    public static String getNameByType(Integer type) {
        if (null == type) {
            return null;
        }
        return Arrays.stream(TransferStatusEnum.values()).filter(item -> item.getType().equals(type)).findFirst().get().getName();
    }
}
