package com.eeip.module.integration.expose;

import com.eeip.common.api.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 对外开放网关（数据面）：/api/open/**。
 *
 * <p>三种鉴权：
 * <ul>
 *   <li>NONE：公开</li>
 *   <li>API_KEY：请求头 <code>X-Api-Key</code></li>
 *   <li>SIGNATURE：<code>Authorization: Bearer &lt;jwt&gt;</code> +
 *       <code>X-Timestamp</code> / <code>X-Nonce</code> / <code>X-Sign</code>，
 *       其中 sign = MD5(appKey + timestamp + nonce + METHOD + path + appSecret)</li>
 * </ul></p>
 */
@Tag(name = "开放接口 · 数据面", description = "对外暴露的接口入口")
@RestController
@RequiredArgsConstructor
public class OpenApiGatewayController {

    private final ApiExposeService service;

    @Operation(summary = "对外接口转发")
    @RequestMapping(value = "/api/open/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> forward(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        String path = uri.substring(uri.indexOf("/api/open") + "/api/open".length());
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        ApiExposeService.ForwardResult r = service.forward(new ApiExposeService.OpenCall(
                request.getMethod(), path, request.getQueryString(), body,
                request.getHeader("X-Api-Key"),
                request.getHeader("Authorization"),
                request.getHeader("X-Timestamp"),
                request.getHeader("X-Nonce"),
                request.getHeader("X-Sign")));

        if (r.status() == null) {
            return ResponseEntity.status(502)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"code\":\"OPEN_502\",\"message\":\""
                            + (r.error() == null ? "上游调用失败" : r.error()) + "\"}");
        }
        return ResponseEntity.status(r.status())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-EEIP-API", r.code())
                .body(r.body());
    }

    @Operation(summary = "开放网关探活")
    @RequestMapping(value = "/api/open/_ping", method = RequestMethod.GET)
    public R<String> ping() {
        return R.ok("open-gateway");
    }
}
