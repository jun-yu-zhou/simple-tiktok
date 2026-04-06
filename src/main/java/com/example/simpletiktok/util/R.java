package com.example.simpletiktok.util;

import lombok.Data;

import java.text.MessageFormat;

@Data
public class R<T> {

    private static final long serialVersionUID = 1L;

    private int code;
    private Boolean state;
    private String message;
    private Object data;
    private long count;

    public static R<?> ok() {
        R<?> r = new R<>();
        r.setCode(200);
        r.setState(true);
        r.setMessage("success");
        return r;
    }

    public static R<?> error() {
        R<?> r = new R<>();
        r.setCode(500);
        r.setState(false);
        r.setMessage("error");
        return r;
    }

    public R<T> count(long count) {
        this.setCount(count);
        return this;
    }

    public R<T> code(int code) {
        this.setCode(code);
        return this;
    }

    public R<T> state(Boolean state) {
        this.setState(state);
        return this;
    }

    public R<T> message(String message) {
        this.setMessage(message);
        return this;
    }

    public R<T> message(String message, Object... args) {
        this.setMessage(MessageFormat.format(message, args));
        return this;
    }

    public R<T> data(Object result) {
        this.setData(result);
        return this;
    }
}
