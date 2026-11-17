package com.eeip.common.security;

import com.eeip.common.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserAuthCache cache;
    private final LoginUserLoader loader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            doFilter0(request, response, chain);
        } finally {
            // 线程池复用，必须清理，防止串租户
            TenantContext.clear();
        }
    }

    private void doFilter0(HttpServletRequest request, HttpServletResponse response,
                           FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }

        Claims claims = tokenProvider.parse(header.substring(BEARER.length()));
        if (claims == null) {
            chain.doFilter(request, response);
            return;
        }

        Object uid = claims.get("uid");
        Long userId = uid instanceof Number n ? n.longValue() : null;
        if (userId == null) {
            chain.doFilter(request, response);
            return;
        }

        LoginUser user = cache.get(userId);
        if (user == null) {
            user = loader.load(userId);
            if (user != null) {
                cache.put(user);
            }
        }
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        user.perms().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        user.roles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));

        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // 写入租户上下文：后续所有 SQL 自动限定在该租户内
        TenantContext.set(user.tenantId());

        chain.doFilter(request, response);
    }
}
