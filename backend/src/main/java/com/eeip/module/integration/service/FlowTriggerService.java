package com.eeip.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.entity.IntFlow;
import com.eeip.module.integration.entity.IntFlowTrigger;
import com.eeip.module.integration.entity.IntFlowVersion;
import com.eeip.module.integration.mapper.IntFlowTriggerMapper;
import com.eeip.module.integration.mapper.IntFlowVersionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * 触发器同步：把流程 DSL 里的触发器节点落成运行时记录。
 *
 * <p>触发条件：流程已发布（有 current_version_id）。发布时重建，下线时置 OFFLINE。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowTriggerService {

    private final IntFlowTriggerMapper triggerMapper;
    private final IntFlowVersionMapper versionMapper;
    private final ObjectMapper json;

    /** 由 DSL 生成的触发器节点类型 → 触发器类型 */
    private static final Map<String, String> NODE_TO_TRIGGER = Map.of(
            "SCHEDULE_CRON", "SCHEDULE",
            "MQ_SUBSCRIBE", "MQ",
            "EVENT_ON", "EVENT",
            "START", "HTTP"
    );

    /**
     * 发布后重建该流程的触发器。
     *
     * @param flow    流程（含 currentVersionId）
     * @param version 已发布版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void rebuild(IntFlow flow, IntFlowVersion version) {
        // 旧记录全部下掉，按新版本重建（简单可靠，触发器数量很小）
        triggerMapper.delete(Wrappers.<IntFlowTrigger>lambdaQuery()
                .eq(IntFlowTrigger::getFlowId, flow.getId()));

        FlowDsl dsl = parse(version.getDslJson());
        if (dsl == null || dsl.nodes() == null) {
            return;
        }
        int created = 0;
        for (FlowDsl.Node n : dsl.nodes()) {
            String type = NODE_TO_TRIGGER.get(n.type());
            if (type == null) {
                continue;
            }
            IntFlowTrigger t = new IntFlowTrigger();
            t.setFlowId(flow.getId());
            t.setVersionId(version.getId());
            t.setFlowCode(flow.getFlowCode());
            t.setNodeId(n.id());
            t.setTriggerType(type);
            t.setConfigJson(toJson(n.config()));
            t.setStatus("ACTIVE");
            t.setFireCount(0);
            t.setMissCount(0);
            if ("SCHEDULE".equals(type)) {
                t.setNextFireAt(nextFireTime(n.config(), OffsetDateTime.now()));
            }
            triggerMapper.insert(t);
            created++;
        }
        log.info("流程 {} 触发计划重建完成，共 {} 个", flow.getFlowCode(), created);
    }

    /** 下线：触发器置 OFFLINE */
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long flowId) {
        List<IntFlowTrigger> list = triggerMapper.selectList(Wrappers.<IntFlowTrigger>lambdaQuery()
                .eq(IntFlowTrigger::getFlowId, flowId));
        for (IntFlowTrigger t : list) {
            t.setStatus("OFFLINE");
            t.setNextFireAt(null);
            triggerMapper.updateById(t);
        }
    }

    /** 启停某个触发器 */
    @Transactional(rollbackFor = Exception.class)
    public void toggle(Long id, boolean active) {
        IntFlowTrigger t = triggerMapper.selectById(id);
        if (t == null) {
            return;
        }
        OffsetDateTime next = null;
        if (active && "SCHEDULE".equals(t.getTriggerType())) {
            next = nextFireTime(parseConfig(t.getConfigJson()), OffsetDateTime.now());
        }
        // updateById 会跳过 null 字段，暂停时必须用 set 显式清空 next_fire_at
        triggerMapper.update(null, Wrappers.<IntFlowTrigger>lambdaUpdate()
                .set(IntFlowTrigger::getStatus, active ? "ACTIVE" : "PAUSED")
                .set(IntFlowTrigger::getNextFireAt, next)
                .set(IntFlowTrigger::getUpdatedAt, OffsetDateTime.now())
                .eq(IntFlowTrigger::getId, id));
    }

    /** 按 ID 取（调度回写用） */
    public IntFlowTrigger byId(Long id) {
        return id == null ? null : triggerMapper.selectById(id);
    }

    /** 某流程的触发器列表 */
    public List<IntFlowTrigger> listByFlow(Long flowId) {
        return triggerMapper.selectList(Wrappers.<IntFlowTrigger>lambdaQuery()
                .eq(IntFlowTrigger::getFlowId, flowId)
                .orderByAsc(IntFlowTrigger::getNodeId));
    }

    /** 计算下一次触发时间；cron 非法返回 null */
    public OffsetDateTime nextFireTime(Map<String, Object> config, OffsetDateTime from) {
        String cron = config == null ? null : String.valueOf(config.getOrDefault("cron", ""));
        String tz = config == null ? "Asia/Shanghai"
                : String.valueOf(config.getOrDefault("timezone", "Asia/Shanghai"));
        try {
            CronExpression expr = CronExpression.parse(cron);
            ZoneId zone = ZoneId.of(tz);
            ZonedDateTime start = from.atZoneSameInstant(zone);
            ZonedDateTime next = expr.next(start);
            return next == null ? null : next.toOffsetDateTime();
        } catch (Exception e) {
            log.warn("cron 表达式非法：{}（{}）", cron, e.getMessage());
            return null;
        }
    }

    private FlowDsl parse(String dslJson) {
        try {
            return dslJson == null ? null : json.readValue(dslJson, FlowDsl.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseConfig(String cfg) {
        try {
            return cfg == null ? Map.of() : json.readValue(cfg, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String toJson(Object o) {
        try {
            return o == null ? "{}" : json.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** 供调度器使用：取到期触发器 */
    public List<IntFlowTrigger> due(int limit) {
        return triggerMapper.selectDue(OffsetDateTime.now(), limit);
    }

    /** 记录一次触发结果 */
    public void markFired(IntFlowTrigger t, String status, Long instanceId,
                          OffsetDateTime nextFireAt) {
        triggerMapper.update(null, Wrappers.<IntFlowTrigger>lambdaUpdate()
                .set(IntFlowTrigger::getLastFireAt, OffsetDateTime.now())
                .set(IntFlowTrigger::getLastStatus, status)
                .set(IntFlowTrigger::getLastInstance, instanceId)
                .set(IntFlowTrigger::getFireCount,
                        (t.getFireCount() == null ? 0 : t.getFireCount()) + 1)
                .set(IntFlowTrigger::getNextFireAt, nextFireAt)
                .set(IntFlowTrigger::getUpdatedAt, OffsetDateTime.now())
                .eq(IntFlowTrigger::getId, t.getId()));
    }

    /** 版本（供调度时取当前发布版本） */
    public IntFlowVersion version(Long versionId) {
        return versionId == null ? null : versionMapper.selectById(versionId);
    }
}
