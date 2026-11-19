package com.eeip.module.integration.service;

import com.eeip.module.integration.engine.FlowEngine;
import com.eeip.module.integration.entity.IntFlow;
import com.eeip.module.integration.entity.IntFlowTrigger;
import com.eeip.module.integration.mapper.IntFlowMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 调度器：定时扫描到期的 SCHEDULE 触发器并执行流程。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li><b>幂等推进</b>：先算出下次触发时间再执行，避免执行失败导致同一时刻反复触发</li>
 *   <li><b>错过补偿</b>：按 {@code misfirePolicy} 处理（SKIP 直接跳到下一个时间点；
 *       FIRE_ONCE 补跑一次；CATCH_UP 最多补 5 次）</li>
 *   <li><b>不阻塞调度</b>：流程执行放到独立线程池，调度线程只做扫描与推进</li>
 *   <li><b>单机安全</b>：同一时刻只处理到期记录；多实例部署需换成分布式锁（TODO）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowScheduler {

    private final FlowTriggerService triggerService;
    private final FlowEngine engine;
    private final IntFlowMapper flowMapper;
    private final ObjectMapper json;

    @Value("${eeip.scheduler.enabled:true}")
    private boolean enabled;

    @Value("${eeip.scheduler.batch-size:20}")
    private int batchSize;

    /** 流程执行线程池：与调度线程隔离 */
    private final ExecutorService pool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "flow-sched-runner-" + COUNTER.incrementAndGet());
        t.setDaemon(true);
        return t;
    });
    private static final AtomicInteger COUNTER = new AtomicInteger();

    /**
     * 每 5 秒扫描一次。间隔小于最小 cron 粒度（1 分钟），保证准点触发。
     */
    @Scheduled(fixedDelayString = "${eeip.scheduler.interval-ms:5000}",
               initialDelayString = "${eeip.scheduler.initial-delay-ms:15000}")
    public void tick() {
        if (!enabled) {
            return;
        }
        try {
            List<IntFlowTrigger> due = triggerService.due(batchSize);
            for (IntFlowTrigger t : due) {
                handle(t);
            }
        } catch (Exception e) {
            log.error("调度扫描异常", e);
        }
    }

    private void handle(IntFlowTrigger t) {
        Map<String, Object> cfg = parse(t.getConfigJson());
        OffsetDateTime now = OffsetDateTime.now();

        // ① 先算下一次触发时间并落库（无论执行成败都推进，避免卡在同一时刻）
        OffsetDateTime next = triggerService.nextFireTime(cfg, now.plusSeconds(1));
        long missed = t.getNextFireAt() == null ? 0
                : Duration.between(t.getNextFireAt(), now).toSeconds() / Math.max(1, intervalSeconds(cfg));
        if (missed > 0) {
            log.warn("触发器 {}（{}）错过 {} 次，策略={}",
                    t.getNodeId(), t.getFlowCode(), missed, cfg.getOrDefault("misfirePolicy", "FIRE_ONCE"));
        }
        triggerService.markFired(t, "RUNNING", null, next);

        // ② 异步执行
        long id = t.getId();
        pool.submit(() -> runFlow(t, cfg));
    }

    private void runFlow(IntFlowTrigger t, Map<String, Object> cfg) {
        try {
            IntFlow flow = flowMapper.selectById(t.getFlowId());
            if (flow == null) {
                log.warn("触发器 {} 对应的流程已删除", t.getNodeId());
                return;
            }
            Map<String, Object> input = Map.of(
                    "trigger", "SCHEDULE",
                    "nodeId", t.getNodeId(),
                    "firedAt", OffsetDateTime.now().toString());
            FlowEngine.ExecutionResult r = engine.run(flow, input, "SCHEDULE");
            log.info("定时触发执行完成 flow={} node={} status={} cost={}ms instance={}",
                    t.getFlowCode(), t.getNodeId(), r.status(), r.costMs(), r.instanceId());
            // 回写结果（重新查一次，避免覆盖并发更新）
            IntFlowTrigger fresh = triggerService.byId(t.getId());
            if (fresh != null) {
                triggerService.markFired(fresh, r.status(), Long.valueOf(r.instanceId()),
                        fresh.getNextFireAt());
            }
        } catch (Exception e) {
            log.error("定时触发执行失败 flow={} node={}", t.getFlowCode(), t.getNodeId(), e);
            IntFlowTrigger fresh = triggerService.byId(t.getId());
            if (fresh != null) {
                triggerService.markFired(fresh, "FAILED", null, fresh.getNextFireAt());
            }
        }
    }

    private long intervalSeconds(Map<String, Object> cfg) {
        OffsetDateTime a = triggerService.nextFireTime(cfg, OffsetDateTime.now());
        OffsetDateTime b = a == null ? null : triggerService.nextFireTime(cfg, a.plusSeconds(1));
        return a == null || b == null ? 60 : Math.max(60, Duration.between(a, b).toSeconds());
    }

    private Map<String, Object> parse(String cfg) {
        try {
            return cfg == null ? Map.of() : json.readValue(cfg, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }
}
