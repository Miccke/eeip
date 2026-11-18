package com.eeip.module.integration.engine;

import com.eeip.module.integration.dto.FlowDsl;

import java.util.Map;

/**
 * 节点执行器 SPI：一种节点类型一个实现。
 *
 * <p>实现类交给 Spring 管理，引擎按 {@link #type()} 建映射；
 * 未实现的类型在运行时直接报 {@code NODE_NOT_IMPLEMENTED}。</p>
 */
public interface NodeExecutor {

    /** 对应 manifest 的 type */
    String type();

    /**
     * 执行节点。
     *
     * @param node   节点定义（含 config）
     * @param vars   变量表（只读使用；写变量请通过返回值的 outputVar 约定）
     * @param input  上游输出
     * @return 本节点输出
     * @throws NodeException 业务失败（引擎据此套用 errorPolicy）
     */
    Object execute(FlowDsl.Node node, Map<String, Object> vars, Object input) throws NodeException;

    /** 节点执行失败 */
    class NodeException extends Exception {
        private final String code;

        public NodeException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
