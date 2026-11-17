package com.eeip.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.eeip.common.datascope.DataScope;
import com.eeip.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按数据范围分页查询用户。
     *
     * <p>标注 {@link DataScope} 后，MyBatis-Plus 会在 SQL 解析阶段自动追加
     * tenant_id / dept_name / created_by 等条件，业务代码不感知。</p>
     *
     * @param page    分页对象
     * @param keyword 用户名/姓名/部门 模糊关键字
     * @param status  状态过滤，可为空
     */
    @DataScope(table = "sys_user", alias = "u")
    @Select("""
            <script>
            SELECT u.* FROM sys_user u
            WHERE u.deleted = false
            <if test="status != null"> AND u.status = #{status} </if>
            <if test="keyword != null and keyword != ''">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                OR u.dept_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY u.created_at DESC
            </script>
            """)
    IPage<SysUser> selectUserPage(IPage<SysUser> page,
                                  @Param("keyword") String keyword,
                                  @Param("status") Integer status);
}
