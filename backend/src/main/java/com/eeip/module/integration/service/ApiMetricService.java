package com.eeip.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.module.integration.entity.ApiMetric;
import com.eeip.module.integration.mapper.ApiMetricMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 调用计量服务。
 *
 * <p>写入路径：每次调用 → Redis 哈希计数（不落库，避免高并发写放大）；
 * 定时任务每 30 秒把计数汇总进 {@code t_api_metric}（分钟桶，upsert）。</p>
 *
 * <p>键格式：{@code eeip:metric:{exposeId}:{yyyyMMddHHmm}}，字段 calls / errors / cost。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiMetricService {

    private static final String PREFIX = "eeip:metric:";
    private static final DateTimeFormatter MIN = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final StringRedisTemplate redis;
    private final ApiMetricMapper mapper;
    private final JdbcTemplate jdbc;

    /** 记录一次调用（异步线程可用，无阻塞） */
    public void record(Long exposeId, Long apiId, long costMs, boolean failed) {
        if (exposeId == null) {
            return;
        }
        log.info("计量记录 expose={} api={} cost={}ms failed={}", exposeId, apiId, costMs, failed);
        try {
            String key = PREFIX + exposeId + ":" + OffsetDateTime.now(ZONE).format(MIN);
            redis.opsForHash().increment(key, "calls", 1);
            redis.opsForHash().increment(key, "cost", costMs);
            if (failed) {
                redis.opsForHash().increment(key, "errors", 1);
            }
            redis.expire(key, java.time.Duration.ofHours(2));
        } catch (Exception e) {
            log.warn("计量写入失败 expose={}：{}", exposeId, e.toString());
        }
    }

    /**
     * 定时落库：把 Redis 中的计数合并到分钟桶。
     *
     * <p>保留最近 3 分钟的键（含当前分钟），更早的清掉，避免重复累加。</p>
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    public void flush() {
        Set<String> keys = redis.keys(PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(ZONE).withSecond(0).withNano(0);
        for (String key : keys) {
            try {
                String[] parts = key.substring(PREFIX.length()).split(":");
                if (parts.length != 2) {
                    continue;
                }
                Long exposeId = Long.valueOf(parts[0]);
                OffsetDateTime bucket = OffsetDateTime.parse(
                        parts[1].substring(0, 4) + "-" + parts[1].substring(4, 6) + "-" + parts[1].substring(6, 8)
                                + "T" + parts[1].substring(8, 10) + ":" + parts[1].substring(10, 12)
                                + ":00+08:00");
                // 只落已结束的分钟，当前分钟继续累积
                if (!bucket.isBefore(now)) {
                    continue;
                }
                Map<Object, Object> h = redis.opsForHash().entries(key);
                long calls = parseLong(h.get("calls"));
                if (calls == 0) {
                    redis.delete(key);
                    continue;
                }
                long errors = parseLong(h.get("errors"));
                long cost = parseLong(h.get("cost"));
                upsert(exposeId, bucket, calls, errors, cost);
                redis.delete(key);
            } catch (Exception e) {
                log.warn("计量落库失败 key={}：{}", key, e.getMessage());
            }
        }
    }

    private void upsert(Long exposeId, OffsetDateTime bucket, long calls, long errors, long cost) {
        jdbc.update("""
                INSERT INTO t_api_metric (id, tenant_id, expose_id, api_id, bucket, calls, errors, cost_ms_sum)
                VALUES (?, 1, ?, (SELECT api_id FROM t_api_expose WHERE id = ?), ?, ?, ?, ?)
                ON CONFLICT (expose_id, bucket) DO UPDATE
                SET calls = t_api_metric.calls + EXCLUDED.calls,
                    errors = t_api_metric.errors + EXCLUDED.errors,
                    cost_ms_sum = t_api_metric.cost_ms_sum + EXCLUDED.cost_ms_sum,
                    updated_at = now()
                """, System.nanoTime(), exposeId, exposeId, bucket, calls, errors, cost);
    }

    private long parseLong(Object o) {
        try {
            return o == null ? 0 : Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return 0;
        }
    }

    /** 最近 N 天的汇总：apiId → {calls, errors, errorRate, avgCostMs} */
    public Map<Long, Map<String, Object>> summaryByApi(int days) {
        OffsetDateTime from = OffsetDateTime.now(ZONE).minusDays(days);
        Map<Long, Map<String, Object>> out = new HashMap<>();
        for (Map<String, Object> row : mapper.sumByApi(from)) {
            // PG 驱动返回小写列名，用 helper 统一取值
            Object apiId = pick(row, "api_id");
            if (apiId == null) {
                continue;
            }
            long calls = num(pick(row, "calls"));
            long errors = num(pick(row, "errors"));
            long cost = num(pick(row, "cost_sum"));
            Map<String, Object> m = new HashMap<>();
            m.put("calls", calls);
            m.put("errors", errors);
            m.put("errorRate", calls == 0 ? 0.0 : Math.round(errors * 10000.0 / calls) / 100.0);
            m.put("avgCostMs", calls == 0 ? 0 : cost / calls);
            out.put(Long.valueOf(String.valueOf(apiId)), m);
        }
        return out;
    }

    /** 某发布记录按天趋势 */
    public List<Map<String, Object>> daily(Long exposeId, int days) {
        return mapper.dailyByExpose(exposeId, OffsetDateTime.now(ZONE).minusDays(days));
    }

    /** 忽略大小写取列值（不同驱动/别名大小写不一致） */
    private Object pick(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v != null) {
            return v;
        }
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key) || e.getKey().equalsIgnoreCase(key.replace("_", ""))) {
                return e.getValue();
            }
        }
        return null;
    }

    private long num(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    /** 清空某接口的计量（重置统计用） */
    public void reset(Long exposeId) {
        mapper.delete(Wrappers.<ApiMetric>lambdaQuery().eq(ApiMetric::getExposeId, exposeId));
    }
}
