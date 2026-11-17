package com.eeip.common.datascope;

import java.lang.annotation.*;

/**
 * 数据范围注解，标注在 Mapper 方法上。
 *
 * <p>由 {@link EeipDataPermissionHandler} 在 SQL 解析阶段自动追加 where 条件，
 * 业务代码无需关心权限过滤。</p>
 *
 * <pre>{@code
 * @DataScope(alias = "u", table = "sys_user")
 * IPage<SysUser> selectUserPage(IPage<SysUser> page, ...);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /** 表名（用于判断 SQL 中哪张表需要追加条件） */
    String table();

    /** 表别名，留空表示 SQL 中不带别名 */
    String alias() default "";

    /** 租户字段 */
    String tenantColumn() default "tenant_id";

    /** 部门字段（PROJECT 范围使用） */
    String deptColumn() default "dept_name";

    /** 创建人字段（SELF 范围使用） */
    String creatorColumn() default "created_by";
}
