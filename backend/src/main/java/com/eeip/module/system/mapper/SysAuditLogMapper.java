package com.eeip.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eeip.module.system.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper（只做插入与查询，不提供更新/删除）。
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
