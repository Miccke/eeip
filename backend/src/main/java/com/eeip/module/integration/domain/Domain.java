package com.eeip.module.integration.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/** 业务领域（集团口径），系统与接口按领域归类。 */
@Data
@TableName("t_domain")
public class Domain {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private String nameEn;

    private Integer sort;

    private Integer status;

    private String remark;

    private OffsetDateTime createdAt;

    @TableLogic
    private Boolean deleted;
}
