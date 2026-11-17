package com.eeip.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser lu) {
            return lu;
        }
        return null;
    }

    public static Long currentUserId() {
        LoginUser lu = currentUser();
        return lu == null ? null : lu.id();
    }

    public static String currentUsername() {
        LoginUser lu = currentUser();
        return lu == null ? "anonymous" : lu.username();
    }
}
