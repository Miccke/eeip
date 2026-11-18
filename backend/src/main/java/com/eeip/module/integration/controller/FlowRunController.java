package com.eeip.module.integration.controller;

import com.eeip.common.api.ErrorCode;
import com.eeip.common.api.PageResult;
import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import com.eeip.common.exception.BizException;
import com.eeip.module.integration.entity.IntFlow;
import com.eeip.module.integration.entity.IntFlowInstance;
import com.eeip.module.integration.entity.IntFlowInstanceNode;
import com.eeip.module.integration.engine.FlowEngine;
import com.eeip.module.integration.mapper.IntFlowInstanceMapper;
import com.eeip.module.integration.mapper.IntFlowInstanceNodeMapper;
import com.eeip.module.integration.mapper.IntFlowMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程运行与实例查询（执行引擎 v0）。
 */
@Tag(name = "集成 · 流程运行", description = "试运行、实例列表与节点轨迹")
@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
public class FlowRunController {

    private final FlowEngine engine;
    private final IntFlowMapper flowMapper;
    private final IntFlowInstanceMapper instanceMapper;
    private final IntFlowInstanceNodeMapper nodeMapper;

    @Operation(summary = "试运行流程", description = "传入触发入参，同步执行并返回每个节点的输入/输出/耗时。")
    @PostMapping("/flows/{id}/run")
    @PreAuthorize("hasAuthority('asset:flow:run')")
    @AuditLog(action = "FLOW_RUN", actionName = "试运行流程",
            resourceType = "FLOW", resourceId = "#id")
    public R<FlowEngine.ExecutionResult> run(
            @Parameter(description = "流程 ID") @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> input) {
        IntFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程不存在");
        }
        return R.ok(engine.run(flow, input == null ? Map.of() : input, "MANUAL"));
    }

    @Operation(summary = "实例列表")
    @GetMapping("/instances")
    @PreAuthorize("hasAuthority('asset:flow:run')")
    public R<PageResult<IntFlowInstance>> instances(
            @Parameter(description = "流程 ID（可选）") @RequestParam(required = false) Long flowId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        Page<IntFlowInstance> page = instanceMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<IntFlowInstance>lambdaQuery()
                        .eq(flowId != null, IntFlowInstance::getFlowId, flowId)
                        .orderByDesc(IntFlowInstance::getStartedAt));
        return R.ok(PageResult.of(page));
    }

    @Operation(summary = "实例详情", description = "含每个节点的执行轨迹，可用于瀑布图。")
    @GetMapping("/instances/{id}")
    @PreAuthorize("hasAuthority('asset:flow:run')")
    public R<Map<String, Object>> instance(@PathVariable Long id) {
        IntFlowInstance inst = instanceMapper.selectById(id);
        if (inst == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "实例不存在");
        }
        List<IntFlowInstanceNode> nodes = nodeMapper.selectList(
                Wrappers.<IntFlowInstanceNode>lambdaQuery()
                        .eq(IntFlowInstanceNode::getInstanceId, id)
                        .orderByAsc(IntFlowInstanceNode::getSeq));
        return R.ok(Map.of("instance", inst, "nodes", nodes));
    }
}
