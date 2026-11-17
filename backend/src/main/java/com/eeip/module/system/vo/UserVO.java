package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 用户列表项。
 */
@Schema(description = "用户列表项")
public record UserVO(

        @Schema(description = "用户 ID", example = "1002")
        Long id,

        @Schema(description = "用户名", example = "dev01")
        String username,

        @Schema(description = "姓名", example = "李开发")
        String nickname,

        @Schema(description = "邮箱", example = "dev01@eeip.local")
        String email,

        @Schema(description = "手机号", example = "13800001111")
        String phone,

        @Schema(description = "部门", example = "IT/集成组")
        String deptName,

        @Schema(description = "状态：1 启用，0 停用", example = "1")
        Integer status,

        @Schema(description = "角色 ID 列表", example = "[2003]")
        List<Long> roleIds,

        @Schema(description = "角色名称列表", example = "[\"集成开发工程师\"]")
        List<String> roleNames,

        @Schema(description = "最近登录时间", example = "2026-11-17T21:18:46+08:00")
        OffsetDateTime lastLoginAt,

        @Schema(description = "创建时间", example = "2026-11-17T21:17:37+08:00")
        OffsetDateTime createdAt,

        @Schema(description = "备注", example = "负责订单域集成")
        String remark
) {
}
