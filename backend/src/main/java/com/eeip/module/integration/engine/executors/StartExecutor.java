package com.eeip.module.integration.engine.executors;

import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** START：把触发入参原样输出。 */
@Component
public class StartExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "START";
    }

    @Override
    public Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) {
        return vars.getOrDefault("trigger", Map.of());
    }
}
