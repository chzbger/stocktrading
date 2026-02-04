package com.example.stocktrading.common.security;

public class AuthContext {

    private static final ThreadLocal<AuthInfo> currentAuth = new ThreadLocal<>();

    public static void set(AuthInfo authInfo) {
        currentAuth.set(authInfo);
    }

    public static AuthInfo get() {
        return currentAuth.get();
    }

    public static Long getUserId() {
        AuthInfo auth = currentAuth.get();
        return auth != null ? auth.userId() : null;
    }

    public static void clear() {
        currentAuth.remove();
    }

    public record AuthInfo(Long userId, String role) {}
}
