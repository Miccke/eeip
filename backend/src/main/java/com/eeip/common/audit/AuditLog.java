package com.eeip.common.audit;

import java.lang.annotation.*;

/**
 * 操作审计注解。
 *
 * <p>标注在 Controller 的写操作方法上，由 {@link AuditLogAspect} 拦截并异步落库。
 * 审计内容包含：操作人、动作、资源、请求参数（已脱敏）、结果、耗时、IP、UA。</p>
 *
 * <pre>{@code
 * @AuditLog(action = "USER_CREATE", actionName = "新增用户",
 *           resourceType = "USER", resourceId = "#request.username()")
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 动作编码，建议大写蛇形，如 USER_CREATE */
    String action();

    /** 动作中文名，用于审计页面直接展示 */
    String actionName() default "";

    /** 资源类型，如 USER / ROLE / FLOW / API */
    String resourceType() default "";

    /**
     * 资源 ID 表达式。
     * <ul>
     *   <li>以 "#" 开头：按 SpEL 解析，如 "#id"、"#request.username()"</li>
     *   <li>其他：作为字面量使用</li>
     * </ul>
     */
    String resourceId() default "";

    /** 是否记录请求参数（默认记录；含敏感字段会自动脱敏） */
    boolean recordParams() default true;
}
