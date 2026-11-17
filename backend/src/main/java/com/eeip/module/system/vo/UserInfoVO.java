package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 当前登录用户信息（含权限与菜单）。
 */
@Schema(description = "当前用户信息")
public record UserInfoVO(

        @Schema(description = "用户 ID", example = "1001")
        Long id,

        @Schema(description = "用户名", example = "admin")
        String username,

        @Schema(description = "姓名", example = "王工")
        String nickname,

        @Schema(description = "头像地址", example = "https://...")
        String avatar,

        @Schema(description = "部门", example = "IT/集成组")
        String deptName,

        @Schema(description = "角色编码列表", example = "[\"SUPER_ADMIN\"]")
        List<String> roles,

        @Schema(description = "权限点列表", example = "[\"system:user:list\",\"flow:create\"]")
        List<String> perms,

        @Schema(description = "按权限过滤后的菜单树")
        List<MenuVO> menus
) {
}
