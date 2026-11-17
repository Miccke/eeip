package com.eeip.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.api.PageResult;
import com.eeip.common.exception.BizException;
import com.eeip.module.system.dto.UserSaveRequest;
import com.eeip.module.system.entity.SysRole;
import com.eeip.module.system.entity.SysUser;
import com.eeip.module.system.entity.SysUserRole;
import com.eeip.module.system.mapper.SysRoleMapper;
import com.eeip.module.system.mapper.SysUserMapper;
import com.eeip.module.system.mapper.SysUserRoleMapper;
import com.eeip.module.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户管理服务。
 *
 * <p>负责账号的增删改查、角色绑定与密码重置；所有写操作完成后都会清理
 * 该用户的登录态缓存，保证角色变更即时生效。</p>
 */
@Service
@RequiredArgsConstructor
public class UserManageService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthCacheFacade authCacheFacade;

    /**
     * 分页查询用户（结果受当前登录人的数据范围约束，见 DataScope 拦截器）。
     */
    public PageResult<UserVO> page(long pageNum, long pageSize, String keyword, Integer status) {
        // 走带 @DataScope 的查询：SQL 会自动追加数据范围条件
        IPage<SysUser> page = userMapper.selectUserPage(
                new Page<>(pageNum, pageSize), keyword, status);

        // 一次性把本页用户的角色与角色名查出来，避免逐行查询
        List<Long> userIds = page.getRecords().stream().map(SysUser::getId).toList();
        Map<Long, List<Long>> roleIdMap = userIds.isEmpty() ? Map.of()
                : userIds.stream().collect(Collectors.toMap(Function.identity(),
                        userRoleMapper::selectRoleIdsByUserId));
        Map<Long, String> roleNameMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getName));

        return PageResult.of(page, u -> new UserVO(
                u.getId(), u.getUsername(), u.getNickname(), u.getEmail(), u.getPhone(),
                u.getDeptName(), u.getStatus(),
                roleIdMap.getOrDefault(u.getId(), List.of()),
                roleIdMap.getOrDefault(u.getId(), List.of()).stream()
                        .map(roleNameMap::get).filter(Objects::nonNull).toList(),
                u.getLastLoginAt(), u.getCreatedAt(), u.getRemark()));
    }

    /**
     * 新增用户：用户名唯一，初始密码必填。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserSaveRequest req) {
        long exists = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, req.username()));
        if (exists > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        if (!StringUtils.hasText(req.password())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "新建用户必须设置初始密码");
        }
        SysUser user = new SysUser();
        // tenant_id 由多租户拦截器自动写入，业务代码无需关心
        user.setUsername(req.username());
        user.setNickname(req.nickname());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setDeptName(req.deptName());
        user.setStatus(req.status() == null ? 1 : req.status());
        user.setRemark(req.remark());
        userMapper.insert(user);
        bindRoles(user.getId(), req.roleIds());
        return user.getId();
    }

    /**
     * 编辑用户：密码留空表示不修改，角色为全量覆盖。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserSaveRequest req) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setNickname(req.nickname());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setDeptName(req.deptName());
        if (req.status() != null) {
            user.setStatus(req.status());
        }
        user.setRemark(req.remark());
        if (StringUtils.hasText(req.password())) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }
        userMapper.updateById(user);

        // 角色全量覆盖：先删后插
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        bindRoles(id, req.roleIds());
        authCacheFacade.evict(id);
    }

    /**
     * 删除用户（逻辑删除）。内置 admin 不允许删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if ("admin".equals(user.getUsername())) {
            throw new BizException(ErrorCode.LAST_ADMIN);
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        authCacheFacade.evict(id);
    }

    /**
     * 重置密码：重置后立即失效登录态缓存，强制重新登录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            throw new BizException(ErrorCode.BAD_REQUEST, "密码长度需 8-64 位");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        authCacheFacade.evict(id);
    }

    /** 绑定角色（去重后逐个插入） */
    private void bindRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        roleIds.stream().distinct().forEach(roleId -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        });
    }
}
