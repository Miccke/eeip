package com.eeip.module.integration.service;

import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.nodetype.MiniSchemaValidator;
import com.eeip.module.integration.nodetype.NodeManifest;
import com.eeip.module.integration.nodetype.NodeTypeRegistry;
import com.eeip.module.integration.vo.FlowValidateResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 流程 DSL 校验器。
 *
 * <p>做什么：结构校验 → 引用完整性 → 可达性 → 环检测 → 节点类型专属约束。
 * 环检测是重点：DSL 没有 while 语义，出现环意味着运行时可能死循环，
 * 必须在发布前拦住。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowValidator {

    /** 支持的 DSL 版本 */
    private static final Set<String> SUPPORTED_SCHEMA = Set.of("1.0");

    /** 支持的节点类型 */
    private final NodeTypeRegistry registry;
    private final MiniSchemaValidator schemaValidator;

    private final ObjectMapper objectMapper;

    /**
     * 执行全量校验。
     *
     * @param dsl 流程 DSL
     * @return 校验结果（errors 非空即不通过）
     */
    public FlowValidateResult validate(FlowDsl dsl) {
        List<FlowValidateResult.Issue> errors = new ArrayList<>();
        List<FlowValidateResult.Issue> warnings = new ArrayList<>();

        if (dsl == null) {
            errors.add(issue("FLOW_EMPTY", "DSL 为空", null));
            return new FlowValidateResult(false, errors, warnings, 0, null);
        }
        if (dsl.schemaVersion() == null || !SUPPORTED_SCHEMA.contains(dsl.schemaVersion())) {
            errors.add(issue("FLOW_SCHEMA_VERSION",
                    "不支持的 DSL 版本：" + dsl.schemaVersion() + "（当前支持 1.0）", null));
        }

        List<FlowDsl.Node> nodes = dsl.nodes() == null ? List.of() : dsl.nodes();
        if (nodes.isEmpty()) {
            errors.add(issue("FLOW_EMPTY", "流程至少需要一个节点", null));
            return new FlowValidateResult(false, errors, warnings, 0, null);
        }

        // ① 节点基础：ID 唯一、类型合法
        Map<String, FlowDsl.Node> byId = new LinkedHashMap<>();
        for (FlowDsl.Node n : nodes) {
            String id = n.id();
            if (id == null || id.isBlank()) {
                errors.add(issue("FLOW_NODE_ID_EMPTY", "存在空节点 ID", null));
                continue;
            }
            if (byId.putIfAbsent(id, n) != null) {
                errors.add(issue("FLOW_NODE_DUP", "节点 ID 重复：" + id, id));
            }
            if (n.type() == null || !registry.contains(n.type())) {
                errors.add(issue("FLOW_NODE_TYPE", "未知节点类型：" + n.type(), id));
            }
        }

        // ② START / END 数量
        long startCount = nodes.stream().filter(n -> "START".equals(n.type())).count();
        long endCount = nodes.stream().filter(n -> "END".equals(n.type())).count();
        if (startCount != 1) {
            errors.add(issue("FLOW_START_COUNT", "必须有且仅有 1 个 START 节点，当前 " + startCount + " 个", null));
        }
        if (endCount < 1) {
            errors.add(issue("FLOW_END_COUNT", "至少需要 1 个 END 节点", null));
        }

        // ③ 引用完整性 + 出边收集
        Map<String, List<String>> edges = new LinkedHashMap<>();
        for (FlowDsl.Node n : nodes) {
            List<String> out = new ArrayList<>();
            switch (n.type() == null ? "" : n.type()) {
                case "END" -> {
                    if (n.next() != null && !n.next().isBlank()) {
                        errors.add(issue("FLOW_END_HAS_NEXT", "END 节点不应有 next", n.id()));
                    }
                }
                case "CHOICE" -> {
                    List<FlowDsl.Branch> branches = n.branches() == null ? List.of() : n.branches();
                    if (branches.isEmpty()) {
                        errors.add(issue("FLOW_CHOICE_EMPTY", "CHOICE 节点至少需要一个分支", n.id()));
                    }
                    long otherwiseCount = branches.stream()
                            .filter(b -> Boolean.TRUE.equals(b.otherwise())).count();
                    if (otherwiseCount > 1) {
                        errors.add(issue("FLOW_CHOICE_OTHERWISE",
                                "CHOICE 只允许一个兜底分支，当前 " + otherwiseCount + " 个", n.id()));
                    }
                    if (otherwiseCount == 0) {
                        warnings.add(issue("FLOW_CHOICE_NO_OTHERWISE",
                                "CHOICE 没有兜底分支，条件都不命中时流程会中断", n.id()));
                    }
                    for (FlowDsl.Branch b : branches) {
                        if (!Boolean.TRUE.equals(b.otherwise()) && (b.when() == null || b.when().isBlank())) {
                            errors.add(issue("FLOW_BRANCH_WHEN", "分支缺少条件表达式", n.id()));
                        }
                        checkRef(b.next(), byId, errors, n.id());
                        if (b.next() != null) {
                            out.add(b.next());
                        }
                    }
                }
                default -> {
                    if (n.next() == null || n.next().isBlank()) {
                        errors.add(issue("FLOW_NODE_NO_NEXT",
                                n.type() + " 节点必须有 next", n.id()));
                    } else {
                        checkRef(n.next(), byId, errors, n.id());
                        out.add(n.next());
                    }
                }
            }
            edges.put(n.id(), out);
            checkNodeConfig(n, errors, warnings);
        }

        // ④ 环检测
        List<String> cycle = findCycle(byId.keySet(), edges);
        if (cycle != null) {
            errors.add(issue("FLOW_CYCLE", "存在环：" + String.join(" → ", cycle), cycle.get(0)));
        }

        // ⑤ 可达性：从 START 出发能否到达所有节点
        String startId = nodes.stream().filter(n -> "START".equals(n.type()))
                .map(FlowDsl.Node::id).findFirst().orElse(null);
        if (startId != null && !errors.stream().anyMatch(e -> "FLOW_START_COUNT".equals(e.code()))) {
            Set<String> reachable = bfs(startId, edges);
            for (String id : byId.keySet()) {
                if (!reachable.contains(id)) {
                    warnings.add(issue("FLOW_UNREACHABLE", "节点从 START 不可达：" + id, id));
                }
            }
        }

        return new FlowValidateResult(errors.isEmpty(), errors, warnings, nodes.size(),
                sha256(toCanonicalJson(dsl)));
    }

    /**
     * 节点配置校验：由 manifest 的 config schema 驱动。
     *
     * <p>不再按类型写 switch —— 新增节点只要带 schema，校验自动生效。
     * （S10 会把"业务级"校验放到 NodeExecutor.validateConfig）</p>
     */
    private void checkNodeConfig(FlowDsl.Node n, List<FlowValidateResult.Issue> errors,
                                 List<FlowValidateResult.Issue> warnings) {
        NodeManifest spec = registry.get(n.type());
        if (spec == null) {
            return;   // 类型非法已在前面报过
        }
        errors.addAll(schemaValidator.validate(spec.config(), n.config(), n.id()));

        // 通用提示：START 建议写说明
        if ("START".equals(n.type())
                && (n.config() == null || n.config().get("description") == null)) {
            warnings.add(issue("FLOW_START_DESC", "建议为 START 填写说明", n.id()));
        }
    }

    private void checkRef(String next, Map<String, FlowDsl.Node> byId,
                          List<FlowValidateResult.Issue> errors, String from) {
        if (next != null && !next.isBlank() && !byId.containsKey(next)) {
            errors.add(issue("FLOW_REF_MISSING", "引用了不存在的节点：" + next, from));
        }
    }

    /** DFS 找环，返回环路径；无环返回 null */
    private List<String> findCycle(Collection<String> ids, Map<String, List<String>> edges) {
        Map<String, Integer> color = new HashMap<>();   // 0 未访问 1 在栈上 2 完成
        Deque<String> stack = new ArrayDeque<>();
        for (String id : ids) {
            List<String> path = dfs(id, edges, color, stack);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    private List<String> dfs(String id, Map<String, List<String>> edges,
                             Map<String, Integer> color, Deque<String> stack) {
        int c = color.getOrDefault(id, 0);
        if (c == 1) {
            // 命中栈上节点：栈自底向顶即 DFS 路径，从重复节点处截断并回到起点
            List<String> path = new ArrayList<>(stack);   // 迭代顺序是栈顶在前
            Collections.reverse(path);                    // 转为自底向顶
            int idx = path.indexOf(id);
            List<String> cycle = new ArrayList<>(path.subList(idx, path.size()));
            cycle.add(id);
            return cycle;
        }
        if (c == 2) {
            return null;
        }
        color.put(id, 1);
        stack.push(id);
        for (String next : edges.getOrDefault(id, List.of())) {
            List<String> res = dfs(next, edges, color, stack);
            if (res != null) {
                return res;
            }
        }
        stack.pop();
        color.put(id, 2);
        return null;
    }

    private Set<String> bfs(String start, Map<String, List<String>> edges) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (String next : edges.getOrDefault(cur, List.of())) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return seen;
    }

    /** 规范化 JSON：键排序，保证同一 DSL 的哈希稳定 */
    private String toCanonicalJson(Object dsl) {
        try {
            ObjectMapper m = objectMapper.copy()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return m.writeValueAsString(dsl);
        } catch (Exception e) {
            return String.valueOf(dsl.hashCode());
        }
    }

    private String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private FlowValidateResult.Issue issue(String code, String message, String nodeId) {
        return new FlowValidateResult.Issue(code, message, nodeId);
    }
}
