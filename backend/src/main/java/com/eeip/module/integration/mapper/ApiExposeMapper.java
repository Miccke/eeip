package com.eeip.module.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eeip.module.integration.expose.ApiExpose;
import org.apache.ibatis.annotations.Mapper;

/** 对外接口 Mapper。 */
@Mapper
public interface ApiExposeMapper extends BaseMapper<ApiExpose> {
}
