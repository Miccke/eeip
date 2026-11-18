package com.eeip.module.integration.nodetype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 节点规格（Manifest）。
 *
 * <p>一个节点 = 一个 `node.json`，是前端调色板/画布/表单与后端校验/执行的**唯一事实来源**。
 * 结构见《13-流程编排器设计》§6.2。</p>
 *
 * <p>注意：宽松反序列化（忽略未知字段），保证新增字段不会让老版本服务启动失败。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "节点规格")
public record NodeManifest(

        @Schema(description = "包类型，固定 eeip/node", example = "eeip/node")
        String kind,

        @Schema(description = "manifest 版本", example = "1.0")
        String apiVersion,

        @Schema(description = "节点类型，全局唯一", example = "HTTP_INVOKE")
        String type,

        @Schema(description = "节点版本", example = "1.0.0")
        String version,

        @Schema(description = "元信息")
        Meta meta,

        @Schema(description = "端口定义")
        Ports ports,

        @Schema(description = "配置的 JSON Schema")
        Map<String, Object> config,

        @Schema(description = "行为契约：超时/重试/幂等/错误策略")
        Map<String, Object> behavior,

        @Schema(description = "执行器定位")
        Map<String, Object> runtime,

        @Schema(description = "兼容性声明")
        Map<String, Object> compat,

        @Schema(description = "使用该节点所需的权限点")
        List<String> permissions
) {

    /** 元信息 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "节点元信息")
    public record Meta(
            @Schema(description = "调色板分组", example = "invoke") String category,
            @Schema(description = "多语言显示名") Map<String, String> displayName,
            @Schema(description = "多语言描述") Map<String, String> description,
            @Schema(description = "主色", example = "#8A6FD4") String color,
            @Schema(description = "画布尺寸") Map<String, Integer> size,
            @Schema(description = "画布摘要模板", example = "${config.connectionCode}") String summary,
            @Schema(description = "调色板排序（同组内升序）", example = "50") Integer order,
            @Schema(description = "搜索关键字") List<String> keywords
    ) {
    }

    /** 端口集合 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "端口集合")
    public record Ports(
            @Schema(description = "输入端口") List<Port> inputs,
            @Schema(description = "输出端口") List<Port> outputs
    ) {
    }

    /** 单个端口 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "端口")
    public record Port(
            @Schema(description = "端口 ID", example = "out") String id,
            @Schema(description = "多语言标签") Map<String, String> label,
            @Schema(description = "数据类型", example = "HttpResponse") String type,
            @Schema(description = "是否必须有出边", example = "true") Boolean required,
            @Schema(description = "是否允许多条出边", example = "false") Boolean multiple
    ) {
    }
}
