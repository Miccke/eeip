package com.eeip.module.integration.domain;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.module.integration.mapper.DomainMapper;
import com.eeip.module.integration.mapper.ProviderSystemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** 业务领域服务。 */
@Service
@RequiredArgsConstructor
public class DomainService {

    private final DomainMapper mapper;
    private final ProviderSystemMapper systemMapper;

    public List<Domain> list() {
        return mapper.selectList(Wrappers.<Domain>lambdaQuery()
                .orderByAsc(Domain::getSort).orderByAsc(Domain::getCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Domain d) {
        if (!StringUtils.hasText(d.getCode()) || !StringUtils.hasText(d.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "领域编码与名称不能为空");
        }
        long dup = mapper.selectCount(Wrappers.<Domain>lambdaQuery().eq(Domain::getCode, d.getCode()));
        if (dup > 0) {
            throw new BizException(ErrorCode.CONFLICT, "领域编码已存在");
        }
        d.setId(null);
        d.setStatus(d.getStatus() == null ? 1 : d.getStatus());
        d.setSort(d.getSort() == null ? 999 : d.getSort());
        mapper.insert(d);
        return d.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, Domain d) {
        Domain e = require(id);
        e.setName(d.getName());
        e.setNameEn(d.getNameEn());
        e.setSort(d.getSort());
        e.setStatus(d.getStatus());
        e.setRemark(d.getRemark());
        mapper.updateById(e);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Domain d = require(id);
        long used = systemMapper.selectCount(Wrappers.<ProviderSystem>lambdaQuery()
                .eq(ProviderSystem::getDomainCode, d.getCode()));
        if (used > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该领域下还有 " + used + " 个系统，无法删除");
        }
        mapper.deleteById(id);
    }

    private Domain require(Long id) {
        Domain d = mapper.selectById(id);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "领域不存在");
        }
        return d;
    }
}
