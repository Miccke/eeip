package com.eeip.module.integration.controller;

import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import com.eeip.module.integration.dto.ConnectionSaveRequest;
import com.eeip.module.integration.service.ConnectionService;
import com.eeip.module.integration.vo.ConnectionTestResult;
import com.eeip.module.integration.vo.ConnectionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 连接管理接口。
 */
@Tag(name = "集成 · 连接管理", description = "外部系统的连接配置与连通性测试")
@RestController
@RequestMapping("/api/v1/integration/connections")
@RequiredArgsConstructor
public class IntConnectionController {

    private final ConnectionService connectionService;

    @Operation(summary = "连接列表", description = "不返回任何凭证字段。")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:connection:list')")
    public R<List<ConnectionVO>> list(
            @Parameter(description = "编码或名称，模糊匹配", example = "github")
            @RequestParam(required = false) String keyword) {
        return R.ok(connectionService.list(keyword));
    }

    @Operation(summary = "新增连接")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:connection:create')")
    @AuditLog(action = "CONNECTION_CREATE", actionName = "新增连接",
            resourceType = "CONNECTION", resourceId = "#request.code()")
    public R<Long> create(@RequestBody @Valid ConnectionSaveRequest request) {
        return R.ok(connectionService.create(request));
    }

    @Operation(summary = "编辑连接", description = "authConfig 留空表示沿用原凭证。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:connection:update')")
    @AuditLog(action = "CONNECTION_UPDATE", actionName = "编辑连接",
            resourceType = "CONNECTION", resourceId = "#id")
    public R<Void> update(
            @Parameter(description = "连接 ID", required = true) @PathVariable Long id,
            @RequestBody @Valid ConnectionSaveRequest request) {
        connectionService.update(id, request);
        return R.ok();
    }

    @Operation(summary = "删除连接")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:connection:delete')")
    @AuditLog(action = "CONNECTION_DELETE", actionName = "删除连接",
            resourceType = "CONNECTION", resourceId = "#id")
    public R<Void> delete(
            @Parameter(description = "连接 ID", required = true) @PathVariable Long id) {
        connectionService.delete(id);
        return R.ok();
    }

    @Operation(summary = "测试连通性", description = "发起一次真实请求，结果写入连接记录的 lastCheck* 字段。")
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('asset:connection:test')")
    @AuditLog(action = "CONNECTION_TEST", actionName = "测试连接",
            resourceType = "CONNECTION", resourceId = "#id")
    public R<ConnectionTestResult> test(
            @Parameter(description = "连接 ID", required = true) @PathVariable Long id) {
        return R.ok(connectionService.test(id));
    }

    @Operation(summary = "保存前预检", description = "不落库，直接按参数发起一次调用，用于表单里试地址。")
    @PostMapping("/test-draft")
    @PreAuthorize("hasAuthority('asset:connection:test')")
    public R<ConnectionTestResult> testDraft(@RequestBody @Valid ConnectionSaveRequest request) {
        return R.ok(connectionService.testDraft(request));
    }
}
