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
 * <p>外部系统按登记的方法与路径调用，平台校验 API Key 后转发到上游，
 * 并把上游响应原样返回。免登录，但不免鉴权（由接口自身的 authType 决定）。</p>
 */
@Tag(name = "开放接口 · 数据面", description = "对外暴露的接口入口")
@RestController
@RequiredArgsConstructor
public class OpenApiGatewayController {

    private final ApiExposeService service;

    @Operation(summary = "对外接口转发",
            description = "按登记信息转发到上游系统；API_KEY 类型需在请求头带 X-Api-Key。")
    @RequestMapping(value = "/api/open/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> forward(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        String path = uri.substring(uri.indexOf("/api/open") + "/api/open".length());
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String apiKey = request.getHeader("X-Api-Key");

        ApiExposeService.ForwardResult r = service.forward(
                request.getMethod(), path, request.getQueryString(), body, apiKey);

        if (r.status() == null) {
            return ResponseEntity.status(502)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"code\":\"OPEN_502\"," +
                            "\"message\":\"" + (r.error() == null ? "上游调用失败" : r.error()) + "\"}");
        }
        return ResponseEntity.status(r.status())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-EEIP-API", r.code())
                .body(r.body());
    }

    /** 便于探活 */
    @Operation(summary = "开放网关探活")
    @RequestMapping(value = "/api/open/_ping", method = RequestMethod.GET)
    public R<String> ping() {
        return R.ok("open-gateway");
    }
}
