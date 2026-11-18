package com.eeip.module.integration.expose;

import com.eeip.common.api.ErrorCode;
import com.eeip.common.api.R;
import com.eeip.common.exception.BizException;
import com.eeip.common.security.AppTokenProvider;
import com.eeip.module.system.app.AppClientService;
import com.eeip.module.system.app.SignSupport;
import com.eeip.module.system.entity.AppClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放平台令牌接口：调用方用 appKey + appSecret 签名换取 JWT。
 *
 * <pre>
 * POST /api/open/auth/token
 * { "appKey": "ak_xxx", "timestamp": 1794928000000, "nonce": "a1b2c3", "sign": "MD5(appKey+timestamp+nonce+secret)" }
 * </pre>
 */
@Slf4j
@Tag(name = "开放接口 · 令牌", description = "签名换取 JWT")
@RestController
@RequestMapping("/api/open/auth")
@RequiredArgsConstructor
public class OpenAuthController {

    /** 令牌有效期（秒） */
    private static final long TTL = 7200;

    private final AppClientService appClientService;
    private final SignSupport sign;
    private final AppTokenProvider tokenProvider;

    @Operation(summary = "换取访问令牌",
            description = "sign = MD5(appKey + timestamp + nonce + appSecret)，大写十六进制；时间戳允许 ±5 分钟。")
    @PostMapping("/token")
    public R<TokenResponse> token(@RequestBody @Valid TokenRequest req) {
        if (!sign.timestampOk(req.timestamp())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "时间戳超出允许范围");
        }
        AppClient client = appClientService.byKey(req.appKey());

        String expected = sign.md5(req.appKey() + req.timestamp() + req.nonce() + client.getAppSecret());
        if (!sign.verify(expected, req.sign())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "签名校验失败");
        }
        if (!sign.useNonce(req.appKey(), req.nonce())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "nonce 已被使用（疑似重放）");
        }

        String token = tokenProvider.createToken(req.appKey(), TTL);
        log.info("应用换取令牌 appKey={}", req.appKey());
        return R.ok(new TokenResponse(token, TTL, req.appKey()));
    }

    /** 换令牌请求 */
    public record TokenRequest(
            @NotBlank String appKey,
            @NotBlank String timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) {
    }

    /** 换令牌响应 */
    public record TokenResponse(String token, long expiresIn, String appKey) {
    }
}
