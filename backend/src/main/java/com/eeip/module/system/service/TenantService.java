package com.eeip.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.common.tenant.TenantContext;
import com.eeip.module.system.dto.TenantProvisionRequest;
import com.eeip.module.system.dto.TenantSaveRequest;
import com.eeip.module.system.entity.*;
import com.eeip.module.system.mapper.*;
import com.eeip.module.system.vo.TenantProvisionResult;
import com.eeip.module.system.vo.TenantVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.List;

/**
 * 租户服务：租户识别（登录用）与租户管理。
 *
 * <p>sys_tenant 属于平台级表，不参与多租户过滤。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    /** 初始密码字符集：去掉了易混淆的 0/O/1/l/I */
    private static final char[] PWD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$%".toCharArray();

    private final SysTenantMapper tenantMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 按编码解析租户 ID，供登录流程使用。
     *
     * @param code 租户编码，为空时返回默认租户（集团总部）
     * @return 租户 ID
     * @throws BizException 租户不存在、已停用或已过期
     */
    public Long resolveTenantId(String code) {
        if (!StringUtils.hasText(code)) {
            return TenantContext.DEFAULT_TENANT_ID;
        }
        SysTenant tenant = tenantMapper.selectOne(Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getCode, code.trim().toUpperCase()));
        if (tenant == null) {
            throw new BizException(ErrorCode.TENANT_NOT_FOUND);
        }
        if (tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw new BizException(ErrorCode.TENANT_DISABLED);
        }
        if (tenant.getExpireAt() != null && tenant.getExpireAt().isBefore(java.time.OffsetDateTime.now())) {
            throw new BizException(ErrorCode.TENANT_EXPIRED);
        }
        return tenant.getId();
    }

    /**
     * 租户列表。
     */
    public List<TenantVO> list(String keyword) {
        return tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(SysTenant::getCode, keyword)
                                .or().like(SysTenant::getName, keyword))
                        .orderByAsc(SysTenant::getId))
                .stream()
                .map(t -> new TenantVO(t.getId(), t.getCode(), t.getName(), t.getTenantType(),
                        t.getStatus(), t.getContactName(), t.getContactPhone(), t.getExpireAt(),
                        t.getRemark(), t.getCreatedAt()))
                .toList();
    }

    /**
     * 新增租户。仅创建租户记录；租户内的角色与管理员账号需后续单独初始化。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(TenantSaveRequest req) {
        long exists = tenantMapper.selectCount(Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getCode, req.code()));
        if (exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "租户编码已存在");
        }
        SysTenant tenant = new SysTenant();
        tenant.setCode(req.code());
        tenant.setName(req.name());
        tenant.setTenantType(req.tenantType() == null ? "PARTNER" : req.tenantType());
        tenant.setStatus(req.status() == null ? 1 : req.status());
        tenant.setContactName(req.contactName());
        tenant.setContactPhone(req.contactPhone());
        tenant.setExpireAt(req.expireAt());
        tenant.setRemark(req.remark());
        tenantMapper.insert(tenant);
        log.info("创建租户 code={} id={}", tenant.getCode(), tenant.getId());
        return tenant.getId();
    }

    /**
     * 编辑租户（编码不可修改）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TenantSaveRequest req) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        tenant.setName(req.name());
        tenant.setTenantType(req.tenantType() == null ? tenant.getTenantType() : req.tenantType());
        tenant.setStatus(req.status() == null ? tenant.getStatus() : req.status());
        tenant.setContactName(req.contactName());
        tenant.setContactPhone(req.contactPhone());
        tenant.setExpireAt(req.expireAt());
        tenant.setRemark(req.remark());
        tenantMapper.updateById(tenant);
    }

    /**
     * 删除租户（逻辑删除）。默认租户与仍有用户的租户不允许删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (TenantContext.DEFAULT_TENANT_ID.equals(id)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "默认租户不允许删除");
        }
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        tenantMapper.deleteById(id);
    }

    /**
     * 一键开通租户：建租户 + 内置角色 + 管理员账号 + 授权。
     *
     * <p>关键点：新增的角色与用户必须归属"新租户"，而当前上下文通常是集团租户。
     * 做法是先把 {@link TenantContext} 切到新租户 ID，插入完成后再恢复，
     * 这样多租户拦截器会把 tenant_id 写成新租户，而无需绕过拦截器。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public TenantProvisionResult provision(TenantProvisionRequest req) {
        String code = req.code().trim().toUpperCase();
        long exists = tenantMapper.selectCount(Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getCode, code));
        if (exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "租户编码已存在");
        }

        // ① 建租户（平台级表，不受租户过滤影响）
        SysTenant tenant = new SysTenant();
        tenant.setCode(code);
        tenant.setName(req.name());
        tenant.setTenantType(req.tenantType() == null ? "PARTNER" : req.tenantType());
        tenant.setStatus(1);
        tenant.setContactName(req.contactName());
        tenant.setContactPhone(req.contactPhone());
        tenant.setRemark(req.remark());
        tenantMapper.insert(tenant);
        Long tenantId = tenant.getId();

        Long previous = TenantContext.get();
        try {
            // ② 切换上下文：后续插入自动带上新租户 ID
            TenantContext.set(tenantId);

            // ③ 两个内置角色
            SysRole adminRole = createRole("SUPER_ADMIN", "租户管理员", "ALL", 1);
            SysRole devRole = createRole("DEVELOPER", "租户开发", "PROJECT", 3);

            // ④ 管理员账号
            String username = StringUtils.hasText(req.adminUsername()) ? req.adminUsername() : "admin";
            String rawPassword = StringUtils.hasText(req.adminPassword())
                    ? req.adminPassword() : randomPassword();
            SysUser admin = new SysUser();
            admin.setUsername(username);
            admin.setNickname(StringUtils.hasText(req.adminNickname()) ? req.adminNickname() : req.name() + "管理员");
            admin.setPassword(passwordEncoder.encode(rawPassword));
            admin.setStatus(1);
            admin.setRemark("开通租户时自动创建");
            userMapper.insert(admin);

            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(adminRole.getId());
            userRoleMapper.insert(ur);

            // ⑤ 租户管理员获得"可授予"的全部权限点（自动排除平台级权限）
            List<Long> permIds = permissionMapper.selectGrantablePermissionIds();
            for (Long pid : permIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(adminRole.getId());
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
            // 租户开发：概览 + 集成资产
            for (Long pid : List.of(3001L, 3100L, 3101L, 3102L, 3103L, 3134L)) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(devRole.getId());
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }

            log.info("开通租户 code={} tenantId={} admin={}", code, tenantId, username);
            return new TenantProvisionResult(tenantId, code, username, rawPassword, 2, permIds.size());
        } finally {
            // ⑥ 恢复原上下文，防止影响本次请求后续操作
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }

    private SysRole createRole(String code, String name, String dataScope, int sort) {
        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(name);
        role.setDataScope(dataScope);
        role.setSort(sort);
        role.setStatus(1);
        role.setBuiltin(true);
        role.setRemark("开通租户时自动创建");
        roleMapper.insert(role);
        return role;
    }

    /** 生成 12 位随机初始密码 */
    private String randomPassword() {
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PWD_CHARS[rnd.nextInt(PWD_CHARS.length)]);
        }
        return sb.toString();
    }

    /**
     * 按 ID 取租户（内部使用）。
     */
    public SysTenant getById(Long id) {
        return tenantMapper.selectById(id);
    }
}
