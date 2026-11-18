package com.eeip.module.integration.engine.executors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.module.integration.connector.HttpConnector;
import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.Expr;
import com.eeip.module.integration.engine.NodeExecutor;
import com.eeip.module.integration.entity.IntConnection;
import com.eeip.module.integration.mapper.IntConnectionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP_INVOKE：调用连接配置的上游接口。
 *
 * <p>URL = 连接 endpoint；方法取 config.operation（GET/POST/…）；
 * 请求体优先 config.body 表达式，否则把上游输出序列化为 JSON。</p>
 */
@Component
@RequiredArgsConstructor
public class HttpInvokeExecutor implements NodeExecutor {

    private final IntConnectionMapper connectionMapper;
    private final HttpConnector connector;
    private final ObjectMapper objectMapper;

    @Override
    public String type() {
        return "HTTP_INVOKE";
    }

    @Override
    public Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) throws NodeException {
        Map<String, Object> cfg = node.config() == null ? Map.of() : node.config();
        Object codeObj = cfg.get("connectionCode");
        if (codeObj == null || !StringUtils.hasText(String.valueOf(codeObj))) {
            throw new NodeException("HTTP_NO_CONNECTION", "未配置连接");
        }
        IntConnection conn = connectionMapper.selectOne(Wrappers.<IntConnection>lambdaQuery()
                .eq(IntConnection::getCode, String.valueOf(codeObj)).last("LIMIT 1"));
        if (conn == null) {
            throw new NodeException("HTTP_CONNECTION_NOT_FOUND", "连接不存在：" + codeObj);
        }

        String method = String.valueOf(cfg.getOrDefault("operation", "GET")).toUpperCase();
        // 连接是"基地址"，节点可再用 config.path 指定具体路径
        String base = conn.getEndpoint().replaceAll("/+$", "");
        Object pathCfg = cfg.get("path");
        String url = base + (pathCfg != null && StringUtils.hasText(String.valueOf(pathCfg))
                ? (String.valueOf(pathCfg).startsWith("/") ? pathCfg : "/" + pathCfg)
                : "");
        String body = null;
        if (cfg.get("body") instanceof String b && b.startsWith("$")) {
            Object v = Expr.resolve(vars, b);
            body = v == null ? null : String.valueOf(v);
        } else if (input != null && !"GET".equals(method) && !"DELETE".equals(method)) {
            try {
                body = objectMapper.writeValueAsString(input);
            } catch (Exception ignored) {
                body = null;
            }
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        HttpConnector.HttpResult r = connector.invoke(new HttpConnector.HttpSpec(
                url, method, headers, body,
                conn.getTimeoutMs(), conn.getAuthType(),
                stringMap(Expr.parseObject(conn.getAuthConfig())), true));

        if (r.error() != null) {
            throw new NodeException("HTTP_CALL_FAILED", r.error());
        }
        if (r.status() == null || r.status() >= 400) {
            throw new NodeException("HTTP_STATUS_" + r.status(), "上游返回 " + r.status());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", r.status());
        out.put("costMs", r.costMs());
        out.put("body", parseBody(r.body()));
        return out;
    }

    private Map<String, String> stringMap(Map<String, Object> src) {
        Map<String, String> m = new LinkedHashMap<>();
        src.forEach((k, v) -> m.put(k, v == null ? null : String.valueOf(v)));
        return m;
    }

    private Object parseBody(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            return objectMapper.readValue(body, new TypeReference<Object>() {
            });
        } catch (Exception e) {
            return body;
        }
    }
}
