package com.eeip.common.datascope;

/**
 * 数据范围类型。
 *
 * <p>数值越大范围越宽，多角色时取最宽的一个（"最小限制"原则的反面：
 * 只要有一个角色能看到，就看得到）。</p>
 */
public enum DataScopeType {

    /** 仅本人创建或本人 */
    SELF(0),
    /** 同部门（当前阶段用部门近似"项目组"；接入项目空间后改为 project_id） */
    PROJECT(1),
    /** 本租户 */
    TENANT(2),
    /** 全部数据 */
    ALL(3);

    private final int weight;

    DataScopeType(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    /** 解析字符串，非法值回落到 SELF（最严格） */
    public static DataScopeType of(String code) {
        if (code == null) {
            return SELF;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SELF;
        }
    }

    /** 取最宽的范围 */
    public static DataScopeType widest(Iterable<String> codes) {
        DataScopeType result = SELF;
        for (String c : codes) {
            DataScopeType t = of(c);
            if (t.weight() > result.weight()) {
                result = t;
            }
        }
        return result;
    }
}
