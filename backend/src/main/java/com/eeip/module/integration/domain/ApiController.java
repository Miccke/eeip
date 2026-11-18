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
 * 接口台账：系统下有哪些接口、什么契约、绑定哪些连接器。
 */
@Tag(name = "集成 · 接口台账", description = "接口身份、契约版本与连接器绑定")
@RestController
@RequestMapping("/api/v1/integration/apis")
@RequiredArgsConstructor
public class ApiController {

    private final ApiService service;

    @Operation(summary = "接口列表")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:api:list')")
    public R<List<ApiVO>> list(
            @Parameter(description = "系统编码") @RequestParam(required = false) String systemCode,
            @Parameter(description = "编码/名称") @RequestParam(required = false) String keyword) {
        return R.ok(service.list(systemCode, keyword));
    }

    @Operation(summary = "登记接口", description = "同时创建 1.0.0 草稿契约并绑定连接器。")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:api:create')")
    @AuditLog(action = "API_CREATE", actionName = "登记接口",
            resourceType = "API", resourceId = "#request.code()")
    public R<Long> create(@RequestBody @Valid ApiSaveRequest request) {
        return R.ok(service.create(request));
    }

    @Operation(summary = "编辑接口")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:api:update')")
    @AuditLog(action = "API_UPDATE", actionName = "编辑接口",
            resourceType = "API", resourceId = "#id")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid ApiSaveRequest request) {
        service.update(id, request);
        return R.ok();
    }

    @Operation(summary = "发布接口", description = "需要至少绑定一个连接器；发布后对外路径生效。")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('asset:api:publish')")
    @AuditLog(action = "API_PUBLISH", actionName = "发布接口",
            resourceType = "API", resourceId = "#id")
    public R<ApiVO> publish(@PathVariable Long id) {
        return R.ok(service.publish(id));
    }

    @Operation(summary = "下线接口")
    @PostMapping("/{id}/offline")
    @PreAuthorize("hasAuthority('asset:api:offline')")
    @AuditLog(action = "API_OFFLINE", actionName = "下线接口",
            resourceType = "API", resourceId = "#id")
    public R<Void> offline(@PathVariable Long id) {
        service.offline(id);
        return R.ok();
    }

    @Operation(summary = "删除接口")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:api:delete')")
    @AuditLog(action = "API_DELETE", actionName = "删除接口",
            resourceType = "API", resourceId = "#id")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
