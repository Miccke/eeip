package com.eeip.common.security;

import java.io.Serializable;
import java.util.List;

/**
 * 登录态用户，缓存于 Redis（30 分钟），权限变更时主动失效。
 *
 * <p>除身份与权限外，还缓存了数据范围（dataScope）与部门（deptName），
 * 供数据权限拦截器在 SQL 改写时直接使用，避免每次请求再查库。</p>
 */
public record LoginUser(
        /** 用户 ID */
        Long id,
        /** 租户 ID */
        Long tenantId,
        /** 用户名（登录名） */
        String username,
        /** 姓名 */
        String nickname,
        /** 部门名称，PROJECT 数据范围按此过滤 */
        String deptName,
        /** 角色编码列表 */
        List<String> roles,
        /** 权限点列表 */
        List<String> perms,
        /** 生效的数据范围：ALL / TENANT / PROJECT / SELF */
        String dataScope
) implements Serializable {

    public boolean hasPerm(String perm) {
        return perms != null && (perms.contains(perm) || perms.contains("*"));
    }
}
