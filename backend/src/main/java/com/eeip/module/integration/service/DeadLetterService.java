package com.eeip.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eeip.common.api.PageResult;
import com.eeip.module.integration.entity.IntFlow;
import com.eeip.module.integration.entity.IntFlowDeadLetter;
import com.eeip.module.integration.entity.IntFlowInstance;
import com.eeip.module.integration.entity.IntFlowInstanceNode;
import com.eeip.module.integration.mapper.IntFlowDeadLetterMapper;
import com.eeip.module.integration.mapper.IntFlowInstanceMapper;
import com.eeip.module.integration.mapper.IntFlowInstanceNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 死信服务：失败实例入库、查询、重投标记。
 *
 * <p>重投入口在 Controller（需要 FlowEngine），这里只做数据操作，避免与引擎循环依赖。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final IntFlowDeadLetterMapper mapper;
    private final IntFlowInstanceMapper instanceMapper;
    private final IntFlowInstanceNodeMapper nodeMapper;

    /**
     * 实例失败时落死信。同一实例只落一次（唯一索引兜底）。
     *
     * @param inst   失败实例
     * @param flow   流程（取名称）
     * @param error  错误信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void record(IntFlowInstance inst, IntFlow flow, String error) {
        long exists = mapper.selectCount(Wrappers.<IntFlowDeadLetter>lambdaQuery()
                .eq(IntFlowDeadLetter::getInstanceId, inst.getId()));
        if (exists > 0) {
            return;
        }
        // 找最后一个失败节点
        IntFlowInstanceNode bad = nodeMapper.selectOne(Wrappers.<IntFlowInstanceNode>lambdaQuery()
                .eq(IntFlowInstanceNode::getInstanceId, inst.getId())
                .eq(IntFlowInstanceNode::getStatus, "FAILED")
                .orderByDesc(IntFlowInstanceNode::getSeq)
                .last("LIMIT 1"));

        IntFlowDeadLetter d = new IntFlowDeadLetter();
        d.setInstanceId(inst.getId());
        d.setFlowId(inst.getFlowId());
        d.setFlowCode(inst.getFlowCode());
        d.setFlowName(flow == null ? null : flow.getFlowName());
        d.setVersionId(inst.getVersionId());
        d.setError(truncate(error, 1000));
        d.setInputJson(inst.getInputJson());
        d.setAttempts(bad == null || bad.getAttempts() == null ? 1 : bad.getAttempts());
        d.setStatus("PENDING");
        d.setRetryCount(0);
        if (bad != null) {
            d.setNodeId(bad.getNodeId());
            d.setNodeName(bad.getNodeName());
            d.setNodeType(bad.getNodeType());
        }
        mapper.insert(d);
        log.info("实例 {} 执行失败，已写入死信 flow={} node={}", inst.getId(), inst.getFlowCode(), d.getNodeId());
    }

    /** 列表 */
    public PageResult<IntFlowDeadLetter> list(String status, String flowCode, long pageNum, long pageSize) {
        Page<IntFlowDeadLetter> page = mapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<IntFlowDeadLetter>lambdaQuery()
                        .eq(status != null && !status.isBlank(), IntFlowDeadLetter::getStatus, status)
                        .eq(flowCode != null && !flowCode.isBlank(), IntFlowDeadLetter::getFlowCode, flowCode)
                        .orderByDesc(IntFlowDeadLetter::getCreatedAt));
        return PageResult.of(page);
    }

    public IntFlowDeadLetter byId(Long id) {
        return mapper.selectById(id);
    }

    /** 标记为重投（记录新实例） */
    @Transactional(rollbackFor = Exception.class)
    public void markRetried(Long id, Long newInstanceId, String operator) {
        IntFlowDeadLetter d = mapper.selectById(id);
        if (d == null) {
            return;
        }
        d.setStatus("RETRIED");
        d.setRetriedInstance(newInstanceId);
        d.setRetryCount((d.getRetryCount() == null ? 0 : d.getRetryCount()) + 1);
        d.setHandledBy(operator);
        d.setHandledAt(OffsetDateTime.now());
        mapper.updateById(d);
    }

    /** 丢弃 */
    @Transactional(rollbackFor = Exception.class)
    public void discard(Long id, String operator, String remark) {
        IntFlowDeadLetter d = mapper.selectById(id);
        if (d == null) {
            return;
        }
        d.setStatus("DISCARDED");
        d.setHandledBy(operator);
        d.setHandledAt(OffsetDateTime.now());
        d.setRemark(remark);
        mapper.updateById(d);
    }

    /** 统计 */
    public java.util.Map<String, Long> stats() {
        long total = mapper.selectCount(null);
        long pending = mapper.selectCount(Wrappers.<IntFlowDeadLetter>lambdaQuery()
                .eq(IntFlowDeadLetter::getStatus, "PENDING"));
        long retried = mapper.selectCount(Wrappers.<IntFlowDeadLetter>lambdaQuery()
                .eq(IntFlowDeadLetter::getStatus, "RETRIED"));
        long discarded = mapper.selectCount(Wrappers.<IntFlowDeadLetter>lambdaQuery()
                .eq(IntFlowDeadLetter::getStatus, "DISCARDED"));
        return java.util.Map.of("total", total, "pending", pending,
                "retried", retried, "discarded", discarded);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
