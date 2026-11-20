package com.eeip.module.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eeip.module.integration.entity.IntFlowDeadLetter;
import org.apache.ibatis.annotations.Mapper;

/** 死信 Mapper。 */
@Mapper
public interface IntFlowDeadLetterMapper extends BaseMapper<IntFlowDeadLetter> {
}
