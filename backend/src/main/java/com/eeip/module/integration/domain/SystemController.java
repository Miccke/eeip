package com.eeip.module.integration.domain;

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
 * 生产方系统注册与台账。
 */
@Tag(name = "集成 · 系统注册", description = "生产方系统台账：连接器与接口的归属")
@RestController
@RequestMapping("/api/v1/integration/systems")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService service;

    @Operation(summary = "系统列表")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:system:list')")
    public R<List<SystemVO>> list(
            @Parameter(description = "编码/名称模糊匹配") @RequestParam(required = false) String keyword) {
        return R.ok(service.list(keyword));
    }

    @Operation(summary = "注册系统")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:system:create')")
    @AuditLog(action = "SYSTEM_CREATE", actionName = "注册系统",
            resourceType = "SYSTEM", resourceId = "#request.code()")
    public R<Long> create(@RequestBody @Valid SystemSaveRequest request) {
        return R.ok(service.create(request));
    }

    @Operation(summary = "编辑系统")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:system:update')")
    @AuditLog(action = "SYSTEM_UPDATE", actionName = "编辑系统",
            resourceType = "SYSTEM", resourceId = "#id")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid SystemSaveRequest request) {
        service.update(id, request);
        return R.ok();
    }

    @Operation(summary = "删除系统", description = "系统下仍有连接器或接口时禁止删除。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:system:delete')")
    @AuditLog(action = "SYSTEM_DELETE", actionName = "删除系统",
            resourceType = "SYSTEM", resourceId = "#id")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
