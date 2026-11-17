package com.eeip.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eeip.module.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /** 用户拥有的权限标识（按钮 + 菜单） */
    @Select("""
            SELECT DISTINCT p.code FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
              AND p.deleted = false AND p.status = 1 AND p.code IS NOT NULL
            """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    /** 用户可见的目录与菜单 */
    @Select("""
            SELECT DISTINCT p.* FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
              AND p.deleted = false AND p.status = 1 AND p.type IN ('DIR','MENU')
            ORDER BY p.sort, p.id
            """)
    List<SysPermission> selectMenusByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT * FROM sys_permission
            WHERE deleted = false AND status = 1 AND type IN ('DIR','MENU')
            ORDER BY sort, id
            """)
    List<SysPermission> selectAllMenus();

    @Select("""
            SELECT DISTINCT p.code FROM sys_permission p
            WHERE p.deleted = false AND p.status = 1 AND p.code IS NOT NULL
            """)
    List<String> selectAllPermCodes();

    /** 可授予租户的权限 ID（排除平台级权限） */
    @Select("SELECT id FROM sys_permission WHERE deleted = false AND platform_only = false")
    List<Long> selectGrantablePermissionIds();
}
