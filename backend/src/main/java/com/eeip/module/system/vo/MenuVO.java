package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 前端菜单节点（目录或页面菜单）。
 */
@Schema(description = "菜单节点")
public record MenuVO(

        @Schema(description = "菜单 ID", example = "3101")
        Long id,

        @Schema(description = "菜单名称", example = "API 资产")
        String name,

        @Schema(description = "前端路由路径", example = "/asset/api")
        String path,

        @Schema(description = "前端组件标识", example = "asset/api/index")
        String component,

        @Schema(description = "图标标识", example = "api")
        String icon,

        @Schema(description = "节点类型：DIR 目录 / MENU 菜单", example = "MENU")
        String type,

        @Schema(description = "是否在菜单中隐藏", example = "false")
        Boolean hidden,

        @Schema(description = "子菜单（目录才有）")
        List<MenuVO> children
) {
}
