package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应。
 */
@Schema(description = "登录响应")
public record LoginResponse(

        @Schema(description = "JWT 访问令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "令牌类型，固定 Bearer", example = "Bearer")
        String tokenType,

        @Schema(description = "有效期（秒）", example = "7200")
        long expiresIn
) {
}
