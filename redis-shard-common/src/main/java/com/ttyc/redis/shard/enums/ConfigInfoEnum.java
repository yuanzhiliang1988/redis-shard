package com.ttyc.redis.shard.enums;

import java.util.Arrays;

/**
 * 配置信息枚举
 */
public enum ConfigInfoEnum {
    ADDRESSES("addresses","分片地址","addresses"),
    PASSWORD("password","分片密码","node"),
    GRAY("gray","灰度","node"),
    DOUBLE_WRITER("double-writer","双写","node"),
    SERIALIZER("serializer","双写","config"),
    KEY_REGEX("key-regex","双写","config"),
    POOL("pool","连接池","pool"),
    TRANSFER("transfer","迁移","transfer");

    private String code;
    private String name;
    private String type;

    ConfigInfoEnum(String code, String name,String type){
        this.code = code;
        this.name = name;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static String getTypeByCode(String code) {
        if (null == code) {
            return null;
        }
        return Arrays.stream(ConfigInfoEnum.values()).filter(item -> item.getType().equals(code)).findFirst().get().getType();
    }
}
