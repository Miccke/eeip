package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 权限树节点（目录 / 菜单 / 按钮）。
 */
@Schema(description = "权限树节点")
public record PermNodeVO(

        @Schema(description = "权限 ID", example = "3311")
        Long id,

        @Schema(description = "父节点 ID，0 表示根", example = "3301")
        Long parentId,

        @Schema(description = "节点类型：DIR / MENU / BUTTON", example = "BUTTON")
        String type,

        @Schema(description = "权限编码，按钮节点必有", example = "system:user:create")
        String code,

        @Schema(description = "节点名称", example = "新增用户")
        String name,

        @Schema(description = "前端路由路径（菜单节点）", example = "/system/user")
        String path,

        @Schema(description = "图标标识", example = "users")
        String icon,

        @Schema(description = "排序值", example = "1")
        Integer sort,

        @Schema(description = "子节点")
        List<PermNodeVO> children
) {
}
