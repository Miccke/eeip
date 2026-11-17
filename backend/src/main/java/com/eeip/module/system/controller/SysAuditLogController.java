package com.eeip.module.system.controller;

import com.eeip.common.api.PageResult;
import com.eeip.common.api.R;
import com.eeip.module.system.query.AuditLogQuery;
import com.eeip.module.system.service.AuditQueryService;
import com.eeip.module.system.vo.AuditActionVO;
import com.eeip.module.system.vo.AuditLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审计日志查询接口（只读）。
 */
@Tag(name = "运营 · 审计日志", description = "操作审计的检索与动作字典")
@RestController
@RequestMapping("/api/v1/ops/audit-logs")
@RequiredArgsConstructor
public class SysAuditLogController {

    private final AuditQueryService auditQueryService;

    @Operation(summary = "分页查询审计日志",
            description = "支持按操作人、动作、结果、资源类型与时间范围过滤，按发生时间倒序。")
    @GetMapping
    @PreAuthorize("hasAuthority('ops:audit:list')")
    public R<PageResult<AuditLogVO>> page(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") Long pageNum,

            @Parameter(description = "每页条数，最大 200", example = "20")
            @RequestParam(defaultValue = "20") Long pageSize,

            @Parameter(description = "操作人（工号或姓名，模糊）", example = "1001")
            @RequestParam(required = false) String operator,

            @Parameter(description = "动作编码", example = "USER_CREATE")
            @RequestParam(required = false) String action,

            @Parameter(description = "结果", example = "SUCCESS")
            @RequestParam(required = false) String result,

            @Parameter(description = "资源类型", example = "USER")
            @RequestParam(required = false) String resourceType,

            @Parameter(description = "起始时间（ISO 8601）", example = "2026-11-01T00:00:00+08:00")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "结束时间（ISO 8601）", example = "2026-11-30T23:59:59+08:00")
            @RequestParam(required = false) String endTime) {

        return R.ok(auditQueryService.page(new AuditLogQuery(
                pageNum, pageSize, operator, action, result, resourceType, startTime, endTime)));
    }

    @Operation(summary = "查询已出现过的审计动作",
            description = "用于前端筛选下拉，按动作编码+中文名去重。")
    @GetMapping("/actions")
    @PreAuthorize("hasAuthority('ops:audit:list')")
    public R<List<AuditActionVO>> actions() {
        return R.ok(auditQueryService.actions());
    }
}
