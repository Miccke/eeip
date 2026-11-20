package com.eeip.module.integration.expose;

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
 * 接口开放管理接口（控制面）。
 */
@Tag(name = "集成 · 接口开放", description = "把内部系统接口登记并对外提供：鉴权、限流、转发")
@RestController
@RequestMapping("/api/v1/integration/open-apis")
@RequiredArgsConstructor
public class ApiExposeController {

    private final ApiExposeService service;
    private final com.eeip.module.integration.service.ApiMetricService metricService;

    @Operation(summary = "接口列表")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:api:list')")
    public R<List<ApiExposeVO>> list(
            @Parameter(description = "编码/名称/路径，模糊匹配") @RequestParam(required = false) String keyword) {
        return R.ok(service.list(keyword));
    }

    @Operation(summary = "登记接口")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:api:create')")
    @AuditLog(action = "API_EXPOSE_CREATE", actionName = "登记接口",
            resourceType = "OPEN_API", resourceId = "#request.code()")
    public R<Long> create(@RequestBody @Valid ApiExposeSaveRequest request) {
        return R.ok(service.create(request));
    }

    @Operation(summary = "编辑接口")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:api:update')")
    @AuditLog(action = "API_EXPOSE_UPDATE", actionName = "编辑接口",
            resourceType = "OPEN_API", resourceId = "#id")
    public R<Void> update(@Parameter(description = "ID") @PathVariable Long id,
                          @RequestBody @Valid ApiExposeSaveRequest request) {
        service.update(id, request);
        return R.ok();
    }

    @Operation(summary = "删除接口")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:api:delete')")
    @AuditLog(action = "API_EXPOSE_DELETE", actionName = "删除接口",
            resourceType = "OPEN_API", resourceId = "#id")
    public R<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "调用趋势", description = "按天汇总调用量 / 错误数 / 平均耗时，最多 90 天。")
    @GetMapping("/{id}/metrics")
    @PreAuthorize("hasAuthority('asset:api:list')")
    public R<List<java.util.Map<String, Object>>> metrics(
            @Parameter(description = "ID") @PathVariable Long id,
            @Parameter(description = "天数，默认 30") @RequestParam(defaultValue = "30") int days) {
        return R.ok(metricService.daily(id, Math.min(90, Math.max(1, days))));
    }

    @Operation(summary = "重置 API Key", description = "返回新 Key（仅本次可见）。")
    @PostMapping("/{id}/reset-key")
    @PreAuthorize("hasAuthority('asset:api:update')")
    @AuditLog(action = "API_EXPOSE_RESET_KEY", actionName = "重置 API Key",
            resourceType = "OPEN_API", resourceId = "#id")
    public R<String> resetKey(@Parameter(description = "ID") @PathVariable Long id) {
        return R.ok(service.resetKey(id));
    }
}
