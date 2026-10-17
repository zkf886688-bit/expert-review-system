package com.review.system.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser user) {
            return user;
        }
        return null;
    }

    public static Long currentUserId() {
        LoginUser user = currentUser();
        return user != null ? user.getUserId() : null;
    }

    public static String currentUsername() {
        LoginUser user = currentUser();
        return user != null ? user.getUsername() : null;
    }
}
