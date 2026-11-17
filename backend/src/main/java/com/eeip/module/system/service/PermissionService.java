package com.eeip.module.system.service;

import com.eeip.common.i18n.LocaleContext;
import com.eeip.module.system.entity.SysPermission;
import com.eeip.module.system.mapper.SysPermissionMapper;
import com.eeip.module.system.vo.MenuVO;
import com.eeip.module.system.vo.PermNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限树装配服务。
 *
 * <p>把扁平的权限表组装成树结构，供两类场景使用：</p>
 * <ul>
 *   <li>登录后返回当前用户可见的菜单树（目录下无可见子菜单时自动隐藏）；</li>
 *   <li>角色授权时返回全量权限树（目录 / 菜单 / 按钮三级）。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysPermissionMapper permissionMapper;

    /**
     * 当前用户可见的菜单树。
     *
     * @param userId     用户 ID
     * @param superAdmin 是否超级管理员（是则返回全部菜单，不查关联表）
     */
    public List<MenuVO> menuTree(Long userId, boolean superAdmin) {
        List<SysPermission> flat = superAdmin
                ? permissionMapper.selectAllMenus()
                : permissionMapper.selectMenusByUserId(userId);
        return buildMenuTree(flat);
    }

    /**
     * 权限树，用于角色分配权限。
     *
     * @param includePlatformOnly 是否包含平台级权限；租户管理员只能看到"可授予"的部分，
     *                            避免把租户管理之类的平台能力授权给租户角色
     */
    public List<PermNodeVO> allPermissionTree(boolean includePlatformOnly) {
        List<SysPermission> flat = permissionMapper.selectList(null).stream()
                .filter(p -> includePlatformOnly || !Boolean.TRUE.equals(p.getPlatformOnly()))
                .toList();
        return buildPermTree(flat);
    }

    /**
     * 当前用户持有的权限编码集合。
     *
     * @param userId     用户 ID
     * @param superAdmin 是否超级管理员（是则返回全量权限编码）
     */
    public List<String> permCodes(Long userId, boolean superAdmin) {
        return superAdmin
                ? permissionMapper.selectAllPermCodes()
                : permissionMapper.selectPermCodesByUserId(userId);
    }

    /** 按 parentId 分组后递归构建菜单树 */
    private List<MenuVO> buildMenuTree(List<SysPermission> flat) {
        Map<Long, List<SysPermission>> byParent = flat.stream()
                .collect(Collectors.groupingBy(SysPermission::getParentId));
        return buildMenus(0L, byParent);
    }

    private List<MenuVO> buildMenus(Long parentId, Map<Long, List<SysPermission>> byParent) {
        List<SysPermission> children = byParent.getOrDefault(parentId, List.of());
        List<MenuVO> result = new ArrayList<>();
        for (SysPermission p : children) {
            List<MenuVO> sub = buildMenus(p.getId(), byParent);
            // 目录下没有任何可见菜单则不展示，避免出现空目录
            if ("DIR".equals(p.getType()) && sub.isEmpty()) {
                continue;
            }
            result.add(new MenuVO(p.getId(), localizedName(p), p.getPath(), p.getComponent(),
                    p.getIcon(), p.getType(), p.getHidden(), sub.isEmpty() ? null : sub));
        }
        return result;
    }

    /** 按请求语言取菜单名：缺翻译时回落简体中文 */
    private String localizedName(SysPermission p) {
        String code = LocaleContext.code();
        if ("en-US".equals(code) && p.getNameEn() != null && !p.getNameEn().isBlank()) {
            return p.getNameEn();
        }
        if ("zh-TW".equals(code) && p.getNameTw() != null && !p.getNameTw().isBlank()) {
            return p.getNameTw();
        }
        return p.getName();
    }

    private List<PermNodeVO> buildPermTree(List<SysPermission> flat) {
        Map<Long, List<SysPermission>> byParent = flat.stream()
                .collect(Collectors.groupingBy(SysPermission::getParentId));
        return buildPermNodes(0L, byParent);
    }

    private List<PermNodeVO> buildPermNodes(Long parentId, Map<Long, List<SysPermission>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(SysPermission::getSort))
                .map(p -> new PermNodeVO(p.getId(), p.getParentId(), p.getType(), p.getCode(),
                        p.getName(), p.getPath(), p.getIcon(), p.getSort(),
                        buildPermNodes(p.getId(), byParent)))
                .toList();
    }
}
