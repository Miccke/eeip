package com.eeip.module.integration.expose;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.common.security.LoginUser;
import com.eeip.common.security.SecurityUtils;
import com.eeip.module.integration.connector.HttpConnector;
import com.eeip.module.integration.engine.FlowEngine;
import com.eeip.module.integration.entity.IntFlow;
import com.eeip.module.integration.mapper.IntFlowMapper;
import com.eeip.common.security.AppTokenProvider;
import com.eeip.module.integration.entity.IntConnection;
import com.eeip.module.system.app.AppClientService;
import com.eeip.module.system.app.SignSupport;
import com.eeip.module.system.entity.AppClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import com.eeip.module.integration.mapper.ApiExposeMapper;
import com.eeip.module.integration.mapper.IntConnectionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 接口开放服务：登记、发布、转发。
 *
 * <p>转发即"接口封装"：外部调用 `/api/open/**`，平台校验 API Key 后
 * 按登记的连接与路径转发到内部系统，返回原始响应。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiExposeService {

    private final ApiExposeMapper mapper;
    private final IntConnectionMapper connectionMapper;
    private final HttpConnector httpConnector;
    private final ObjectMapper objectMapper;
    private final AppClientService appClientService;
    private final AppTokenProvider appTokenProvider;
    private final SignSupport sign;
    private final StringRedisTemplate redis;
    private final FlowEngine flowEngine;
    private final IntFlowMapper flowMapper;

    /** 列表 */
    public List<ApiExposeVO> list(String keyword) {
        return mapper.selectList(Wrappers.<ApiExpose>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(ApiExpose::getCode, keyword)
                                .or().like(ApiExpose::getName, keyword)
                                .or().like(ApiExpose::getPath, keyword))
                        .orderByDesc(ApiExpose::getUpdatedAt))
                .stream().map(this::toVO).toList();
    }

    /** 登记 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApiExposeSaveRequest req) {
        checkDuplicate(req, null);
        ApiExpose e = new ApiExpose();
        fill(e, req, true);
        if (StringUtils.hasText(e.getAuthType()) && "API_KEY".equals(e.getAuthType())
                && !StringUtils.hasText(e.getApiKey())) {
            e.setApiKey("ak_" + UUID.randomUUID().toString().replace("-", ""));
        }
        mapper.insert(e);
        return e.getId();
    }

    /** 编辑 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ApiExposeSaveRequest req) {
        ApiExpose e = require(id);
        checkDuplicate(req, id);
        fill(e, req, false);
        mapper.updateById(e);
    }

    /** 删除 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        mapper.deleteById(id);
    }

    /** 重置 API Key，返回新值（仅本次可见） */
    @Transactional(rollbackFor = Exception.class)
    public String resetKey(Long id) {
        ApiExpose e = require(id);
        String key = "ak_" + UUID.randomUUID().toString().replace("-", "");
        e.setApiKey(key);
        e.setAuthType("API_KEY");
        mapper.updateById(e);
        return key;
    }

    /**
     * 对外开放调用：鉴权后转发。
     *
     * @param method      HTTP 方法
     * @param path        对外路径（/api/open 之后的部分）
     * @param queryString 原样透传的查询串
     * @param body        请求体
     * @param apiKey      调用方传入的 X-Api-Key
     */
    public ForwardResult forward(OpenCall call) {
        String method = call.method();
        String path = call.path();
        String queryString = call.query();
        String body = call.body();
        String apiKey = call.apiKey();
        String actual = "/" + path.replaceAll("^/+", "");
        // 路径支持 {id} 占位，按模板匹配（表很小，内存过滤即可）
        List<ApiExpose> candidates = mapper.selectList(Wrappers.<ApiExpose>lambdaQuery()
                .eq(ApiExpose::getMethod, method));
        ApiExpose e = null;
        Map<String, String> pathVars = Map.of();
        for (ApiExpose c : candidates) {
            Map<String, String> vars = matchPath(c.getPath(), actual);
            if (vars != null) {
                e = c;
                pathVars = vars;
                break;
            }
        }
        if (e == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "接口不存在：" + method + " /" + path);
        }
        if (e.getStatus() == null || e.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "接口已停用");
        }
        if ("API_KEY".equals(e.getAuthType())) {
            if (!StringUtils.hasText(apiKey) || !apiKey.equals(e.getApiKey())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "API Key 无效");
            }
        } else if ("SIGNATURE".equals(e.getAuthType())) {
            // A 系统：JWT + MD5 签名 + 授权校验
            String appKey = appTokenProvider.parseAppKey(stripBearer(call.authorization()));
            if (!StringUtils.hasText(appKey)) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "令牌无效或已过期");
            }
            if (!sign.timestampOk(call.timestamp())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "时间戳超出允许范围");
            }
            AppClient client = appClientService.byKey(appKey);
            String expected = sign.md5(appKey + call.timestamp() + call.nonce()
                    + method.toUpperCase() + "/" + path.replaceAll("^/+", "") + client.getAppSecret());
            if (!sign.verify(expected, call.sign())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "签名校验失败");
            }
            if (!sign.useNonce(appKey, call.nonce())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "nonce 已被使用（疑似重放）");
            }
            if (!appClientService.hasGrant(client.getId(), e.getId())) {
                throw new BizException(ErrorCode.FORBIDDEN, "该应用未被授权调用此接口");
            }
            if (!ipAllowed(client.getAllowedIps(), call.clientIp())) {
                throw new BizException(ErrorCode.FORBIDDEN, "调用方 IP 不在白名单：" + call.clientIp());
            }
        }
        // 限流：按接口维度每秒计数（Redis），窗口 2 秒兜底过期
        if (e.getQps() != null && e.getQps() > 0) {
            String bucket = "eeip:rl:" + e.getId() + ":" + (System.currentTimeMillis() / 1000);
            Long n = redis.opsForValue().increment(bucket);
            if (n != null && n == 1L) {
                redis.expire(bucket, Duration.ofSeconds(2));
            }
            if (n != null && n > e.getQps()) {
                throw new BizException(ErrorCode.CONFLICT, "超过接口 QPS 限制：" + e.getQps());
            }
        }

        if ("FLOW".equals(e.getTargetType())) {
            // 场景 A 核心：请求进入 → 执行流程（编排/转换）→ 返回流程输出
            if (!StringUtils.hasText(e.getFlowCode())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "该接口未绑定流程");
            }
            IntFlow flow = flowMapper.selectOne(Wrappers.<IntFlow>lambdaQuery()
                    .eq(IntFlow::getFlowCode, e.getFlowCode()).last("LIMIT 1"));
            if (flow == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "流程不存在：" + e.getFlowCode());
            }
            Map<String, Object> input = new java.util.LinkedHashMap<>();
            input.put("method", method);
            input.put("path", "/" + path.replaceAll("^/+", ""));
            input.put("query", queryString);
            input.put("body", StringUtils.hasText(body) ? parseJson(body) : Map.of());
            FlowEngine.ExecutionResult r = flowEngine.run(flow, input, "HTTP");
            String json = toJson(r.output());
            log.info("开放接口走流程 code={} flow={} status={} cost={}ms",
                    e.getCode(), e.getFlowCode(), r.status(), r.costMs());
            return new ForwardResult("SUCCESS".equals(r.status()) ? 200 : 500,
                    json == null ? "{}" : json, null, e.getCode());
        }

        if (!"PROXY".equals(e.getTargetType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不支持的目标类型：" + e.getTargetType());
        }

        IntConnection conn = connectionMapper.selectOne(Wrappers.<IntConnection>lambdaQuery()
                .eq(IntConnection::getCode, e.getConnectionCode())
                .last("LIMIT 1"));
        if (conn == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "上游连接不存在：" + e.getConnectionCode());
        }

        String upstream = StringUtils.hasText(e.getUpstreamPath()) ? e.getUpstreamPath() : actual;
        for (Map.Entry<String, String> v : pathVars.entrySet()) {
            upstream = upstream.replace("{" + v.getKey() + "}", v.getValue());
        }
        String target = conn.getEndpoint().replaceAll("/+$", "")
                + (upstream.startsWith("/") ? upstream : "/" + upstream)
                + (StringUtils.hasText(queryString) ? "?" + queryString : "");

        HttpConnector.HttpResult r = httpConnector.invoke(new HttpConnector.HttpSpec(
                target,
                StringUtils.hasText(e.getUpstreamMethod()) ? e.getUpstreamMethod() : method,
                parseHeaders(e.getHeadersJson()),
                body,
                conn.getTimeoutMs(),
                conn.getAuthType(),
                parseHeaders(conn.getAuthConfig()),
                // 上游是管理员登记的连接，属于受信目标，允许访问内网
                true));

        log.info("开放接口转发 code={} {} -> {} status={} cost={}ms",
                e.getCode(), method, target, r.status(), r.costMs());
        return new ForwardResult(r.status(), r.body(), r.error(), e.getCode());
    }

    /** 转发结果 */
    public record ForwardResult(Integer status, String body, String error, String code) {
    }

    /**
     * 一次开放调用携带的凭据。
     */
    public record OpenCall(String method, String path, String query, String body,
                           String apiKey, String authorization, String timestamp,
                           String nonce, String sign, String clientIp) {
    }

    /** IP 白名单：空表示不限；支持逗号分隔与 * 通配前缀 */
    private boolean ipAllowed(String allowed, String ip) {
        if (!StringUtils.hasText(allowed) || "*".equals(allowed.trim())) {
            return true;
        }
        if (ip == null) {
            return false;
        }
        for (String item : allowed.split(",")) {
            String rule = item.trim();
            if (rule.isEmpty()) {
                continue;
            }
            if (rule.endsWith("*") ? ip.startsWith(rule.substring(0, rule.length() - 1)) : rule.equals(ip)) {
                return true;
            }
        }
        return false;
    }

    private String stripBearer(String auth) {
        if (auth == null) {
            return null;
        }
        return auth.startsWith("Bearer ") ? auth.substring(7) : auth;
    }

    // ── 内部 ────────────────────────────────────────────

    /**
     * 路径模板匹配：/orders/{id} 命中 /orders/123，返回 {id: 123}；不命中返回 null。
     */
    private Map<String, String> matchPath(String template, String actual) {
        if (template == null || actual == null) {
            return null;
        }
        String[] t = template.split("/");
        String[] a = actual.split("/");
        if (t.length != a.length) {
            return null;
        }
        Map<String, String> vars = new java.util.LinkedHashMap<>();
        for (int i = 0; i < t.length; i++) {
            String seg = t[i];
            if (seg.startsWith("{") && seg.endsWith("}")) {
                vars.put(seg.substring(1, seg.length() - 1), a[i]);
            } else if (!seg.equals(a[i])) {
                return null;
            }
        }
        return vars;
    }

    private ApiExpose require(Long id) {
        ApiExpose e = mapper.selectById(id);
        if (e == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "接口不存在");
        }
        return e;
    }

    private void checkDuplicate(ApiExposeSaveRequest req, Long selfId) {
        long same = mapper.selectCount(Wrappers.<ApiExpose>lambdaQuery()
                .eq(ApiExpose::getCode, req.code())
                .ne(selfId != null, ApiExpose::getId, selfId));
        if (same > 0) {
            throw new BizException(ErrorCode.CONFLICT, "接口编码已存在");
        }
        long samePath = mapper.selectCount(Wrappers.<ApiExpose>lambdaQuery()
                .eq(ApiExpose::getMethod, req.method() == null ? "GET" : req.method())
                .eq(ApiExpose::getPath, req.path())
                .ne(selfId != null, ApiExpose::getId, selfId));
        if (samePath > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该路径与方法已被占用");
        }
    }

    private void fill(ApiExpose e, ApiExposeSaveRequest req, boolean isCreate) {
        e.setCode(req.code());
        e.setName(req.name());
        e.setMethod(req.method() == null ? "GET" : req.method().toUpperCase());
        e.setPath(req.path());
        e.setTargetType(req.targetType() == null ? "PROXY" : req.targetType());
        e.setConnectionCode(req.connectionCode());
        e.setUpstreamPath(req.upstreamPath());
        e.setFlowCode(req.flowCode());
        e.setUpstreamMethod(req.upstreamMethod());
        e.setHeadersJson(req.headersJson());
        e.setAuthType(req.authType() == null ? "NONE" : req.authType());
        if (StringUtils.hasText(req.apiKey())) {
            e.setApiKey(req.apiKey());
        } else if (isCreate) {
            e.setApiKey(null);
        }
        e.setQps(req.qps() == null ? 0 : req.qps());
        e.setStatus(req.status() == null ? 1 : req.status());
        e.setVersion(req.version() == null ? "v1" : req.version());
        e.setRemark(req.remark());
    }

    private Object parseJson(String body) {
        try {
            return objectMapper.readValue(body, Object.class);
        } catch (Exception e) {
            return body;
        }
    }

    private String toJson(Object o) {
        try {
            return o == null ? "{}" : objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, String> parseHeaders(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private ApiExposeVO toVO(ApiExpose e) {
        return new ApiExposeVO(String.valueOf(e.getId()), e.getCode(), e.getName(), e.getMethod(),
                e.getPath(), "/api/open" + e.getPath(), e.getTargetType(), e.getConnectionCode(),
                e.getUpstreamPath(), e.getAuthType(), StringUtils.hasText(e.getApiKey()),
                e.getQps(), e.getStatus(), e.getVersion(), e.getRemark(), e.getUpdatedAt());
    }
}
