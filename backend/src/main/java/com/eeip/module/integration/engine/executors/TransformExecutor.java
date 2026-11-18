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

        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            Object src = e.getValue();
            if (src instanceof String s && s.startsWith("$")) {
                out.put(String.valueOf(e.getKey()), Expr.resolve(scope, s));
            } else {
                out.put(String.valueOf(e.getKey()), src);
            }
        }
        if (input instanceof Map<?, ?> in) {
            for (Map.Entry<?, ?> e : in.entrySet()) {
                out.putIfAbsent(String.valueOf(e.getKey()), e.getValue());
            }
        }
        return out;
    }
}
