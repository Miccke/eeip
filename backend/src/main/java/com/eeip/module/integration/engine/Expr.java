package com.eeip.module.integration.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * 极简表达式与取值工具（执行引擎 v0）。
 *
 * <p>只做两件事：</p>
 * <ul>
 *   <li>取值：{@code $.a.b} / {@code $a.b} / {@code $nodes.n2.data} 解析成对象</li>
 *   <li>条件：{@code $resp.code == 200} 这类二元比较（== != &gt; &lt; &gt;= &lt;= contains）</li>
 * </ul>
 * <p>刻意不做完整表达式引擎：安全边界小、可预测、易调试。S12 再考虑 JEXL。</p>
 */
public final class Expr {

    private static final ObjectMapper M = new ObjectMapper();

    private Expr() {
    }

    /**
     * 按路径取值。
     *
     * @param vars 变量表（trigger / 各节点输出 / vars / sys）
     * @param path 形如 {@code $resp.data.orderNo} 或 {@code $.trigger.id}
     */
    @SuppressWarnings("unchecked")
    public static Object resolve(Map<String, Object> vars, String path) {
        if (path == null) {
            return null;
        }
        String p = path.trim();
        if (!p.startsWith("$")) {
            return p;   // 字面量
        }
        p = p.substring(1);
        if (p.startsWith(".")) {
            p = p.substring(1);
        }
        if (p.isEmpty()) {
            return vars;
        }
        String[] segs = p.split("\\.");
        Object cur = vars.get(segs[0]);
        for (int i = 1; i < segs.length && cur != null; i++) {
            String seg = segs[i];
            if (cur instanceof Map<?, ?> m) {
                cur = m.get(seg);
            } else if (cur instanceof List<?> l) {
                try {
                    int idx = Integer.parseInt(seg);
                    cur = idx >= 0 && idx < l.size() ? l.get(idx) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return cur;
    }

    /**
     * 求值条件表达式。
     *
     * @param expr 形如 {@code $resp.code == 200}；空表示恒真
     */
    public static boolean eval(Map<String, Object> vars, String expr) {
        if (expr == null || expr.isBlank()) {
            return true;
        }
        String e = expr.trim();
        String[] ops = {"==", "!=", ">=", "<=", ">", "<", " contains "};
        for (String op : ops) {
            int i = e.indexOf(op);
            if (i > 0) {
                String left = e.substring(0, i).trim();
                String right = e.substring(i + op.length()).trim();
                return compare(resolve(vars, left), op.trim(), right);
            }
        }
        Object v = resolve(vars, e);
        return v instanceof Boolean b ? b : v != null;
    }

    private static boolean compare(Object left, String op, String rawRight) {
        Object right = rawRight.startsWith("$") ? resolve(null, rawRight) : literal(rawRight);
        if (op.equals("contains")) {
            return left != null && String.valueOf(left).contains(String.valueOf(right));
        }
        int cmp = cmp(left, right);
        return switch (op) {
            case "==" -> eq(left, right);
            case "!=" -> !eq(left, right);
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            default -> false;
        };
    }

    private static boolean eq(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number x && b instanceof Number y) {
            return x.doubleValue() == y.doubleValue();
        }
        return String.valueOf(a).equals(String.valueOf(b));
    }

    private static int cmp(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) {
            return Double.compare(x.doubleValue(), y.doubleValue());
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    /** 字面量：数字 / true / false / null / 带引号字符串 / 普通字符串 */
    private static Object literal(String s) {
        String v = s.trim();
        if (v.equals("null")) {
            return null;
        }
        if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(v);
        }
        if ((v.startsWith("'") && v.endsWith("'")) || (v.startsWith("\"") && v.endsWith("\""))) {
            return v.substring(1, v.length() - 1);
        }
        try {
            return v.contains(".") ? Double.parseDouble(v) : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return v;
        }
    }

    /** JSON 字符串 → Map（用于把配置里的 JSON 文本解析成对象） */
    public static Map<String, Object> parseObject(String json) {
        try {
            return json == null || json.isBlank() ? Map.of()
                    : M.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }
}
