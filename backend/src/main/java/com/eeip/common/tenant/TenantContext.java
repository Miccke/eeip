package com.eeip.common.tenant;

/**
 * 租户上下文（ThreadLocal）。
 *
 * <p>由 JwtAuthFilter 在认证通过后写入，请求结束清理；登录接口在识别出租户后
 * 手动写入，保证后续 SQL 自动带上 tenant_id 条件。</p>
 *
 * <p>未设置时回落到默认租户（1），避免定时任务、启动初始化等无请求上下文的场景报错。</p>
 */
public final class TenantContext {

    /** 默认租户（集团总部） */
    public static final Long DEFAULT_TENANT_ID = 1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CURRENT.set(tenantId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static Long getOrDefault() {
        Long id = CURRENT.get();
        return id == null ? DEFAULT_TENANT_ID : id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
