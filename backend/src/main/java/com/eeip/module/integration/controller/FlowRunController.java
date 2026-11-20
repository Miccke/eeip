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

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.eeip.module.integration.vo.InstanceStatsVO;
import com.eeip.module.integration.vo.InstanceVO;

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

    @Operation(summary = "实例列表", description = "支持按流程、状态、触发方式与时间范围过滤。")
    @GetMapping("/instances")
    @PreAuthorize("hasAuthority('run:instance:list')")
    public R<PageResult<InstanceVO>> instances(
            @Parameter(description = "流程 ID（可选）") @RequestParam(required = false) Long flowId,
            @Parameter(description = "流程编码（可选）") @RequestParam(required = false) String flowCode,
            @Parameter(description = "状态：SUCCESS / FAILED / RUNNING") @RequestParam(required = false) String status,
            @Parameter(description = "触发方式：MANUAL / SCHEDULE / HTTP") @RequestParam(required = false) String triggerType,
            @Parameter(description = "开始时间起（ISO8601）") @RequestParam(required = false) String from,
            @Parameter(description = "开始时间止（ISO8601）") @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {

        Page<IntFlowInstance> page = instanceMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<IntFlowInstance>lambdaQuery()
                        .eq(flowId != null, IntFlowInstance::getFlowId, flowId)
                        .eq(flowCode != null && !flowCode.isBlank(), IntFlowInstance::getFlowCode, flowCode)
                        .eq(status != null && !status.isBlank(), IntFlowInstance::getStatus, status)
                        .eq(triggerType != null && !triggerType.isBlank(),
                                IntFlowInstance::getTriggerType, triggerType)
                        .ge(from != null && !from.isBlank(), IntFlowInstance::getStartedAt, parseTime(from))
                        .le(to != null && !to.isBlank(), IntFlowInstance::getStartedAt, parseTime(to))
                        .orderByDesc(IntFlowInstance::getStartedAt));

        // 补流程名：一次批量查询，避免 N+1
        List<Long> flowIds = page.getRecords().stream()
                .map(IntFlowInstance::getFlowId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = flowIds.isEmpty() ? Map.of()
                : flowMapper.selectBatchIds(flowIds).stream()
                    .collect(Collectors.toMap(IntFlow::getId, IntFlow::getFlowName, (a, b) -> a));

        Page<InstanceVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(i -> toVO(i, nameMap.get(i.getFlowId())))
                .toList());
        return R.ok(PageResult.of(voPage));
    }

    @Operation(summary = "运行统计", description = "按时间窗口统计成功率、平均与最大耗时。")
    @GetMapping("/instances/stats")
    @PreAuthorize("hasAuthority('run:instance:list')")
    public R<InstanceStatsVO> stats(
            @Parameter(description = "统计窗口（小时），默认 24") @RequestParam(defaultValue = "24") int hours) {
        OffsetDateTime from = OffsetDateTime.now().minusHours(Math.max(1, hours));
        List<IntFlowInstance> list = instanceMapper.selectList(
                Wrappers.<IntFlowInstance>lambdaQuery()
                        .ge(IntFlowInstance::getStartedAt, from));
        long total = list.size();
        long success = list.stream().filter(i -> "SUCCESS".equals(i.getStatus())).count();
        long failed = list.stream().filter(i -> "FAILED".equals(i.getStatus())).count();
        long running = list.stream().filter(i -> "RUNNING".equals(i.getStatus())).count();
        long avg = total == 0 ? 0 : Math.round(list.stream()
                .mapToInt(i -> i.getCostMs() == null ? 0 : i.getCostMs()).average().orElse(0));
        long max = list.stream().mapToInt(i -> i.getCostMs() == null ? 0 : i.getCostMs()).max().orElse(0);
        double rate = total == 0 ? 0 : Math.round(success * 1000.0 / total) / 10.0;
        return R.ok(new InstanceStatsVO(total, success, failed, running, rate, avg, max, hours));
    }

    @Operation(summary = "实例详情", description = "含每个节点的执行轨迹，可用于瀑布图。")
    @GetMapping("/instances/{id}")
    @PreAuthorize("hasAuthority('run:instance:list')")
    public R<Map<String, Object>> instance(@PathVariable Long id) {
        IntFlowInstance inst = instanceMapper.selectById(id);
        if (inst == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "实例不存在");
        }
        List<IntFlowInstanceNode> nodes = nodeMapper.selectList(
                Wrappers.<IntFlowInstanceNode>lambdaQuery()
                        .eq(IntFlowInstanceNode::getInstanceId, id)
                        .orderByAsc(IntFlowInstanceNode::getSeq));
        IntFlow flow = flowMapper.selectById(inst.getFlowId());
        return R.ok(Map.of(
                "instance", toVO(inst, flow == null ? null : flow.getFlowName()),
                "nodes", nodes));
    }

    private String parseTime(String v) {
        try {
            return OffsetDateTime.parse(v.length() == 10 ? v + "T00:00:00+08:00" : v).toString();
        } catch (Exception e) {
            return v;
        }
    }

    private InstanceVO toVO(IntFlowInstance i, String flowName) {
        return new InstanceVO(
                String.valueOf(i.getId()),
                i.getFlowId() == null ? null : String.valueOf(i.getFlowId()),
                i.getFlowCode(), flowName,
                i.getVersionId() == null ? null : String.valueOf(i.getVersionId()),
                i.getStatus(), i.getTriggerType(), i.getCostMs(), i.getTraceId(),
                i.getStartedAt(), i.getFinishedAt(), i.getError(), null);
    }
}
