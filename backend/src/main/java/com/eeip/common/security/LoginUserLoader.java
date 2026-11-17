package com.eeip.common.security;

/**
 * 由业务模块实现：从数据库中加载用户、角色与权限。
 */
public interface LoginUserLoader {
    LoginUser load(Long userId);
}
