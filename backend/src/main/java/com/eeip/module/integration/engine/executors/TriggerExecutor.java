package com.eeip.module.integration.engine.executors;

import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 触发器节点（定时 / 消息订阅 / 事件）：把触发上下文作为下游输入。
 *
 * <p>触发器本身不做业务处理，只负责"把触发的入参交给流程"。</p>
 */
@Component
public class TriggerExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "SCHEDULE_CRON";   // 注册代表，其余类型在 FlowEngine 里按 manifest 复用
    }

    @Override
    public Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) {
        return vars.getOrDefault("trigger", Map.of());
    }
}
