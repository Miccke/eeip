package com.eeip.module.integration.expose;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 对外暴露的接口。
 *
 * <p>把内部系统的接口登记进来，由平台统一鉴权、限流与转发，
 * 形成"各系统接口的管理平台"。</p>
 */
@Data
@TableName("t_api_expose")
public class ApiExpose {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 接口编码 */
    private String code;

    /** 接口名称 */
    private String name;

    /** 对外方法 */
    private String method;

    /** 对外路径（挂 /api/open 下） */
    private String path;

    /** PROXY：直连转发；FLOW：交给流程引擎执行 */
    private String targetType;

    /** 上游连接编码 */
    private String connectionCode;

    /** 上游路径模板，支持 {id} 与 query 透传 */
    private String upstreamPath;

    /** 上游方法（空则沿用对外方法） */
    private String upstreamMethod;

    /** 追加请求头 JSON */
    private String headersJson;

    /** NONE / API_KEY */
    private String authType;

    /** API Key（演示阶段明文） */
    private String apiKey;

    /** QPS 限制，0 表示不限 */
    private Integer qps;

    private Integer status;

    private String version;

    private String remark;

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;

    private Integer versionLock;
}
