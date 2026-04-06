package com.example.simpletiktok.exception;

import lombok.Getter;

/**
 * 统一业务异常基类。
 * 用于在业务层主动抛出可预期异常，并由全局异常处理器统一转为 R 响应。
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 业务错误码，默认使用 500。
     */
    private final int code;

    /**
     * 面向前端的错误信息。
     */
    private final String msg;

    public BizException(String msg) {
        this(500, msg);
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
