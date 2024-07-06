package com.cat.common.entity;

import org.springframework.http.HttpStatus;

/***
 * <TODO description class purpose>
 * @title HttpResultStatus
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 0:28
 **/
public enum HttpResultStatus {
    SUCCESS(HttpStatus.OK.value(), "请求成功"),
    ERROR(-1,"请求失败"),
    NOT_FOUND(HttpStatus.NOT_FOUND.value(), "请求不存在"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), "鉴权失败"),
    FORBIDDEN(HttpStatus.FORBIDDEN.value(), "权限不足")
    ;

    HttpResultStatus(long code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private final long code;
    private String msg;

    public HttpResultStatus setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public long code() {
        return this.code;
    }

    public String msg() {
        return this.msg;
    }

}
