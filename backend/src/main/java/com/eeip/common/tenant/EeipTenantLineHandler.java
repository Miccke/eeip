package com.eeip.common.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Set;

/**
 * 多租户处理器：对所有业务表自动追加 {@code tenant_id = 当前租户}。
 *
 * <p>以下表被显式排除：</p>
 * <ul>
 *   <li>{@code sys_tenant}：平台级租户注册表；</li>
 *   <li>{@code sys_permission}：权限点为全局定义，所有租户共用；</li>
 *   <li>{@code sys_user_role} / {@code sys_role_permission}：关联表无租户字段，
 *       通过主表（用户/角色）间接隔离；</li>
 *   <li>{@code sys_login_log} / {@code sys_audit_log}：由业务代码显式写入 tenant_id，
 *       避免异步线程丢失上下文导致归属错误。</li>
 * </ul>
 */
public class EeipTenantLineHandler implements TenantLineHandler {

    private static final Set<String> IGNORE_TABLES = Set.of(
            "sys_tenant",
            "sys_permission",
            "sys_user_role",
            "sys_role_permission",
            "sys_login_log",
            "sys_audit_log"
    );

    @Override
    public Expression getTenantId() {
        return new LongValue(TenantContext.getOrDefault());
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (tableName == null) {
            return true;
        }
        String name = tableName.replace("\"", "").toLowerCase();
        return IGNORE_TABLES.contains(name);
    }
}
