package com.eeip.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eeip.module.system.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT count(1) FROM sys_user_role WHERE role_id = #{roleId}")
    long countUsersByRoleId(@Param("roleId") Long roleId);
}
