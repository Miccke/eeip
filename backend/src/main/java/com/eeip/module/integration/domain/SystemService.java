package com.eeip.module.integration.domain;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.module.integration.entity.IntConnection;
import com.eeip.module.integration.mapper.ApiDefinitionMapper;
import com.eeip.module.integration.mapper.IntConnectionMapper;
import com.eeip.module.integration.mapper.ProviderSystemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产方系统服务。
 */
@Service
@RequiredArgsConstructor
public class SystemService {

    private final ProviderSystemMapper mapper;
    private final IntConnectionMapper connectionMapper;
    private final ApiDefinitionMapper apiMapper;

    public List<SystemVO> list(String keyword) {
        return mapper.selectList(Wrappers.<ProviderSystem>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(ProviderSystem::getCode, keyword)
                                .or().like(ProviderSystem::getName, keyword))
                        .orderByAsc(ProviderSystem::getCode))
                .stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemSaveRequest req) {
        String env = req.env() == null ? "PROD" : req.env();
        long dup = mapper.selectCount(Wrappers.<ProviderSystem>lambdaQuery()
                .eq(ProviderSystem::getCode, req.code())
                .eq(ProviderSystem::getEnv, env));
        if (dup > 0) {
            throw new BizException(ErrorCode.CONFLICT, "同环境下的系统编码已存在");
        }
        ProviderSystem s = new ProviderSystem();
        s.setCode(req.code());
        s.setName(req.name());
        s.setEnv(env);
        s.setProtocol(req.protocol() == null ? "HTTP" : req.protocol());
        s.setDomainCode(req.domainCode() == null ? "IT" : req.domainCode());
        s.setBaseUrl(req.baseUrl());
        s.setOwner(req.owner());
        s.setOwnerPhone(req.ownerPhone());
        s.setStatus(req.status() == null ? 1 : req.status());
        s.setRemark(req.remark());
        mapper.insert(s);
        return s.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemSaveRequest req) {
        ProviderSystem s = require(id);
        s.setName(req.name());
        if (req.env() != null) {
            s.setEnv(req.env());
        }
        s.setProtocol(req.protocol() == null ? s.getProtocol() : req.protocol());
        if (req.domainCode() != null) {
            s.setDomainCode(req.domainCode());
        }
        s.setBaseUrl(req.baseUrl());
        s.setOwner(req.owner());
        s.setOwnerPhone(req.ownerPhone());
        if (req.status() != null) {
            s.setStatus(req.status());
        }
        s.setRemark(req.remark());
        mapper.updateById(s);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProviderSystem s = require(id);
        long conns = connectionMapper.selectCount(Wrappers.<IntConnection>lambdaQuery()
                .eq(IntConnection::getSystemCode, s.getCode()));
        long apis = apiMapper.selectCount(Wrappers.<ApiDefinition>lambdaQuery()
                .eq(ApiDefinition::getSystemCode, s.getCode()));
        if (conns > 0 || apis > 0) {
            throw new BizException(ErrorCode.CONFLICT,
                    "该系统下还有 " + conns + " 个连接器、" + apis + " 个接口，请先清理");
        }
        mapper.deleteById(id);
    }

    /** 校验系统存在且启用 */
    public ProviderSystem requireEnabled(String code) {
        ProviderSystem s = mapper.selectOne(Wrappers.<ProviderSystem>lambdaQuery()
                .eq(ProviderSystem::getCode, code).last("LIMIT 1"));
        if (s == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "系统不存在：" + code);
        }
        if (s.getStatus() == null || s.getStatus() != 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "系统已停用：" + code);
        }
        return s;
    }

    private ProviderSystem require(Long id) {
        ProviderSystem s = mapper.selectById(id);
        if (s == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "系统不存在");
        }
        return s;
    }

    private SystemVO toVO(ProviderSystem s) {
        long conns = connectionMapper.selectCount(Wrappers.<IntConnection>lambdaQuery()
                .eq(IntConnection::getSystemCode, s.getCode()));
        long apis = apiMapper.selectCount(Wrappers.<ApiDefinition>lambdaQuery()
                .eq(ApiDefinition::getSystemCode, s.getCode()));
        return new SystemVO(String.valueOf(s.getId()), s.getCode(), s.getName(), s.getDomainCode(), s.getEnv(),
                s.getProtocol(), s.getBaseUrl(), s.getOwner(), s.getOwnerPhone(), s.getStatus(),
                (int) conns, (int) apis, s.getRemark(), s.getUpdatedAt());
    }
}
