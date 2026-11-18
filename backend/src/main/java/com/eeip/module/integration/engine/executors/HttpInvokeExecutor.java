package com.eeip.module.integration.engine.executors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.module.integration.connector.HttpConnector;
import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.Expr;
import com.eeip.module.integration.engine.NodeExecutor;
import com.eeip.module.integration.domain.ApiConnector;
import com.eeip.module.integration.domain.ApiDefinition;
import com.eeip.module.integration.domain.ApiVersion;
import com.eeip.module.integration.entity.IntConnection;
import com.eeip.module.integration.mapper.ApiConnectorMapper;
import com.eeip.module.integration.mapper.ApiDefinitionMapper;
import com.eeip.module.integration.mapper.ApiVersionMapper;
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
    private final ApiDefinitionMapper apiMapper;
    private final ApiVersionMapper apiVersionMapper;
    private final ApiConnectorMapper apiConnectorMapper;
    private final HttpConnector connector;
    private final ObjectMapper objectMapper;

    @Override
    public String type() {
        return "HTTP_INVOKE";
    }

    @Override
    public Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) throws NodeException {
        Map<String, Object> cfg = node.config() == null ? Map.of() : node.config();

        // 两种引用方式：
        //   ① apiCode：引用接口台账（推荐），方法/路径/连接器都来自接口契约
        //   ② connectionCode：直接引用连接器（兼容老流程）
        String connectionCode = null;
        String method = String.valueOf(cfg.getOrDefault("operation", "GET")).toUpperCase();
        String pathCfg = cfg.get("path") == null ? null : String.valueOf(cfg.get("path"));

        Object apiCodeObj = cfg.get("apiCode");
        if (apiCodeObj != null && StringUtils.hasText(String.valueOf(apiCodeObj))) {
            String[] ref = String.valueOf(apiCodeObj).split("/", 2);   // systemCode/apiCode
            ApiDefinition api = ref.length == 2
                    ? apiMapper.selectOne(Wrappers.<ApiDefinition>lambdaQuery()
                        .eq(ApiDefinition::getSystemCode, ref[0])
                        .eq(ApiDefinition::getCode, ref[1]).last("LIMIT 1"))
                    : apiMapper.selectOne(Wrappers.<ApiDefinition>lambdaQuery()
                        .eq(ApiDefinition::getCode, String.valueOf(apiCodeObj)).last("LIMIT 1"));
            if (api == null) {
                throw new NodeException("FLOW_REF_API_MISSING", "接口不存在：" + apiCodeObj);
            }
            ApiVersion ver = api.getCurrentVersionId() == null ? null
                    : apiVersionMapper.selectById(api.getCurrentVersionId());
            if (ver == null) {
                throw new NodeException("FLOW_REF_API_MISSING", "接口没有可用版本：" + apiCodeObj);
            }
            method = ver.getMethod();
            pathCfg = ver.getPath();
            ApiConnector mainConn = apiConnectorMapper.selectOne(Wrappers.<ApiConnector>lambdaQuery()
                    .eq(ApiConnector::getApiVersionId, ver.getId())
                    .eq(ApiConnector::getStatus, 1)
                    .orderByAsc(ApiConnector::getId).last("LIMIT 1"));
            if (mainConn == null) {
                throw new NodeException("FLOW_REF_CONNECTOR_MISSING", "接口未绑定连接器：" + apiCodeObj);
            }
            connectionCode = mainConn.getConnectorCode();
        } else {
            Object codeObj = cfg.get("connectionCode");
            if (codeObj == null || !StringUtils.hasText(String.valueOf(codeObj))) {
                throw new NodeException("HTTP_NO_CONNECTION", "未配置连接或接口");
            }
            connectionCode = String.valueOf(codeObj);
        }

        IntConnection conn = connectionMapper.selectOne(Wrappers.<IntConnection>lambdaQuery()
                .eq(IntConnection::getCode, connectionCode).last("LIMIT 1"));
        if (conn == null) {
            throw new NodeException("HTTP_CONNECTION_NOT_FOUND", "连接不存在：" + connectionCode);
        }

        String base = conn.getEndpoint().replaceAll("/+$", "");
        String url = base + (pathCfg != null && StringUtils.hasText(pathCfg)
                ? (pathCfg.startsWith("/") ? pathCfg : "/" + pathCfg)
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
