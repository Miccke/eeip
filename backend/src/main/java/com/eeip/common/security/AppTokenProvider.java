package com.eeip.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 应用令牌（JWT）：给第三方系统换取访问令牌用。
 *
 * <p>与用户令牌共用同一密钥但 **issuer 不同**（`eeip-app`），
 * 这样即使令牌格式相同也不会被用户令牌校验放行。</p>
 */
@Component
@RequiredArgsConstructor
public class AppTokenProvider {

    private static final String ISSUER = "eeip-app";

    private final JwtProperties props;
    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发应用令牌。
     *
     * @param appKey  应用标识
     * @param ttlSecs 有效期（秒）
     */
    public String createToken(String appKey, long ttlSecs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(appKey)
                .issuer(ISSUER)
                .claim("typ", "app")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlSecs * 1000))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验应用令牌。
     *
     * @return appKey；无效/过期/类型不符返回 null
     */
    public String parseAppKey(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
                    .parseSignedClaims(token).getPayload();
            if (!"app".equals(c.get("typ"))) {
                return null;
            }
            return c.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
