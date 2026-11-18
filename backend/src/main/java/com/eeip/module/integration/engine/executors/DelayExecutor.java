package com.eeip.module.integration.engine.executors;

import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** DELAY：等待指定毫秒。 */
@Component
public class DelayExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "DELAY";
    }

    @Override
    public Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) throws NodeException {
        int ms = 0;
        Object v = node.config() == null ? null : node.config().get("ms");
        if (v instanceof Number n) {
            ms = n.intValue();
        }
        try {
            Thread.sleep(Math.min(Math.max(ms, 0), 60000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NodeException("DELAY_INTERRUPTED", "延时被中断");
        }
        return input;
    }
}
