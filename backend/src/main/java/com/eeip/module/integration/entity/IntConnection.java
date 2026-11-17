package com.eeip.module.integration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 集成连接配置。
 *
 * <p>注意：{@code authConfig} 含凭证，任何对外 VO 都不得返回该字段。</p>
 */
@Data
@TableName("t_connection")
public class IntConnection {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID（由多租户拦截器自动维护） */
    private Long tenantId;

    /** 连接编码，租户内唯一 */
    private String code;

    /** 连接名称 */
    private String name;

    /** 连接类型，当前仅 HTTP */
    private String type;

    /** 目标地址（测试与调用均以此为准） */
    private String endpoint;

    /** HTTP 方法：GET / POST / PUT / DELETE */
    private String method;

    /** 认证方式：NONE / BEARER / BASIC / API_KEY */
    private String authType;

    /** 认证配置 JSON（凭证，不外露） */
    private String authConfig;

    /** 自定义请求头 JSON，如 {"Accept":"application/json"} */
    private String headersJson;

    /** 超时毫秒 */
    private Integer timeoutMs;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 最近一次连通性测试时间 */
    private OffsetDateTime lastCheckAt;

    /** 最近一次测试结果：SUCCESS / FAILED */
    private String lastCheckStatus;

    /** 最近一次测试耗时（毫秒） */
    private Integer lastCheckMs;

    /** 最近一次测试失败原因 */
    private String lastCheckError;

    /** 备注 */
    private String remark;

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;

    @Version
    private Integer version;
}
