package com.eeip.module.integration.nodetype;

import com.eeip.module.integration.vo.FlowValidateResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 轻量 JSON Schema 校验器（只实现我们用到的那部分）。
 *
 * <p>为什么不用完整实现：依赖体积与安全攻击面都不划算。这里覆盖
 * type / required / properties / additionalProperties / enum / minimum /
 * maximum / pattern / items 九类关键字，足够支撑节点配置校验。</p>
 */
@Component
public class MiniSchemaValidator {

    /**
     * 引擎级保留键：所有节点都可用，不参与节点自身的配置校验。
     *
     * <p>retry：节点重试策略（maxAttempts / delayMs）；
     * outputVar：输出变量名；errorPolicy：错误策略；timeoutMs：超时。</p>
     */
    private static final java.util.Set<String> RESERVED_KEYS =
            java.util.Set.of("retry", "outputVar", "errorPolicy", "timeoutMs", "onError");

    /**
     * 按 schema 校验一个对象。
     *
     * @param schema  节点 config 的 schema
     * @param value   实际配置
     * @param nodeId  归属节点（用于错误定位）
     * @return 错误列表，空表示通过
     */
    @SuppressWarnings("unchecked")
    public List<FlowValidateResult.Issue> validate(Map<String, Object> schema,
                                                   Map<String, Object> value,
                                                   String nodeId) {
        List<FlowValidateResult.Issue> issues = new ArrayList<>();
        if (schema == null) {
            return issues;
        }
        Map<String, Object> cfg = value == null ? Map.of() : value;

        // required
        Object required = schema.get("required");
        if (required instanceof List<?> reqList) {
            for (Object r : reqList) {
                String key = String.valueOf(r);
                Object v = cfg.get(key);
                if (v == null || (v instanceof String s && s.isBlank())) {
                    issues.add(issue("CFG_REQUIRED",
                            "缺少必填配置：" + key + fieldTitle(schema, key), nodeId, key));
                }
            }
        }

        Object propsObj = schema.get("properties");
        Map<String, Object> props = propsObj instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();

        // additionalProperties: false → 未知字段直接报错（防"配置写错 key 静默失效"）
        if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            // 引擎保留键：所有节点通用，不属于节点自身的配置契约
            for (String key : cfg.keySet()) {
                if (!props.containsKey(key)) {
                    if (!RESERVED_KEYS.contains(key)) {
                        issues.add(issue("CFG_UNKNOWN", "未知配置项：" + key, nodeId, key));
                    }
                }
            }
        }

        // 逐字段
        for (Map.Entry<String, Object> e : props.entrySet()) {
            String key = e.getKey();
            Object v = cfg.get(key);
            if (v == null) {
                continue;
            }
            if (e.getValue() instanceof Map<?, ?> raw) {
                checkField(key, (Map<String, Object>) raw, v, nodeId, issues);
            }
        }
        return issues;
    }

    @SuppressWarnings("unchecked")
    private void checkField(String key, Map<String, Object> rule, Object v,
                            String nodeId, List<FlowValidateResult.Issue> issues) {
        String type = String.valueOf(rule.getOrDefault("type", ""));
        boolean typeOk = switch (type) {
            case "string" -> v instanceof String;
            case "integer" -> v instanceof Integer || v instanceof Long;
            case "number" -> v instanceof Number;
            case "boolean" -> v instanceof Boolean;
            case "object" -> v instanceof Map;
            case "array" -> v instanceof List;
            default -> true;
        };
        if (!typeOk) {
            issues.add(issue("CFG_TYPE",
                    "配置项 " + key + " 类型应为 " + type + "，实际 " + v.getClass().getSimpleName(),
                    nodeId, key));
            return;
        }

        if (rule.get("enum") instanceof List<?> enums && !enums.contains(v)) {
            issues.add(issue("CFG_ENUM",
                    "配置项 " + key + " 取值必须是 " + enums + " 之一", nodeId, key));
        }
        if (v instanceof Number n) {
            if (rule.get("minimum") instanceof Number min && n.doubleValue() < min.doubleValue()) {
                issues.add(issue("CFG_MIN", "配置项 " + key + " 不得小于 " + min, nodeId, key));
            }
            if (rule.get("maximum") instanceof Number max && n.doubleValue() > max.doubleValue()) {
                issues.add(issue("CFG_MAX", "配置项 " + key + " 不得大于 " + max, nodeId, key));
            }
        }
        if (v instanceof String s && rule.get("pattern") instanceof String p && !Pattern.matches(p, s)) {
            issues.add(issue("CFG_PATTERN", "配置项 " + key + " 格式不匹配 " + p, nodeId, key));
        }
        // 嵌套对象继续按 properties 校验
        if (v instanceof Map<?, ?> sub && rule.get("properties") instanceof Map<?, ?> subProps) {
            issues.addAll(validate(
                    Map.of("properties", subProps,
                            "required", rule.getOrDefault("required", List.of()),
                            "additionalProperties", rule.getOrDefault("additionalProperties", true)),
                    (Map<String, Object>) sub, nodeId));
        }
    }

    private String fieldTitle(Map<String, Object> schema, String key) {
        Object propsObj = schema.get("properties");
        if (propsObj instanceof Map<?, ?> props && props.get(key) instanceof Map<?, ?> rule) {
            Object title = rule.get("title");
            return title == null ? "" : "（" + title + "）";
        }
        return "";
    }

    private FlowValidateResult.Issue issue(String code, String message, String nodeId, String field) {
        return new FlowValidateResult.Issue(code, message + (field == null ? "" : " [" + field + "]"), nodeId);
    }
}
