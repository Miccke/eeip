package com.eeip.common.audit;

import com.eeip.common.security.LoginUser;
import com.eeip.common.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 操作审计切面。
 *
 * <p>只做"记录"，不影响业务事务：审计写入失败只打印告警日志，
 * 绝不能让审计把业务请求拖垮。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    /** 敏感字段脱敏：password / secret / token 等 */
    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)(\"(?:password|oldPassword|newPassword|secret|token|accessKey|sign)\"\\s*:\\s*)\"[^\"]*\"");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    private final AuditLogService auditLogService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                record(pjp, auditLog, result, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                // 审计异常不得影响主流程
                log.warn("写入审计日志失败 action={}", auditLog.action(), e);
            }
        }
    }

    private void record(ProceedingJoinPoint pjp, AuditLog auditLog, Object result,
                        Throwable error, long costMs) {
        LoginUser user = SecurityUtils.currentUser();
        HttpServletRequest request = currentRequest();

        AuditLogCommand cmd = new AuditLogCommand();
        cmd.setOperatorId(user == null ? "anonymous" : String.valueOf(user.id()));
        cmd.setOperatorName(user == null ? "anonymous" : user.nickname());
        cmd.setAction(auditLog.action());
        cmd.setActionName(auditLog.actionName());
        cmd.setResourceType(auditLog.resourceType());
        cmd.setResourceId(resolveResourceId(pjp, auditLog));
        cmd.setResult(error == null ? "SUCCESS" : "FAILED");
        cmd.setErrorMsg(error == null ? null : truncate(error.getMessage(), 1000));
        cmd.setCostMs((int) costMs);
        if (request != null) {
            cmd.setRequestMethod(request.getMethod());
            cmd.setRequestUri(request.getRequestURI());
            cmd.setClientIp(clientIp(request));
            cmd.setUserAgent(truncate(request.getHeader("User-Agent"), 250));
        }
        if (auditLog.recordParams()) {
            cmd.setRequestParams(truncate(mask(toJson(pjp.getArgs())), 4000));
        }
        auditLogService.saveAsync(cmd);
    }

    /** 解析资源 ID：支持 "#id" 形式的 SpEL */
    private String resolveResourceId(ProceedingJoinPoint pjp, AuditLog auditLog) {
        String expr = auditLog.resourceId();
        if (!StringUtils.hasText(expr)) {
            return null;
        }
        if (!expr.startsWith("#")) {
            return expr;
        }
        try {
            Method method = ((MethodSignature) pjp.getSignature()).getMethod();
            var context = new MethodBasedEvaluationContext(
                    pjp.getTarget(), method, pjp.getArgs(), NAME_DISCOVERER);
            Object value = PARSER.parseExpression(expr).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.debug("解析资源 ID 失败 expr={}", expr, e);
            return null;
        }
    }

    private String toJson(Object[] args) {
        try {
            // 过滤掉 Servlet 对象，避免把请求体/响应体整个序列化
            Object[] filtered = Arrays.stream(args)
                    .filter(a -> !(a instanceof HttpServletRequest)
                            && !(a instanceof jakarta.servlet.http.HttpServletResponse))
                    .toArray();
            return MAPPER.writeValueAsString(filtered);
        } catch (Exception e) {
            return null;
        }
    }

    private String mask(String json) {
        return json == null ? null : SENSITIVE.matcher(json).replaceAll("$1\"******\"");
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
