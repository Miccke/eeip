package com.eeip.module.integration.domain;

import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 业务领域管理。 */
@Tag(name = "集成 · 领域管理", description = "集团业务领域，用于系统与接口归类")
@RestController
@RequestMapping("/api/v1/integration/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService service;

    @Operation(summary = "领域列表")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:domain:list')")
    public R<List<Domain>> list() {
        return R.ok(service.list());
    }

    @Operation(summary = "新增领域")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:domain:create')")
    @AuditLog(action = "DOMAIN_CREATE", actionName = "新增领域",
            resourceType = "DOMAIN", resourceId = "#domain.code")
    public R<Long> create(@RequestBody Domain domain) {
        return R.ok(service.create(domain));
    }

    @Operation(summary = "编辑领域")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:domain:update')")
    @AuditLog(action = "DOMAIN_UPDATE", actionName = "编辑领域",
            resourceType = "DOMAIN", resourceId = "#id")
    public R<Void> update(@PathVariable Long id, @RequestBody Domain domain) {
        service.update(id, domain);
        return R.ok();
    }

    @Operation(summary = "删除领域", description = "领域下仍有系统时禁止删除。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:domain:delete')")
    @AuditLog(action = "DOMAIN_DELETE", actionName = "删除领域",
            resourceType = "DOMAIN", resourceId = "#id")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
