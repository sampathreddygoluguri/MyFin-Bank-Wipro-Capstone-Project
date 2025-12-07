package com.myfinbank.admin.config;

public class TokenContextHolder {
    private static final ThreadLocal<String> holder = new ThreadLocal<>();

    public static void setToken(String token) {
        holder.set(token);
    }

    public static String getToken() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }
}
