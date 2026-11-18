package com.eeip.module.integration.engine.executors;

import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.Expr;
import com.eeip.module.integration.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * CHOICE：不做计算，只把上游透传；分支判定由引擎负责（需要访问 branches）。
 */
@Component
public class ChoiceExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "CHOICE";
    }

    @Override
    public Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) {
        return input;
    }

    /** 求值分支条件，返回命中的分支；未命中返回 null */
    public FlowDsl.Branch pick(FlowDsl.Node node, Map<String, Object> vars) {
        if (node.branches() == null) {
            return null;
        }
        FlowDsl.Branch otherwise = null;
        for (FlowDsl.Branch b : node.branches()) {
            if (Boolean.TRUE.equals(b.otherwise())) {
                otherwise = b;
                continue;
            }
            if (Expr.eval(vars, b.when())) {
                return b;
            }
        }
        return otherwise;
    }
}
