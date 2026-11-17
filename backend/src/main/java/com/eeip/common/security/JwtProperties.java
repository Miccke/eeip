package com.eeip.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "eeip.jwt")
public record JwtProperties(String secret, long expireSeconds, String issuer) {
}
