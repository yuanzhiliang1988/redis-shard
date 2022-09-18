package com.ttyc.redis.shard.support;

import com.ttyc.redis.shard.constant.ShardConstants;
import com.ttyc.redis.shard.enums.NodeTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 分片节点信息
 * @author yuanzl
 * @date 2021/8/23 5:08 下午
 */
public class Node extends ShardConfig implements Serializable{
    /**
     * redis分片类型：1-单例（默认），2-哨兵，3-集群
     */
    private int type = NodeTypeEnum.SINGLE.getType();

    /**
     * Database index used by the connection factory.
     */
    private int database = 0;

    /**
     * Redis server host and port.
     */
    private String addresses = "127.0.0.1:6379";

    /**
     * Login password of the redis server.
     */
    private String password;

    /**
     * Whether to enable SSL support.
     */
    private boolean ssl;

    /**
     * 灰度分片：扩容时需要配置该值，灰度时默认双写，灰度节点只冗余数据，不对外提供服务，不配置灰度，则默认正式分片对外提供读写
     */
    private Boolean gray=false;
    /**
     * 双写分片：多个用逗号分隔，不灰度时配置双写，双写节点对外提供服务
     */
    private Boolean doubleWriter=false;

    private Sentinel sentinel;

    public int getDatabase() {
        return this.database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSsl() {
        return this.ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Sentinel getSentinel() {
        return sentinel;
    }

    public void setSentinel(Sentinel sentinel) {
        this.sentinel = sentinel;
    }

    public String getAddresses() {
        return addresses;
    }

    public void setAddresses(String addresses) {
        this.addresses = addresses;
    }

    public Boolean isGray() {
        return gray;
    }

    public void setGray(Boolean gray) {
        this.gray = gray;
    }

    public Boolean isDoubleWriter() {
        return doubleWriter;
    }

    public void setDoubleWriter(Boolean doubleWriter) {
        this.doubleWriter = doubleWriter;
    }

    public HashSet<String> getHostAndPorts() {
        return new HashSet<>(Arrays.stream(StringUtils.split(this.addresses, ShardConstants.HOST_SPLIT)).collect(Collectors.toSet()));
    }

    /**
     * Redis sentinel properties.
     */
    public static class Sentinel {

        /**
         * Name of the Redis server.
         */
        private String master;

        public String getMaster() {
            return this.master;
        }

        public void setMaster(String master) {
            this.master = master;
        }
    }
}
