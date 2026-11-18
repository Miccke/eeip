package com.eeip.module.integration.engine;

import com.eeip.common.exception.BizException;
import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.engine.executors.ChoiceExecutor;
import com.eeip.module.integration.entity.IntFlow;
import com.eeip.module.integration.entity.IntFlowInstance;
import com.eeip.module.integration.entity.IntFlowInstanceNode;
import com.eeip.module.integration.entity.IntFlowVersion;
import com.eeip.module.integration.mapper.IntFlowInstanceMapper;
import com.eeip.module.integration.mapper.IntFlowInstanceNodeMapper;
import com.eeip.module.integration.mapper.IntFlowMapper;
import com.eeip.module.integration.mapper.IntFlowVersionMapper;
import com.eeip.module.integration.nodetype.NodeManifest;
import com.eeip.module.integration.nodetype.NodeTypeRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * 流程执行引擎 v0：串行执行。
 *
 * <p>能力边界（v0 刻意保守）：</p>
 * <ul>
 *   <li>串行：沿 next / CHOICE 分支一路走到底</li>
 *   <li>变量：{@code trigger}（入参）、各节点 {@code outputVar}、{@code nodes.<id>}、{@code sys.*}</li>
 *   <li>错误策略：FAIL（中断）/ SKIP（跳过继续）/ ERROR_PORT（未实现，退化为 FAIL）</li>
 *   <li>限步：{@code maxSteps = 节点数 × 3 + 10}，防死循环</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowEngine {

    private final IntFlowMapper flowMapper;
    private final IntFlowVersionMapper versionMapper;
    private final IntFlowInstanceMapper instanceMapper;
    private final IntFlowInstanceNodeMapper nodeMapper;
    private final NodeTypeRegistry registry;
    private final ObjectMapper json;
    private final List<NodeExecutor> executors;

    private Map<String, NodeExecutor> executorMap;

    private Map<String, NodeExecutor> executors() {
        if (executorMap == null) {
            Map<String, NodeExecutor> m = new HashMap<>();
            for (NodeExecutor e : executors) {
                m.put(e.type(), e);
            }
            executorMap = m;
        }
        return executorMap;
    }

    /**
     * 执行流程。
     *
     * @param flow  流程
     * @param input 触发入参
     * @param trigger 触发类型（MANUAL/HTTP/SCHEDULE…）
     */
    public ExecutionResult run(IntFlow flow, Map<String, Object> input, String trigger) {
        IntFlowVersion ver = currentVersion(flow);
        if (ver == null) {
            throw new BizException(com.eeip.common.api.ErrorCode.BAD_REQUEST, "流程没有可执行版本");
        }
        FlowDsl dsl = parse(ver.getDslJson());
        Map<String, FlowDsl.Node> nodes = new LinkedHashMap<>();
        for (FlowDsl.Node n : dsl.nodes()) {
            nodes.put(n.id(), n);
        }
        FlowDsl.Node start = dsl.nodes().stream()
                .filter(n -> "START".equals(n.type())).findFirst()
                .orElseThrow(() -> new BizException(com.eeip.common.api.ErrorCode.BAD_REQUEST, "缺少 START 节点"));

        // 实例
        IntFlowInstance inst = new IntFlowInstance();
        inst.setFlowId(flow.getId());
        inst.setVersionId(ver.getId());
        inst.setFlowCode(flow.getFlowCode());
        inst.setTriggerType(trigger == null ? "MANUAL" : trigger);
        inst.setStatus("RUNNING");
        inst.setInputJson(toJson(input));
        inst.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        inst.setStartedAt(OffsetDateTime.now());
        instanceMapper.insert(inst);

        Map<String, Object> vars = new HashMap<>();
        vars.put("trigger", input == null ? Map.of() : input);
        vars.put("sys", Map.of(
                "tenantId", flow.getTenantId() == null ? 1L : flow.getTenantId(),
                "flowCode", flow.getFlowCode(),
                "traceId", inst.getTraceId(),
                "now", OffsetDateTime.now().toString()));

        List<NodeTrace> trace = new ArrayList<>();
        long t0 = System.currentTimeMillis();
        String status = "SUCCESS";
        String error = null;
        Object output = null;

        FlowDsl.Node cur = start;
        Object carry = vars.get("trigger");
        int seq = 0;
        int maxSteps = nodes.size() * 3 + 10;

        while (cur != null) {
            if (++seq > maxSteps) {
                status = "FAILED";
                error = "执行步数超过上限（疑似死循环）";
                break;
            }
            NodeExecutor ex = executors().get(cur.type());
            long ns = System.currentTimeMillis();
            String nodeStatus = "SUCCESS";
            String nodeError = null;
            Object out = null;

            if (ex == null) {
                NodeManifest spec = registry.get(cur.type());
                boolean planned = spec != null && spec.runtime() != null
                        && "planned".equals(String.valueOf(spec.runtime().get("state")));
                nodeStatus = "FAILED";
                nodeError = planned ? "节点执行器尚未实现：" + cur.type() : "未知节点类型：" + cur.type();
                status = "FAILED";
                error = nodeError;
            } else {
                try {
                    out = ex.execute(cur, vars, carry);
                } catch (NodeExecutor.NodeException ne) {
                    nodeStatus = "FAILED";
                    nodeError = ne.code() + " " + ne.getMessage();
                } catch (Exception e) {
                    nodeStatus = "FAILED";
                    nodeError = e.getClass().getSimpleName() + " " + e.getMessage();
                }
            }

            long cost = System.currentTimeMillis() - ns;

            // 错误策略
            if ("FAILED".equals(nodeStatus)) {
                String policy = policyOf(cur);
                if ("SKIP".equals(policy)) {
                    nodeStatus = "SKIPPED";
                    out = null;
                } else {
                    status = "FAILED";
                    error = nodeError;
                }
            }

            // 写变量
            if (out != null) {
                vars.put("nodes." + cur.id(), out);
                Object ov = cur.config() == null ? null : cur.config().get("outputVar");
                if (ov != null && StringUtils.hasText(String.valueOf(ov))) {
                    vars.put(String.valueOf(ov), out);
                }
                carry = out;
            }

            trace.add(new NodeTrace(cur.id(), cur.type(), cur.name(), nodeStatus,
                    cost, truncate(carry), truncate(out), nodeError));
            saveNode(inst.getId(), seq, cur, nodeStatus, carry, out, nodeError, cost, ns);

            if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                if ("FAILED".equals(status)) {
                    break;
                }
            }

            // 下一节点
            if ("END".equals(cur.type())) {
                output = out;
                break;
            }
            String nextId = nextOf(cur, vars);
            cur = nextId == null ? null : nodes.get(nextId);
            if (cur == null && nextId != null) {
                status = "FAILED";
                error = "下一节点不存在：" + nextId;
                break;
            }
        }

        long total = System.currentTimeMillis() - t0;
        inst.setStatus(status);
        inst.setError(error == null ? null : error.substring(0, Math.min(error.length(), 1000)));
        inst.setOutputJson(toJson(output));
        inst.setCostMs((int) total);
        inst.setFinishedAt(OffsetDateTime.now());
        instanceMapper.updateById(inst);

        log.info("流程执行 flow={} status={} cost={}ms steps={}",
                flow.getFlowCode(), status, total, trace.size());
        return new ExecutionResult(String.valueOf(inst.getId()), inst.getTraceId(), status, total,
                output, trace);
    }

    // ── 内部 ───────────────────────────────────────────────

    private String nextOf(FlowDsl.Node node, Map<String, Object> vars) {
        if ("CHOICE".equals(node.type())) {
            NodeExecutor ex = executors().get("CHOICE");
            if (ex instanceof ChoiceExecutor ce) {
                FlowDsl.Branch b = ce.pick(node, vars);
                return b == null ? null : b.next();
            }
            return null;
        }
        return node.next();
    }

    private String policyOf(FlowDsl.Node node) {
        NodeManifest spec = registry.get(node.type());
        if (spec == null || spec.behavior() == null) {
            return "FAIL";
        }
        Object ep = spec.behavior().get("errorPolicy");
        if (ep instanceof Map<?, ?> m) {
            Object def = m.get("default");
            return def == null ? "FAIL" : String.valueOf(def);
        }
        return "FAIL";
    }

    private void saveNode(Long instanceId, int seq, FlowDsl.Node node, String status,
                          Object in, Object out, String error, long cost, long startedAt) {
        try {
            IntFlowInstanceNode r = new IntFlowInstanceNode();
            r.setInstanceId(instanceId);
            r.setSeq(seq);
            r.setNodeId(node.id());
            r.setNodeType(node.type());
            r.setNodeName(node.name());
            r.setStatus(status);
            r.setInputJson(toJson(in));
            r.setOutputJson(toJson(out));
            r.setError(error == null ? null : error.substring(0, Math.min(error.length(), 1000)));
            r.setCostMs((int) cost);
            r.setStartedAt(OffsetDateTime.now().minusNanos((System.currentTimeMillis() - startedAt) * 1_000_000));
            nodeMapper.insert(r);
        } catch (Exception e) {
            log.warn("节点执行记录写入失败 instance={} node={}", instanceId, node.id(), e);
        }
    }

    private IntFlowVersion currentVersion(IntFlow flow) {
        if (flow.getCurrentVersionId() != null) {
            IntFlowVersion v = versionMapper.selectById(flow.getCurrentVersionId());
            if (v != null) {
                return v;
            }
        }
        return versionMapper.selectPublished(flow.getId());
    }

    private FlowDsl parse(String dslJson) {
        try {
            return json.readValue(dslJson, FlowDsl.class);
        } catch (Exception e) {
            throw new BizException(com.eeip.common.api.ErrorCode.INTERNAL, "DSL 解析失败：" + e.getMessage());
        }
    }

    private String toJson(Object o) {
        if (o == null) {
            return null;
        }
        try {
            String s = json.writeValueAsString(o);
            return s.length() > 8000 ? s.substring(0, 8000) + "…" : s;
        } catch (Exception e) {
            return null;
        }
    }

    private Object truncate(Object o) {
        return o;
    }

    /** 单节点执行轨迹 */
    public record NodeTrace(String nodeId, String type, String name, String status,
                            long costMs, Object input, Object output, String error) {
    }

    /** 执行结果 */
    public record ExecutionResult(String instanceId, String traceId, String status, long costMs,
                                  Object output, List<NodeTrace> trace) {
    }
}
