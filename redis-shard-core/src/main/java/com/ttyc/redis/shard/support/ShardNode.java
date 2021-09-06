package com.ttyc.redis.shard.support;

import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ttyc.redis.shard.core.XRedisTemplate;
import com.ttyc.redis.shard.enums.NodeTypeEnum;

import java.io.Serializable;

/**
 * @author yuanzl
 * @date 2021/8/23 4:10 下午
 */
public class ShardNode extends XRedisTemplate implements Serializable {
    /**
     * 节点名称
     */
    private String name;
    /**
     * host地址
     */
    private String addresses;
    /**
     * 密码
     */
    private String password;
    /**
     * 节点类型，NodeTypeEnum
     */
    private Integer nodeType= NodeTypeEnum.SINGLE.getType();
    /**
     * 虚拟节点数权重，默认160，分片节点数=160*weight
     */
    private Integer weight=1;
    /**
     * 变更类型 新增、修改、删除
     */
    private PropertyChangeType changeType;
    /**
     * 灰度分片：扩容时需要配置该值，灰度时默认双写，灰度节点只冗余数据，不对外提供服务，不配置灰度，则默认正式分片对外提供读写
     */
    private Boolean gray=false;
    /**
     * 双写分片：多个用逗号分隔，不灰度时配置双写，双写节点对外提供服务
     */
    private Boolean doubleWriter=false;
    /**
     * 迁移
     */
    private Boolean transfer=false;
    private Integer index;
    /**
     * 下一个节点
     */
    private ShardNode next;

    public ShardNode getNext() {
        return next;
    }

    public void setNext(ShardNode next) {
        this.next = next;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNodeType() {
        return nodeType;
    }

    public void setNodeType(Integer nodeType) {
        this.nodeType = nodeType;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
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

    public Boolean isTransfer() {
        return transfer;
    }

    public void setTransfer(Boolean transfer) {
        this.transfer = transfer;
    }

    public PropertyChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(PropertyChangeType changeType) {
        this.changeType = changeType;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
