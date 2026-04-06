package com.example.simpletiktok.util;

import com.example.simpletiktok.pojo.entity.User;

public final class UserHolder {

    private static final ThreadLocal<User> USER_HOLDER = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void set(User user) {
        USER_HOLDER.set(user);
    }

    public static User get() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
