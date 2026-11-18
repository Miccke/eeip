package com.eeip.module.integration.nodetype;

import com.eeip.common.api.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 节点类型下发接口。
 *
 * <p>前端启动时拉一次并缓存；调色板、画布、属性面板全部按这里的数据渲染，
 * 不再维护静态 TS 注册表。</p>
 */
@Tag(name = "集成 · 节点类型", description = "编排器的节点规格清单（Manifest）")
@RestController
@RequestMapping("/api/v1/integration/node-types")
@RequiredArgsConstructor
public class NodeTypeController {

    private final NodeTypeRegistry registry;

    @Operation(summary = "节点规格清单",
            description = "返回全部已注册节点（每类最高版本）。前端用它渲染调色板与属性面板。")
    @GetMapping
    public R<List<NodeManifest>> list(
            @io.swagger.v3.oas.annotations.Parameter(description = "按分组过滤", example = "invoke")
            @RequestParam(required = false) String category) {
        List<NodeManifest> all = registry.list();
        if (category != null && !category.isBlank()) {
            all = all.stream()
                    .filter(m -> m.meta() != null && category.equals(m.meta().category()))
                    .toList();
        }
        return R.ok(all);
    }

    @Operation(summary = "单个节点规格", description = "按类型取（最高版本）。")
    @GetMapping("/{type}")
    public R<NodeManifest> get(@org.springframework.web.bind.annotation.PathVariable String type) {
        NodeManifest m = registry.get(type);
        return m == null ? R.fail(com.eeip.common.api.ErrorCode.NOT_FOUND, "节点类型不存在：" + type)
                : R.ok(m);
    }
}
