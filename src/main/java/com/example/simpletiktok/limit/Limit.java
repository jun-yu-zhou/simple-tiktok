package com.example.simpletiktok.limit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解。
 * 用于声明某个接口在指定时间窗口内的最大访问次数。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Limit {

    /**
     * 时间窗口内允许的最大次数。
     */
    int limit() default 0;

    /**
     * 时间窗口（秒）。
     */
    long time() default 0;

    /**
     * Redis key 前缀。
     */
    String key() default "";

    /**
     * 超限提示文案。
     */
    String msg() default "系统服务繁忙";
}
