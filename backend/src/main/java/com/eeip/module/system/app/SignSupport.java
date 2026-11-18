package com.eeip.module.system.app;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;

/**
 * MD5 签名与防重放。
 *
 * <p>签名规则（大写十六进制）：</p>
 * <pre>
 * 换令牌：MD5(appKey + timestamp + nonce + appSecret)
 * 调接口：MD5(appKey + timestamp + nonce + METHOD + path + appSecret)
 * </pre>
 * <p>时间戳允许 ±5 分钟偏差；nonce 在窗口内只能使用一次（Redis 占位）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignSupport {

    /** 允许的时间偏差（毫秒） */
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    private final StringRedisTemplate redis;

    /** MD5 大写十六进制 */
    public String md5(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }

    /** 时间戳是否在允许窗口内 */
    public boolean timestampOk(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp);
            return Math.abs(System.currentTimeMillis() - ts) <= WINDOW_MS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 占位 nonce 防重放。
     *
     * @return true 首次使用；false 重放
     */
    public boolean useNonce(String appKey, String nonce) {
        Boolean ok = redis.opsForValue().setIfAbsent(
                "eeip:nonce:" + appKey + ":" + nonce, "1", Duration.ofMinutes(6));
        return Boolean.TRUE.equals(ok);
    }

    /** 校验签名（常量时间比较，避免计时攻击） */
    public boolean verify(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] a = expected.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
