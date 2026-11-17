package com.eeip.module.system.controller;

import com.eeip.common.api.PageResult;
import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import com.eeip.module.system.dto.UserSaveRequest;
import com.eeip.module.system.service.UserManageService;
import com.eeip.module.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理接口。
 *
 * <p>权限点：查询 {@code system:user:list}、新增 {@code system:user:create}、
 * 编辑 {@code system:user:update}、删除 {@code system:user:delete}、
 * 重置密码 {@code system:user:reset}。</p>
 *
 * <p>所有写操作都会记录审计日志（见 {@link AuditLog}）。</p>
 */
@Tag(name = "系统 · 用户管理", description = "账号的增删改查与密码重置")
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final UserManageService userManageService;

    @Operation(summary = "分页查询用户",
            description = "支持按关键字（用户名/姓名/部门）与状态过滤；结果受当前用户数据范围约束。")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<PageResult<UserVO>> page(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNum,

            @Parameter(description = "每页条数", example = "20")
            @RequestParam(defaultValue = "20") long pageSize,

            @Parameter(description = "关键字：用户名 / 姓名 / 部门", example = "李")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "状态：1 启用，0 停用", example = "1")
            @RequestParam(required = false) Integer status) {
        return R.ok(userManageService.page(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "新增用户", description = "用户名唯一；初始密码必须不少于 8 位。")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create')")
    @AuditLog(action = "USER_CREATE", actionName = "新增用户",
            resourceType = "USER", resourceId = "#request.username()")
    public R<Long> create(@RequestBody @Valid UserSaveRequest request) {
        return R.ok(userManageService.create(request));
    }

    @Operation(summary = "编辑用户", description = "密码留空表示不修改；角色为全量覆盖。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    @AuditLog(action = "USER_UPDATE", actionName = "编辑用户",
            resourceType = "USER", resourceId = "#id")
    public R<Void> update(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id,
            @RequestBody @Valid UserSaveRequest request) {
        userManageService.update(id, request);
        return R.ok();
    }

    @Operation(summary = "删除用户", description = "内置 admin 账号不允许删除。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @AuditLog(action = "USER_DELETE", actionName = "删除用户",
            resourceType = "USER", resourceId = "#id")
    public R<Void> delete(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id) {
        userManageService.delete(id);
        return R.ok();
    }

    @Operation(summary = "重置密码", description = "重置后该用户的登录态缓存会立即失效。")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:reset')")
    @AuditLog(action = "USER_RESET_PASSWORD", actionName = "重置密码",
            resourceType = "USER", resourceId = "#id")
    public R<Void> resetPassword(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        userManageService.resetPassword(id, body.get("password"));
        return R.ok();
    }
}
