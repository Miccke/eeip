package com.eeip.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 用户新增/编辑请求。
 *
 * <p>编辑时 username 不可变更；password 留空表示不修改。</p>
 */
@Schema(description = "用户保存请求")
public record UserSaveRequest(

        @Schema(description = "用户名，字母开头，3-32 位字母/数字/下划线", example = "dev02",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.username.required}")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,31}$", message = "{validation.username.pattern}")
        String username,

        @Schema(description = "姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.nickname.required}")
        @Size(max = 64)
        String nickname,

        @Schema(description = "密码，新建必填；编辑时留空表示不修改", example = "Admin@123")
        @Size(min = 8, max = 64, message = "{validation.password.size}")
        String password,

        @Schema(description = "邮箱", example = "dev02@eeip.local")
        @Email(message = "{validation.email.invalid}")
        String email,

        @Schema(description = "手机号", example = "13800001111")
        @Size(max = 32)
        String phone,

        @Schema(description = "部门名称", example = "IT/集成组")
        @Size(max = 64)
        String deptName,

        @Schema(description = "状态：1 启用，0 停用", example = "1", defaultValue = "1")
        Integer status,

        @Schema(description = "角色 ID 列表（全量覆盖）", example = "[2003]")
        List<Long> roleIds,

        @Schema(description = "备注", example = "负责订单域集成")
        @Size(max = 256)
        String remark
) {
}
