package com.eeip.module.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eeip.module.integration.entity.IntFlowVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 流程版本 Mapper。 */
@Mapper
public interface IntFlowVersionMapper extends BaseMapper<IntFlowVersion> {

    /** 取某流程已发布的版本（运行时用） */
    @Select("""
            SELECT * FROM t_flow_version
            WHERE flow_id = #{flowId} AND status = 'PUBLISHED' AND deleted = false
            ORDER BY published_at DESC NULLS LAST LIMIT 1
            """)
    IntFlowVersion selectPublished(@Param("flowId") Long flowId);
}
