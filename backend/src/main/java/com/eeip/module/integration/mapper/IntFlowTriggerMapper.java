package com.eeip.module.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eeip.module.integration.entity.IntFlowTrigger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;

/** 流程触发器 Mapper。 */
@Mapper
public interface IntFlowTriggerMapper extends BaseMapper<IntFlowTrigger> {

    /** 扫描到期的 SCHEDULE 触发器（限制批量，避免一次拉太多） */
    @Select("""
            SELECT * FROM t_flow_trigger
            WHERE status = 'ACTIVE' AND deleted = false
              AND trigger_type = 'SCHEDULE'
              AND next_fire_at IS NOT NULL AND next_fire_at <= #{now}
            ORDER BY next_fire_at ASC
            LIMIT #{limit}
            """)
    List<IntFlowTrigger> selectDue(@Param("now") OffsetDateTime now, @Param("limit") int limit);
}
