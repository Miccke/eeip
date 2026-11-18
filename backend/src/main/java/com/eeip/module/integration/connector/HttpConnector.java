package com.eeip.module.integration.connector;

import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 连接器：负责发起出站调用。
 *
 * <p>内置 SSRF 防护：默认禁止访问内网/回环/元数据地址，可通过
 * {@code eeip.connector.allow-private-network=true} 在受控环境放开。</p>
 */
@Slf4j
@Component
public class HttpConnector {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${eeip.connector.allow-private-network:false}")
    private boolean allowPrivateNetwork;

    /**
     * 发起一次 HTTP 调用。
     *
     * @param spec 调用描述（地址、方法、头、体、超时、认证）
     * @return 响应结果
     * @throws BizException 地址不合法、被 SSRF 拦截、或调用失败
     */
    public HttpResult invoke(HttpSpec spec) {
        URI uri = validateUri(spec.url(), spec.trusted());
        int timeout = spec.timeoutMs() == null ? 5000 : Math.min(Math.max(spec.timeoutMs(), 500), 60000);

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(timeout));

        Map<String, String> headers = new HashMap<>(spec.headers() == null ? Map.of() : spec.headers());
        applyAuth(headers, spec);

        headers.forEach(builder::header);
        if (spec.body() == null || spec.body().isBlank()) {
            builder.method(spec.method(), HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(spec.method(),
                    HttpRequest.BodyPublishers.ofString(spec.body(), StandardCharsets.UTF_8));
        }

        long start = System.currentTimeMillis();
        try {
            HttpResponse<String> resp = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long cost = System.currentTimeMillis() - start;
            Map<String, String> respHeaders = new HashMap<>();
            resp.headers().map().forEach((k, v) -> respHeaders.put(k, v.isEmpty() ? "" : v.get(0)));
            return new HttpResult(resp.statusCode(), resp.body(), cost, respHeaders, null);
        } catch (java.net.http.HttpTimeoutException e) {
            return new HttpResult(null, null, System.currentTimeMillis() - start, Map.of(), "请求超时");
        } catch (Exception e) {
            log.info("HTTP 调用失败 url={} err={}", spec.url(), e.toString());
            return new HttpResult(null, null, System.currentTimeMillis() - start, Map.of(),
                    "调用失败：" + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    /** 按认证方式补充请求头 */
    private void applyAuth(Map<String, String> headers, HttpSpec spec) {
        String type = spec.authType() == null ? "NONE" : spec.authType().toUpperCase();
        Map<String, String> cfg = spec.authConfig() == null ? Map.of() : spec.authConfig();
        switch (type) {
            case "BEARER" -> {
                String token = cfg.get("token");
                if (token != null && !token.isBlank()) {
                    headers.put("Authorization", "Bearer " + token);
                }
            }
            case "BASIC" -> {
                String user = cfg.getOrDefault("username", "");
                String pwd = cfg.getOrDefault("password", "");
                String raw = Base64.getEncoder()
                        .encodeToString((user + ":" + pwd).getBytes(StandardCharsets.UTF_8));
                headers.put("Authorization", "Basic " + raw);
            }
            case "API_KEY" -> {
                String header = cfg.getOrDefault("header", "X-Api-Key");
                String value = cfg.getOrDefault("value", "");
                if (!value.isBlank()) {
                    headers.put(header, value);
                }
            }
            default -> {
                // NONE：不加认证头
            }
        }
    }

    /**
     * 地址校验：只允许 http/https，并做内网地址拦截。
     */
    private URI validateUri(String url, boolean trusted) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "地址格式不合法");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅支持 http/https 协议");
        }
        // 受信目标（管理员登记的连接）或显式放开时，跳过内网拦截
        if (allowPrivateNetwork || trusted) {
            return uri;
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            for (InetAddress addr : addresses) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                    throw new BizException(ErrorCode.FORBIDDEN, "禁止访问内网地址：" + uri.getHost());
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "域名解析失败：" + uri.getHost());
        }
        return uri;
    }

    /**
     * 调用描述。
     */
    public record HttpSpec(String url, String method, Map<String, String> headers,
                           String body, Integer timeoutMs, String authType,
                           Map<String, String> authConfig,
                           /** 受信调用（目标来自管理员登记的连接），允许访问内网 */ boolean trusted) {

        /** 非受信调用（用户现填地址）：走 SSRF 校验 */
        public HttpSpec(String url, String method, Map<String, String> headers, String body,
                        Integer timeoutMs, String authType, Map<String, String> authConfig) {
            this(url, method, headers, body, timeoutMs, authType, authConfig, false);
        }
    }

    /**
     * 调用结果（不抛异常，由调用方判断 success）。
     */
    public record HttpResult(Integer status, String body, long costMs,
                             Map<String, String> headers, String error) {

        public boolean success() {
            return error == null && status != null && status >= 200 && status < 300;
        }
    }
}
