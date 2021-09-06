package com.ttyc.redis.shard.enums;

/**
 * @author yuanzl
 * @date 2021/8/23 3:09 下午
 */
public enum ExceptionMsgEnum {

    PARAM_NULL(20001, "参数为空"),
    PARAM_INVALID(20002, "参数不合法"),
    RESULT_NULL(20003, "结果为空"),
    APP_NAME_NULL(20004, "spring.application.name不能为空"),
    SCAN_EXCEPTION(20005, "scan操作异常"),
    GET_JEDIS_FACTORY_FAIL(20006, "get JedisConnectionFactory fail"),
    NODE_NOT_EXISTS(20007, "节点不存在");

    private int code;
    private String message;

    ExceptionMsgEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 支持同一种case多种未知msg
     *
     * @param message
     * @return
     */
    public ExceptionMsgEnum setMessage(String message) {
        this.message = message;
        return this;
    }
}
