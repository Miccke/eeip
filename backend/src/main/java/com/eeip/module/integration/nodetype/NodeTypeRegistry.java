package com.eeip.module.integration.nodetype;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 节点类型注册中心。
 *
 * <p>启动时扫描 `classpath:node-packages/&#42;/node.json`，把每个 manifest 注册进来。
 * 后续扩展包（上传的 zip）走 {@link #register(NodeManifest, String)} 动态注册。</p>
 *
 * <p>冲突规则：同 type 同 version 后加载覆盖并 WARN；同 type 不同 version 并存，
 * 取版本号最大的作为"新建流程默认版本"。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeTypeRegistry {

    private final ObjectMapper objectMapper;

    /** type → version → manifest（version 有序，便于取最高版本） */
    private final Map<String, NavigableMap<String, NodeManifest>> registry = new LinkedHashMap<>();
    /** type → 来源（builtin / 包名），便于运维排查 */
    private final Map<String, String> source = new HashMap<>();

    @PostConstruct
    public void loadBuiltin() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath*:node-packages/*/node.json");
            for (Resource r : resources) {
                try (InputStream in = r.getInputStream()) {
                    NodeManifest m = objectMapper.readValue(in, NodeManifest.class);
                    register(m, "builtin");
                } catch (Exception e) {
                    log.error("节点 manifest 解析失败：{}", r.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.error("扫描 node-packages 失败", e);
        }
        log.info("节点类型注册完成，共 {} 种：{}", registry.size(), registry.keySet());
    }

    /**
     * 注册一个节点规格。
     *
     * @param m      规格
     * @param origin 来源标识（builtin / 包名）
     */
    public void register(NodeManifest m, String origin) {
        if (m.type() == null || m.type().isBlank()) {
            log.warn("跳过无 type 的 manifest（来源 {}）", origin);
            return;
        }
        String ver = m.version() == null ? "0.0.0" : m.version();
        NavigableMap<String, NodeManifest> byVersion =
                registry.computeIfAbsent(m.type(), k -> new TreeMap<>());
        if (byVersion.containsKey(ver)) {
            log.warn("节点 {}@{} 重复定义，覆盖（来源 {}）", m.type(), ver, origin);
        }
        byVersion.put(ver, m);
        source.put(m.type(), origin);
    }

    /** 全部类型（每类取最高版本） */
    public List<NodeManifest> list() {
        return registry.values().stream()
                .map(byVer -> byVer.lastEntry().getValue())
                .sorted(Comparator.comparing(m -> m.type()))
                .collect(Collectors.toList());
    }

    /** 取某类型（最高版本） */
    public NodeManifest get(String type) {
        NavigableMap<String, NodeManifest> byVersion = registry.get(type);
        return byVersion == null ? null : byVersion.lastEntry().getValue();
    }

    /** 取某类型指定版本 */
    public NodeManifest get(String type, String version) {
        NavigableMap<String, NodeManifest> byVersion = registry.get(type);
        if (byVersion == null) {
            return null;
        }
        if (version == null || version.isBlank()) {
            return byVersion.lastEntry().getValue();
        }
        return byVersion.get(version);
    }

    public boolean contains(String type) {
        return registry.containsKey(type);
    }

    /** 已注册的类型名集合（校验器用） */
    public Set<String> types() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    /** 校验器可以据此判断"目录清单里登记的版本" */
    public List<String> versionsOf(String type) {
        return registry.containsKey(type) ? List.copyOf(registry.get(type).keySet()) : List.of();
    }
}
