package com.eeip.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.common.datascope.DataScopeType;
import com.eeip.common.security.*;
import com.eeip.common.tenant.TenantContext;
import com.eeip.module.system.dto.LoginRequest;
import com.eeip.module.system.entity.SysLoginLog;
import com.eeip.module.system.entity.SysUser;
import com.eeip.module.system.mapper.SysLoginLogMapper;
import com.eeip.module.system.mapper.SysUserMapper;
import com.eeip.module.system.mapper.SysUserRoleMapper;
import com.eeip.module.system.vo.LoginResponse;
import com.eeip.module.system.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 认证服务：登录、当前用户信息、登出。
 *
 * <p>登录成功后把「用户 + 角色 + 权限」写入 Redis 缓存
 * （{@code eeip:auth:user:{id}}，30 分钟），后续请求由 JwtAuthFilter 直接读缓存，
 * 避免每次请求查库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthService implements LoginUserLoader {

    /** 超级管理员角色编码：拥有全量权限，不走关联表 */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final PermissionService permissionService;
    private final UserAuthCache cache;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TenantService tenantService;

    /**
     * 从数据库加载登录态用户（JwtAuthFilter 在缓存未命中时调用）。
     *
     * @return 用户不存在或已停用时返回 null
     */
    @Override
    public LoginUser load(Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u == null || u.getStatus() == null || u.getStatus() != 1) {
            return null;
        }
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(userId);
        // 注意：超级管理员特权只对"平台租户"（集团总部）开放，
        // 合作方租户的同名角色不能自动获得全量权限，否则会越权看到平台级功能
        boolean platformAdmin = isPlatformAdmin(u.getTenantId(), roleCodes);
        List<String> perms = permissionService.permCodes(userId, platformAdmin);
        // 数据范围：取该用户所有角色中最宽的一个；平台超管直接 ALL
        String dataScope = platformAdmin
                ? DataScopeType.ALL.name()
                : DataScopeType.widest(userRoleMapper.selectDataScopesByUserId(userId)).name();
        return new LoginUser(u.getId(), u.getTenantId(), u.getUsername(), u.getNickname(),
                u.getDeptName(), roleCodes, perms, dataScope);
    }

    /**
     * 登录：校验密码 → 写登录日志 → 写入登录态缓存 → 签发 JWT。
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest req, String ip, String userAgent) {
        // 先识别租户：后续 SQL 由多租户拦截器自动限定在该租户内
        Long tenantId = tenantService.resolveTenantId(req.tenantCode());
        TenantContext.set(tenantId);

        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, req.username()));

        // 用户不存在与密码错误返回同一错误码，避免账号枚举
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            writeLog(user == null ? null : user.getTenantId(), req.username(), ip, userAgent,
                    false, "用户名或密码错误");
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            writeLog(user.getTenantId(), req.username(), ip, userAgent, false, "账号已停用");
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        LoginUser loginUser = load(user.getId());
        if (loginUser == null) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        cache.put(loginUser);

        // 更新最近登录信息（用 UpdateWrapper 避免乐观锁版本冲突）
        userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getLastLoginAt, OffsetDateTime.now())
                .set(SysUser::getLastLoginIp, ip)
                .eq(SysUser::getId, user.getId()));

        writeLog(user.getTenantId(), req.username(), ip, userAgent, true, "登录成功");
        log.info("用户登录成功 userId={} username={} ip={}", user.getId(), user.getUsername(), ip);

        String token = tokenProvider.createToken(loginUser);
        return new LoginResponse(token, "Bearer", tokenProvider.expireSeconds());
    }

    /**
     * 当前登录用户信息（含权限点与菜单树）。
     */
    public UserInfoVO me() {
        LoginUser lu = SecurityUtils.currentUser();
        if (lu == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        boolean platformAdmin = isPlatformAdmin(lu.tenantId(), lu.roles());
        return new UserInfoVO(lu.id(), lu.username(), lu.nickname(), null, null,
                lu.roles(), lu.perms(), permissionService.menuTree(lu.id(), platformAdmin));
    }

    /**
     * 登出：清除服务端登录态缓存（JWT 本身无状态，靠缓存失效实现主动下线）。
     */
    public void logout() {
        LoginUser lu = SecurityUtils.currentUser();
        if (lu != null) {
            cache.evict(lu.id());
        }
    }

    /**
     * 是否"平台级超管"：既是 SUPER_ADMIN，又属于平台租户（集团总部）。
     * 租户内的 SUPER_ADMIN 仅拥有本租户被显式授予的权限。
     */
    private boolean isPlatformAdmin(Long tenantId, List<String> roleCodes) {
        return roleCodes.contains(SUPER_ADMIN)
                && TenantContext.DEFAULT_TENANT_ID.equals(tenantId);
    }

    private void writeLog(Long tenantId, String username, String ip, String ua,
                          boolean success, String message) {
        SysLoginLog log = new SysLoginLog();
        log.setTenantId(tenantId == null ? 1L : tenantId);
        log.setUsername(username);
        log.setIp(ip);
        log.setUserAgent(ua != null && ua.length() > 250 ? ua.substring(0, 250) : ua);
        log.setSuccess(success);
        log.setMessage(message);
        log.setCreatedAt(OffsetDateTime.now());
        loginLogMapper.insert(log);
    }
}
