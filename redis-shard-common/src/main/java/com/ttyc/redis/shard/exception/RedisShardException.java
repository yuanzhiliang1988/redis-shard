package com.ttyc.redis.shard.exception;


import com.ttyc.redis.shard.enums.ExceptionMsgEnum;

public class RedisShardException extends RuntimeException {

    private static final long serialVersionUID = -6003868869041167435L;

    private int errorCode;
    private String errorMsg;
    private Throwable t;

    public RedisShardException(int errorCode) {
        this.errorCode = errorCode;
    }

    public RedisShardException(int errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public RedisShardException(int errorCode, Throwable t) {
        this.errorCode = errorCode;
        this.t = t;
    }

    public RedisShardException(String errorMsg, Throwable t) {
        super(errorMsg, t);
        this.errorMsg = errorMsg;
        this.t = t;
    }

    public RedisShardException(int errorCode, String errorMsg, Throwable t) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.t = t;
    }

    public RedisShardException(ExceptionMsgEnum msgEnum) {
        super(msgEnum.getMessage());
        this.errorCode = msgEnum.getCode();
        this.errorMsg = msgEnum.getMessage();
    }

    public RedisShardException(ExceptionMsgEnum msgEnum, Throwable t) {
        super(msgEnum.getMessage(),t);
        this.errorCode = msgEnum.getCode();
        this.errorMsg = msgEnum.getMessage();
        this.t = t;
    }


    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Throwable getT() {
        return t;
    }

    public void setT(Throwable t) {
        this.t = t;
    }

}
