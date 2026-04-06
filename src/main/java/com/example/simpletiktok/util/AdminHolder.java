package com.example.simpletiktok.util;

import com.example.simpletiktok.pojo.entity.Admin;

/**
 * 管理员线程上下文。
 */
public final class AdminHolder {

    private static final ThreadLocal<Admin> ADMIN_HOLDER = new ThreadLocal<>();

    private AdminHolder() {
    }

    public static void set(Admin admin) {
        ADMIN_HOLDER.set(admin);
    }

    public static Admin get() {
        return ADMIN_HOLDER.get();
    }

    public static void clear() {
        ADMIN_HOLDER.remove();
    }
}
