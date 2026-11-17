package com.eeip.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.module.system.dto.RoleSaveRequest;
import com.eeip.module.system.entity.SysRole;
import com.eeip.module.system.entity.SysRolePermission;
import com.eeip.module.system.mapper.SysRoleMapper;
import com.eeip.module.system.mapper.SysRolePermissionMapper;
import com.eeip.module.system.mapper.SysUserRoleMapper;
import com.eeip.module.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理服务。
 *
 * <p>角色承载三类信息：菜单可见性、按钮权限、数据范围（dataScope）。
 * 角色权限或数据范围变更后，会清理该角色下所有用户的登录态缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class RoleManageService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final UserAuthCacheFacade authCacheFacade;

    /**
     * 角色列表（带成员数量）。
     */
    public List<RoleVO> list(String keyword) {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), SysRole::getName, keyword)
                        .orderByAsc(SysRole::getSort))
                .stream()
                .map(r -> new RoleVO(r.getId(), r.getCode(), r.getName(), r.getDataScope(),
                        r.getSort(), r.getStatus(), r.getBuiltin(), r.getRemark(),
                        rolePermissionMapper.countUsersByRoleId(r.getId())))
                .toList();
    }

    /**
     * 新增角色。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleSaveRequest req) {
        long exists = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, req.code()));
        if (exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "角色编码已存在");
        }
        SysRole role = new SysRole();
        // tenant_id 由多租户拦截器自动写入
        role.setCode(req.code());
        role.setName(req.name());
        role.setDataScope(req.dataScope() == null ? "SELF" : req.dataScope());
        role.setSort(req.sort() == null ? 99 : req.sort());
        role.setStatus(req.status() == null ? 1 : req.status());
        role.setBuiltin(false);
        role.setRemark(req.remark());
        roleMapper.insert(role);
        return role.getId();
    }

    /**
     * 编辑角色：内置角色不允许修改，其余字段可改（含数据范围）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleSaveRequest req) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw new BizException(ErrorCode.BUILTIN_ROLE);
        }
        role.setName(req.name());
        role.setDataScope(req.dataScope() == null ? role.getDataScope() : req.dataScope());
        role.setSort(req.sort() == null ? role.getSort() : req.sort());
        role.setStatus(req.status() == null ? role.getStatus() : req.status());
        role.setRemark(req.remark());
        roleMapper.updateById(role);
        evictRoleUsers(id);
    }

    /**
     * 删除角色：内置角色与仍被用户持有的角色不允许删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw new BizException(ErrorCode.BUILTIN_ROLE);
        }
        if (rolePermissionMapper.countUsersByRoleId(id) > 0) {
            throw new BizException(ErrorCode.ROLE_IN_USE);
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, id));
    }

    /**
     * 查询角色已分配的权限 ID（前端权限树回显用）。
     */
    public List<Long> permissionIds(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    /**
     * 全量覆盖角色权限；保存后清理该角色下所有用户的缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId));
        if (permissionIds != null) {
            permissionIds.stream().distinct().forEach(pid -> {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            });
        }
        evictRoleUsers(roleId);
    }

    /** 清理持有该角色所有用户的登录态缓存（权限即时生效） */
    private void evictRoleUsers(Long roleId) {
        userRoleMapper.selectUserIdsByRoleId(roleId).forEach(authCacheFacade::evict);
    }
}
