package com.eeip.module.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eeip.module.integration.entity.ApiMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** 调用计量 Mapper。 */
@Mapper
public interface ApiMetricMapper extends BaseMapper<ApiMetric> {

    /** 按接口汇总一段时间内的调用量 */
    @Select("""
            SELECT api_id AS api_id, expose_id AS expose_id,
                   SUM(calls) AS calls, SUM(errors) AS errors, SUM(cost_ms_sum) AS cost_sum
            FROM t_api_metric
            WHERE bucket >= #{from}
            GROUP BY api_id, expose_id
            """)
    List<Map<String, Object>> sumByApi(@Param("from") OffsetDateTime from);

    /** 某接口按天汇总（趋势） */
    @Select("""
            SELECT date_trunc('day', bucket) AS day,
                   SUM(calls) AS calls, SUM(errors) AS errors, SUM(cost_ms_sum) AS cost_sum
            FROM t_api_metric
            WHERE expose_id = #{exposeId} AND bucket >= #{from}
            GROUP BY 1 ORDER BY 1
            """)
    List<Map<String, Object>> dailyByExpose(@Param("exposeId") Long exposeId,
                                            @Param("from") OffsetDateTime from);
}
