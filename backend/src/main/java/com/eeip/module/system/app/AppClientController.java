package com.eeip.module.system.app;

import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用授权管理：A 系统注册、发密钥、授权可调用的接口。
 */
@Tag(name = "系统 · 应用授权", description = "调用方应用注册、密钥与接口授权")
@RestController
@RequestMapping("/api/v1/system/app-clients")
@RequiredArgsConstructor
public class AppClientController {

    private final AppClientService service;

    @Operation(summary = "应用列表")
    @GetMapping
    @PreAuthorize("hasAuthority('system:app:list')")
    public R<List<AppClientVO>> list(
            @Parameter(description = "appKey 或名称") @RequestParam(required = false) String keyword) {
        return R.ok(service.list(keyword));
    }

    @Operation(summary = "创建应用", description = "返回一次性明文密钥，请立即保存。")
    @PostMapping
    @PreAuthorize("hasAuthority('system:app:create')")
    @AuditLog(action = "APP_CLIENT_CREATE", actionName = "创建应用",
            resourceType = "APP_CLIENT", resourceId = "#request.appName()")
    public R<AppClientService.AppClientCreated> create(@RequestBody @Valid AppClientSaveRequest request) {
        return R.ok(service.create(request));
    }

    @Operation(summary = "编辑应用")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:app:update')")
    @AuditLog(action = "APP_CLIENT_UPDATE", actionName = "编辑应用",
            resourceType = "APP_CLIENT", resourceId = "#id")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid AppClientSaveRequest request) {
        service.update(id, request);
        return R.ok();
    }

    @Operation(summary = "删除应用")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:app:delete')")
    @AuditLog(action = "APP_CLIENT_DELETE", actionName = "删除应用",
            resourceType = "APP_CLIENT", resourceId = "#id")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "重置密钥", description = "返回新密钥，旧密钥立即失效。")
    @PostMapping("/{id}/reset-secret")
    @PreAuthorize("hasAuthority('system:app:update')")
    @AuditLog(action = "APP_CLIENT_RESET_SECRET", actionName = "重置应用密钥",
            resourceType = "APP_CLIENT", resourceId = "#id")
    public R<String> resetSecret(@PathVariable Long id) {
        return R.ok(service.resetSecret(id));
    }

    @Operation(summary = "查询已授权接口")
    @GetMapping("/{id}/grants")
    @PreAuthorize("hasAuthority('system:app:list')")
    public R<List<String>> grants(@PathVariable Long id) {
        return R.ok(service.grants(id));
    }

    @Operation(summary = "保存接口授权", description = "覆盖式：传入全部可调用接口 ID。")
    @PutMapping("/{id}/grants")
    @PreAuthorize("hasAuthority('system:app:grant')")
    @AuditLog(action = "APP_CLIENT_GRANT", actionName = "接口授权",
            resourceType = "APP_CLIENT", resourceId = "#id")
    public R<Void> saveGrants(@PathVariable Long id, @RequestBody List<String> apiIds) {
        service.saveGrants(id, apiIds);
        return R.ok();
    }
}
