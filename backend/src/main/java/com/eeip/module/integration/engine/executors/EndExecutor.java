package com.eeip.module.integration.engine.executors;

import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** END：把上游结果作为流程输出。 */
@Component
public class EndExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "END";
    }

    @Override
    public Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) {
        return input;
    }
}
