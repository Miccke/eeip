package com.eeip.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        byte[] secret = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("eeip.jwt.secret 至少需要 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String createToken(LoginUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.username())
                .issuer(props.issuer())
                .claim("uid", user.id())
                .claim("tid", user.tenantId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(props.expireSeconds())))
                .signWith(key)
                .compact();
    }

    /**
     * @return 解析后的 claims；令牌无效或过期时返回 null
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long expireSeconds() {
        return props.expireSeconds();
    }
}
