package com.eeip.module.integration.engine.executors;

import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.Expr;
import com.eeip.module.integration.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TRANSFORM：按 mapping 做字段映射。
 *
 * <p>值以 {@code $} 开头按路径取值（可用 {@code $input.xxx} 引用上游输出），否则视为常量。
 * 上游是 Map 时未映射字段默认透传。</p>
 */
@Component
public class TransformExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "TRANSFORM";
    }

    @Override
    public Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) throws NodeException {
        Map<String, Object> cfg = node.config() == null ? Map.of() : node.config();
        Object mapping = cfg.get("mapping");
        if (!(mapping instanceof Map<?, ?> m) || m.isEmpty()) {
            throw new NodeException("TRANSFORM_NO_MAPPING", "未配置字段映射");
        }
        Map<String, Object> scope = new java.util.HashMap<>(vars);
        scope.put("input", input);
        return applyMapping(m, scope, input);
    }

    /**
     * 映射求值（执行与"映射预览"共用同一段逻辑，避免两边语义漂移）。
     *
     * <p>值以 {@code $} 开头按路径取值，否则视为常量；上游 Map 的字段默认透传（不覆盖已映射的键）。</p>
     */
    public static Map<String, Object> applyMapping(Map<?, ?> mapping, Map<String, Object> scope,
                                                   Object input) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : mapping.entrySet()) {
            out.put(String.valueOf(e.getKey()), resolveValue(scope, e.getValue()));
        }
        if (input instanceof Map<?, ?> in) {
            for (Map.Entry<?, ?> e : in.entrySet()) {
                out.putIfAbsent(String.valueOf(e.getKey()), e.getValue());
            }
        }
        return out;
    }

    /**
     * 求值单个映射项。
     *
     * <p>支持两种写法：{@code $input.orderNo}（显式）与 {@code $.orderNo}（简写，等价于上游字段）。
     * 简写只在第一个段不是已有变量时才生效，避免误伤 {@code $.nodes.n2}。</p>
     */
    public static Object resolveValue(Map<String, Object> scope, Object src) {
        if (!(src instanceof String expr) || !expr.startsWith("$")) {
            return src;
        }
        String path = expr;
        if (expr.startsWith("$.") && scope.containsKey("input")) {
            String rest = expr.substring(2);
            String first = rest.split("\\.")[0];
            if (!scope.containsKey(first)) {
                path = "$input." + rest;
            }
        }
        return Expr.resolve(scope, path);
    }
}
