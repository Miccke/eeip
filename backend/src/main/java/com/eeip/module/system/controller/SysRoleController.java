package com.eeip.module.system.controller;

import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import com.eeip.module.system.dto.AssignPermRequest;
import com.eeip.module.system.dto.RoleSaveRequest;
import com.eeip.module.system.service.RoleManageService;
import com.eeip.module.system.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口。
 *
 * <p>角色是 RBAC 的核心：决定菜单可见性（DIR/MENU）、按钮权限（BUTTON）与数据范围（dataScope）。</p>
 */
@Tag(name = "系统 · 角色管理", description = "角色的增删改查与权限分配")
@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final RoleManageService roleManageService;

    @Operation(summary = "角色列表", description = "返回全部角色及成员数量，用于用户编辑时的角色选择与角色页表格。")
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<RoleVO>> list(
            @Parameter(description = "角色名称，模糊匹配", example = "开发")
            @RequestParam(required = false) String keyword) {
        return R.ok(roleManageService.list(keyword));
    }

    @Operation(summary = "新增角色", description = "角色编码需为大写蛇形且唯一。")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:create')")
    @AuditLog(action = "ROLE_CREATE", actionName = "新增角色",
            resourceType = "ROLE", resourceId = "#request.code()")
    public R<Long> create(@RequestBody @Valid RoleSaveRequest request) {
        return R.ok(roleManageService.create(request));
    }

    @Operation(summary = "编辑角色", description = "内置角色不允许修改。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    @AuditLog(action = "ROLE_UPDATE", actionName = "编辑角色",
            resourceType = "ROLE", resourceId = "#id")
    public R<Void> update(
            @Parameter(description = "角色 ID", required = true) @PathVariable Long id,
            @RequestBody @Valid RoleSaveRequest request) {
        roleManageService.update(id, request);
        return R.ok();
    }

    @Operation(summary = "删除角色", description = "已分配给用户的角色不允许删除。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @AuditLog(action = "ROLE_DELETE", actionName = "删除角色",
            resourceType = "ROLE", resourceId = "#id")
    public R<Void> delete(
            @Parameter(description = "角色 ID", required = true) @PathVariable Long id) {
        roleManageService.delete(id);
        return R.ok();
    }

    @Operation(summary = "查询角色已分配的权限 ID",
            description = "返回权限 ID 列表，前端用于回显权限树的勾选状态。")
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:assign')")
    public R<List<Long>> permissions(
            @Parameter(description = "角色 ID", required = true) @PathVariable Long id) {
        return R.ok(roleManageService.permissionIds(id));
    }

    @Operation(summary = "分配权限",
            description = "全量覆盖角色权限；保存后该角色下所有用户的登录态缓存立即失效，下次请求即按新权限生效。")
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:assign')")
    @AuditLog(action = "ROLE_ASSIGN_PERM", actionName = "分配权限",
            resourceType = "ROLE", resourceId = "#id")
    public R<Void> assign(
            @Parameter(description = "角色 ID", required = true) @PathVariable Long id,
            @RequestBody @Valid AssignPermRequest request) {
        roleManageService.assignPermissions(id, request.permissionIds());
        return R.ok();
    }
}
