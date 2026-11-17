package com.eeip.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String code;
    private String name;
    private String dataScope;
    private Integer sort;
    private Integer status;
    private Boolean builtin;
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
