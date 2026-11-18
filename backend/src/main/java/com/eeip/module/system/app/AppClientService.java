package com.eeip.module.system.app;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.module.system.entity.ApiGrant;
import com.eeip.module.system.entity.AppClient;
import com.eeip.module.system.mapper.ApiGrantMapper;
import com.eeip.module.system.mapper.AppClientMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * 应用授权服务：应用注册、密钥管理、接口授权。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppClientService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RND = new SecureRandom();

    private final AppClientMapper clientMapper;
    private final ApiGrantMapper grantMapper;
    private final ObjectMapper objectMapper;

    /** 列表（含已授权接口数） */
    public List<AppClientVO> list(String keyword) {
        return clientMapper.selectList(Wrappers.<AppClient>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(AppClient::getAppKey, keyword)
                                .or().like(AppClient::getAppName, keyword))
                        .orderByDesc(AppClient::getCreatedAt))
                .stream().map(c -> {
                    long granted = grantMapper.selectCount(Wrappers.<ApiGrant>lambdaQuery()
                            .eq(ApiGrant::getClientId, c.getId())
                            .eq(ApiGrant::getStatus, 1));
                    return new AppClientVO(String.valueOf(c.getId()), c.getAppKey(), c.getAppName(),
                            c.getOwner(), c.getStatus(), c.getExpireAt(), c.getAllowedIps(),
                            (int) granted, c.getRemark(), c.getCreatedAt());
                }).toList();
    }

    /** 创建应用，返回明文密钥（仅本次可见） */
    @Transactional(rollbackFor = Exception.class)
    public AppClientCreated create(AppClientSaveRequest req) {
        AppClient c = new AppClient();
        c.setAppKey(StringUtils.hasText(req.appKey()) ? req.appKey() : random(16, true));
        long dup = clientMapper.selectCount(Wrappers.<AppClient>lambdaQuery()
                .eq(AppClient::getAppKey, c.getAppKey()));
        if (dup > 0) {
            throw new BizException(ErrorCode.CONFLICT, "appKey 已存在");
        }
        c.setAppSecret(random(32, false));
        c.setAppName(req.appName());
        c.setOwner(req.owner());
        c.setStatus(req.status() == null ? 1 : req.status());
        c.setExpireAt(req.expireAt());
        c.setAllowedIps(req.allowedIps());
        c.setRemark(req.remark());
        clientMapper.insert(c);
        log.info("创建应用 appKey={}", c.getAppKey());
        return new AppClientCreated(String.valueOf(c.getId()), c.getAppKey(), c.getAppSecret());
    }

    /** 编辑（不改密钥） */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AppClientSaveRequest req) {
        AppClient c = require(id);
        c.setAppName(req.appName());
        c.setOwner(req.owner());
        if (req.status() != null) {
            c.setStatus(req.status());
        }
        c.setExpireAt(req.expireAt());
        c.setAllowedIps(req.allowedIps());
        c.setRemark(req.remark());
        clientMapper.updateById(c);
    }

    /** 重置密钥，返回新密钥（仅本次可见） */
    @Transactional(rollbackFor = Exception.class)
    public String resetSecret(Long id) {
        AppClient c = require(id);
        c.setAppSecret(random(32, false));
        clientMapper.updateById(c);
        return c.getAppSecret();
    }

    /** 删除应用及其授权 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        clientMapper.deleteById(id);
        grantMapper.delete(Wrappers.<ApiGrant>lambdaQuery().eq(ApiGrant::getClientId, id));
    }

    /** 查询已授权接口 ID */
    public List<String> grants(Long clientId) {
        require(clientId);
        return grantMapper.selectList(Wrappers.<ApiGrant>lambdaQuery()
                        .eq(ApiGrant::getClientId, clientId)
                        .eq(ApiGrant::getStatus, 1))
                .stream().map(g -> String.valueOf(g.getApiId())).toList();
    }

    /** 覆盖式授权 */
    @Transactional(rollbackFor = Exception.class)
    public void saveGrants(Long clientId, List<String> apiIds) {
        require(clientId);
        grantMapper.delete(Wrappers.<ApiGrant>lambdaQuery().eq(ApiGrant::getClientId, clientId));
        for (String apiId : apiIds) {
            ApiGrant g = new ApiGrant();
            g.setClientId(clientId);
            g.setApiId(Long.valueOf(apiId));
            g.setStatus(1);
            g.setQps(0);
            grantMapper.insert(g);
        }
    }

    /** 校验应用是否可调用某接口 */
    public boolean hasGrant(Long clientId, Long apiId) {
        return grantMapper.selectCount(Wrappers.<ApiGrant>lambdaQuery()
                .eq(ApiGrant::getClientId, clientId)
                .eq(ApiGrant::getApiId, apiId)
                .eq(ApiGrant::getStatus, 1)) > 0;
    }

    /** 按 appKey 取启用的应用 */
    public AppClient byKey(String appKey) {
        AppClient c = clientMapper.selectOne(Wrappers.<AppClient>lambdaQuery()
                .eq(AppClient::getAppKey, appKey).last("LIMIT 1"));
        if (c == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "应用不存在");
        }
        if (c.getStatus() == null || c.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "应用已停用");
        }
        if (c.getExpireAt() != null && c.getExpireAt().isBefore(OffsetDateTime.now())) {
            throw new BizException(ErrorCode.FORBIDDEN, "应用已过期");
        }
        return c;
    }

    private AppClient require(Long id) {
        AppClient c = clientMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "应用不存在");
        }
        return c;
    }

    private String random(int len, boolean keyLike) {
        String chars = keyLike ? "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" : CHARS;
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(RND.nextInt(chars.length())));
        }
        return keyLike ? "ak_" + sb.substring(0, len - 3) : sb.toString();
    }

    /** 创建结果（含一次性密钥） */
    public record AppClientCreated(String id, String appKey, String appSecret) {
    }
}
