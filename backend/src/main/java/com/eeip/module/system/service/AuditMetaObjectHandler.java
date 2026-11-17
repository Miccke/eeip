package com.eeip.module.system.service;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.eeip.common.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * 审计字段自动填充：createdAt / updatedAt / createdBy / updatedBy。
 *
 * <p>允许业务代码显式赋值，未赋值时由这里补齐。</p>
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        String user = currentUser();
        OffsetDateTime now = OffsetDateTime.now();
        strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", String.class, user);
        strictInsertFill(metaObject, "updatedBy", String.class, user);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());
        strictUpdateFill(metaObject, "updatedBy", String.class, currentUser());
    }

    /** 未登录场景（如定时任务）回落为 system */
    private String currentUser() {
        String u = SecurityUtils.currentUsername();
        return u == null ? "system" : u;
    }
}
