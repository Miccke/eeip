package com.eeip.module.system.controller;

import com.eeip.common.api.R;
import com.eeip.common.security.LoginUser;
import com.eeip.common.security.SecurityUtils;
import com.eeip.common.tenant.TenantContext;
import com.eeip.module.system.service.PermissionService;
import com.eeip.module.system.vo.PermNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限点查询接口。
 */
@Tag(name = "系统 · 权限点", description = "权限树查询，供角色分配权限使用")
@RestController
@RequestMapping("/api/v1/system/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "权限树",
            description = "返回目录/菜单/按钮三合一的完整权限树，节点带权限编码，用于角色授权。")
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('system:role:assign','system:menu:list')")
    public R<List<PermNodeVO>> tree() {
        LoginUser user = SecurityUtils.currentUser();
        // 只有平台租户的超管才能看到并分配平台级权限
        boolean platformAdmin = user != null
                && user.roles().contains("SUPER_ADMIN")
                && TenantContext.DEFAULT_TENANT_ID.equals(user.tenantId());
        return R.ok(permissionService.allPermissionTree(platformAdmin));
    }
}
