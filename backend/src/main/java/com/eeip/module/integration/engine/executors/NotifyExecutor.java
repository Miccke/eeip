package com.eeip.module.integration.engine.executors;

import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NOTIFY：v0 只记录日志并透传（真实通道在 S12 接入）。
 */
@Slf4j
@Component
public class NotifyExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "NOTIFY";
    }

    @Override
    public Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) {
        Map<String, Object> cfg = node.config() == null ? Map.of() : node.config();
        log.info("[NOTIFY] channel={} to={} template={}",
                cfg.get("channel"), cfg.get("to"), cfg.get("template"));
        return input;
    }
}
