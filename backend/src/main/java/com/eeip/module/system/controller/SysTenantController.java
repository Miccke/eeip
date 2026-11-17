package com.eeip.module.system.controller;

import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import com.eeip.module.system.dto.TenantProvisionRequest;
import com.eeip.module.system.dto.TenantSaveRequest;
import com.eeip.module.system.service.TenantService;
import com.eeip.module.system.vo.TenantProvisionResult;
import com.eeip.module.system.vo.TenantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理接口（平台运营能力，仅集团超管可见）。
 */
@Tag(name = "系统 · 租户管理", description = "租户的增删改查；租户内数据由多租户拦截器自动隔离")
@RestController
@RequestMapping("/api/v1/system/tenants")
@RequiredArgsConstructor
public class SysTenantController {

    private final TenantService tenantService;

    @Operation(summary = "租户列表", description = "平台级数据，不做租户过滤；仅授予集团超管。")
    @GetMapping
    @PreAuthorize("hasAuthority('system:tenant:list')")
    public R<List<TenantVO>> list(
            @Parameter(description = "编码或名称，模糊匹配", example = "SUPPLIER")
            @RequestParam(required = false) String keyword) {
        return R.ok(tenantService.list(keyword));
    }

    @Operation(summary = "新增租户", description = "仅创建租户记录；租户内角色与管理员需后续初始化。")
    @PostMapping
    @PreAuthorize("hasAuthority('system:tenant:create')")
    @AuditLog(action = "TENANT_CREATE", actionName = "新增租户",
            resourceType = "TENANT", resourceId = "#request.code()")
    public R<Long> create(@RequestBody @Valid TenantSaveRequest request) {
        return R.ok(tenantService.create(request));
    }

    @Operation(summary = "一键开通租户",
            description = "一次调用完成：建租户 → 建内置角色 → 建管理员 → 授权。初始密码仅在本次响应中返回。")
    @PostMapping("/provision")
    @PreAuthorize("hasAuthority('system:tenant:create')")
    @AuditLog(action = "TENANT_PROVISION", actionName = "开通租户",
            resourceType = "TENANT", resourceId = "#request.code()")
    public R<TenantProvisionResult> provision(@RequestBody @Valid TenantProvisionRequest request) {
        return R.ok(tenantService.provision(request));
    }

    @Operation(summary = "编辑租户", description = "租户编码不可修改。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:tenant:update')")
    @AuditLog(action = "TENANT_UPDATE", actionName = "编辑租户",
            resourceType = "TENANT", resourceId = "#id")
    public R<Void> update(
            @Parameter(description = "租户 ID", required = true) @PathVariable Long id,
            @RequestBody @Valid TenantSaveRequest request) {
        tenantService.update(id, request);
        return R.ok();
    }

    @Operation(summary = "删除租户", description = "默认租户不允许删除。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:tenant:delete')")
    @AuditLog(action = "TENANT_DELETE", actionName = "删除租户",
            resourceType = "TENANT", resourceId = "#id")
    public R<Void> delete(
            @Parameter(description = "租户 ID", required = true) @PathVariable Long id) {
        tenantService.delete(id);
        return R.ok();
    }
}
