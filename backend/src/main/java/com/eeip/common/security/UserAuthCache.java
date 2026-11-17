package com.eeip.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * 登录态缓存：避免每次请求查库；权限变更时调用 evict 立即生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthCache {

    private static final String PREFIX = "eeip:auth:user:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public LoginUser get(Long userId) {
        try {
            String json = redis.opsForValue().get(PREFIX + userId);
            return json == null ? null : objectMapper.readValue(json, LoginUser.class);
        } catch (Exception e) {
            log.warn("read auth cache failed userId={}", userId, e);
            return null;
        }
    }

    public void put(LoginUser user) {
        try {
            redis.opsForValue().set(PREFIX + user.id(),
                    Objects.requireNonNull(objectMapper.writeValueAsString(user)), TTL);
        } catch (Exception e) {
            log.warn("write auth cache failed userId={}", user.id(), e);
        }
    }

    public void evict(Long userId) {
        redis.delete(PREFIX + userId);
    }
}
