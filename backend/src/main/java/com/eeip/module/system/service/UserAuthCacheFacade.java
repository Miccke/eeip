package com.eeip.module.system.service;

import com.eeip.common.security.UserAuthCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 登录态缓存门面。
 *
 * <p>业务服务通过它失效缓存，避免在业务代码里到处直接依赖 security 包。</p>
 */
@Component
@RequiredArgsConstructor
public class UserAuthCacheFacade {

    private final UserAuthCache cache;

    /** 清除指定用户的登录态缓存（角色/权限变更后调用） */
    public void evict(Long userId) {
        cache.evict(userId);
    }
}
