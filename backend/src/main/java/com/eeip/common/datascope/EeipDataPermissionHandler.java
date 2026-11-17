package com.eeip.common.datascope;

import com.eeip.common.security.LoginUser;
import com.eeip.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限处理器。
 *
 * <p>只对标注了 {@link DataScope} 的 Mapper 方法生效；按当前登录人的
 * 数据范围拼接 where 条件，交给 MyBatis-Plus 的 DataPermissionInterceptor 改写 SQL。</p>
 *
 * <p>范围语义：</p>
 * <ul>
 *   <li>ALL：不加任何条件</li>
 *   <li>TENANT：tenant_id = 当前租户</li>
 *   <li>PROJECT：dept_name = 当前用户所在部门（以部门近似项目组）</li>
 *   <li>SELF：created_by = 当前用户名 OR id = 当前用户</li>
 * </ul>
 */
@Slf4j
public class EeipDataPermissionHandler implements MultiDataPermissionHandler {

    /** 方法注解缓存：mappedStatementId → 方法（null 表示无注解） */
    private static final Map<String, Object> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        Method method = resolveMethod(mappedStatementId);
        if (method == null) {
            return null;
        }
        DataScope ann = method.getAnnotation(DataScope.class);
        if (ann == null) {
            return null;
        }
        // 只处理目标表，避免误伤 join 进来的其他表
        if (table != null && table.getName() != null
                && !table.getName().equalsIgnoreCase(ann.table())
                && !table.getName().equalsIgnoreCase(quote(ann.table()))) {
            return null;
        }
        // 非请求线程（定时任务/启动初始化）不做限制
        LoginUser user = SecurityUtils.currentUser();
        if (user == null) {
            return null;
        }

        DataScopeType type = DataScopeType.of(user.dataScope());
        String prefix = ann.alias() == null || ann.alias().isBlank() ? "" : ann.alias() + ".";

        switch (type) {
            case ALL:
                return null;
            case TENANT:
                // 租户隔离已由 TenantLineInnerInterceptor 全局完成，这里不再重复拼接
                return null;
            case PROJECT:
                String dept = user.deptName() == null ? "" : user.deptName();
                // 部门为空时退化为 SELF，避免 "= ''" 误伤全表
                if (dept.isBlank()) {
                    return selfExpression(prefix, ann, user);
                }
                return new EqualsTo(column(prefix, ann.deptColumn()), new StringValue(dept));
            case SELF:
            default:
                return selfExpression(prefix, ann, user);
        }
    }

    /** (created_by = '用户名' OR id = 用户ID) */
    private Expression selfExpression(String prefix, DataScope ann, LoginUser user) {
        Expression byCreator = new EqualsTo(column(prefix, ann.creatorColumn()),
                new StringValue(user.username()));
        Expression byId = new EqualsTo(column(prefix, "id"), new LongValue(user.id()));
        return new Parenthesis(new OrExpression(byCreator, byId));
    }

    private Column column(String prefix, String name) {
        return new Column(prefix + name);
    }

    private String quote(String table) {
        return "\"" + table + "\"";
    }

    /**
     * 由 mappedStatementId（全限定 Mapper 方法名）反射拿到 Method。
     */
    private Method resolveMethod(String mappedStatementId) {
        Object cached = METHOD_CACHE.get(mappedStatementId);
        if (cached != null) {
            return cached == NONE ? null : (Method) cached;
        }
        try {
            int idx = mappedStatementId.lastIndexOf('.');
            String className = mappedStatementId.substring(0, idx);
            String methodName = mappedStatementId.substring(idx + 1);
            Class<?> clazz = Class.forName(className);
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    METHOD_CACHE.put(mappedStatementId, m);
                    return m;
                }
            }
        } catch (Exception e) {
            log.debug("解析 Mapper 方法失败 {}", mappedStatementId, e);
        }
        METHOD_CACHE.put(mappedStatementId, NONE);
        return null;
    }
}
