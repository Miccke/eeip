package com.eeip.module.system.controller;

import com.eeip.common.api.R;
import com.eeip.module.system.dto.LoginRequest;
import com.eeip.module.system.service.UserAuthService;
import com.eeip.module.system.vo.LoginResponse;
import com.eeip.module.system.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口：登录、当前用户、登出。
 *
 * <p>登录成功后返回 JWT，前端后续请求以 {@code Authorization: Bearer <token>} 携带。</p>
 */
@Tag(name = "认证", description = "登录、当前用户信息与登出")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserAuthService authService;

    @Operation(summary = "登录", description = "校验账号密码，返回 JWT 访问令牌；同时写入登录日志。")
    @PostMapping("/login")
    public R<LoginResponse> login(
            @Parameter(description = "登录凭证", required = true)
            @RequestBody @Valid LoginRequest req,
            HttpServletRequest request) {
        return R.ok(authService.login(req, clientIp(request), request.getHeader("User-Agent")));
    }

    @Operation(summary = "当前用户信息",
            description = "返回用户资料、角色、权限点与按权限过滤后的菜单树，前端据此渲染侧边栏。")
    @GetMapping("/me")
    public R<UserInfoVO> me() {
        return R.ok(authService.me());
    }

    @Operation(summary = "登出", description = "清除服务端登录态缓存（Redis）。")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /**
     * 取真实客户端 IP：优先代理头 X-Forwarded-For 的第一个地址。
     */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
