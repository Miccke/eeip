package com.eeip.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long parentId;
    /** DIR / MENU / BUTTON */
    private String type;
    private String code;
    private String name;

    /** 繁体中文名称 */
    private String nameTw;

    /** 英文名称 */
    private String nameEn;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Boolean hidden;
    private Integer status;

    /** 是否平台级权限（仅平台租户可用，不可授予其他租户） */
    private Boolean platformOnly;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic
    private Boolean deleted;
}
