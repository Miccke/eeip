package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 角色列表项。
 */
@Schema(description = "角色")
public record RoleVO(

        @Schema(description = "角色 ID", example = "2003")
        Long id,

        @Schema(description = "角色编码", example = "DEVELOPER")
        String code,

        @Schema(description = "角色名称", example = "集成开发工程师")
        String name,

        @Schema(description = "数据范围：ALL/TENANT/PROJECT/SELF", example = "PROJECT")
        String dataScope,

        @Schema(description = "排序值", example = "3")
        Integer sort,

        @Schema(description = "状态：1 启用，0 停用", example = "1")
        Integer status,

        @Schema(description = "是否内置角色（内置角色不可删除）", example = "true")
        Boolean builtin,

        @Schema(description = "备注", example = "编排与调试")
        String remark,

        @Schema(description = "持有该角色的用户数", example = "1")
        long userCount
) {
}
