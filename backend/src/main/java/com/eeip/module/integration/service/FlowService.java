package com.eeip.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.dto.FlowSaveRequest;
import com.eeip.module.integration.entity.IntFlow;
import com.eeip.module.integration.entity.IntFlowVersion;
import com.eeip.module.integration.mapper.IntFlowMapper;
import com.eeip.module.integration.mapper.IntFlowVersionMapper;
import com.eeip.module.integration.vo.FlowDetailVO;
import com.eeip.module.integration.vo.FlowVO;
import com.eeip.module.integration.vo.FlowValidateResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 流程定义服务：草稿保存、校验、发布、下线、版本管理。
 *
 * <p>发布语义：校验通过 → 生成新版本（1.0.0 → 1.1.0…）→ 旧版本置 DEPRECATED
 * → 流程 current_version_id 指向新版本。草稿只在 DRAFT 版本上原地覆盖，不产生新版本。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowService {

    private final IntFlowMapper flowMapper;
    private final IntFlowVersionMapper versionMapper;
    private final FlowValidator validator;
    private final ObjectMapper objectMapper;

    /**
     * 流程列表。
     */
    public List<FlowVO> list(String keyword) {
        List<IntFlow> flows = flowMapper.selectList(Wrappers.<IntFlow>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(IntFlow::getFlowCode, keyword)
                        .or().like(IntFlow::getFlowName, keyword))
                .orderByDesc(IntFlow::getUpdatedAt));
        return flows.stream().map(this::toVO).toList();
    }

    /**
     * 流程详情（含当前 DSL：优先当前版本，没有则取草稿）。
     */
    public FlowDetailVO detail(Long id) {
        IntFlow flow = require(id);
        IntFlowVersion ver = currentVersion(flow);
        FlowDsl dsl = ver == null ? null : parseDsl(ver.getDslJson());
        return new FlowDetailVO(String.valueOf(flow.getId()), flow.getFlowCode(), flow.getFlowName(),
                flow.getTriggerType(), flow.getStatus(),
                ver == null ? null : ver.getVersion(), flow.getDescription(), dsl);
    }

    /**
     * 新建流程（同时落一个 1.0.0 草稿版本）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(FlowSaveRequest req) {
        long exists = flowMapper.selectCount(Wrappers.<IntFlow>lambdaQuery()
                .eq(IntFlow::getFlowCode, req.code()));
        if (exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "流程编码已存在");
        }
        IntFlow flow = new IntFlow();
        flow.setFlowCode(req.code());
        flow.setFlowName(req.name());
        flow.setTriggerType(req.triggerType() == null ? "HTTP" : req.triggerType());
        flow.setStatus("DRAFT");
        flow.setDescription(req.description());
        flowMapper.insert(flow);

        IntFlowVersion ver = newDraftVersion(flow.getId(), "1.0.0", req);
        flow.setCurrentVersionId(ver.getId());
        flowMapper.updateById(flow);
        log.info("创建流程 code={} id={}", req.code(), flow.getId());
        return flow.getId();
    }

    /**
     * 编辑流程：DRAFT 状态下原地更新草稿；已发布流程的修改会写回草稿版本（不影响线上版本）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FlowSaveRequest req) {
        IntFlow flow = require(id);
        flow.setFlowName(req.name());
        if (req.triggerType() != null) {
            flow.setTriggerType(req.triggerType());
        }
        flow.setDescription(req.description());
        flowMapper.updateById(flow);

        IntFlowVersion draft = versionMapper.selectOne(Wrappers.<IntFlowVersion>lambdaQuery()
                .eq(IntFlowVersion::getFlowId, id)
                .eq(IntFlowVersion::getStatus, "DRAFT")
                .orderByDesc(IntFlowVersion::getCreatedAt)
                .last("LIMIT 1"));
        FlowValidateResult result = validator.validate(req.dsl());
        if (draft != null) {
            draft.setDslJson(toJson(req.dsl()));
            draft.setDslHash(result.dslHash());
            draft.setNodeCount(result.nodeCount());
            versionMapper.updateById(draft);
        } else {
            IntFlowVersion ver = newDraftVersion(id, nextVersion(flow), req);
            flow.setCurrentVersionId(flow.getCurrentVersionId() == null ? ver.getId() : flow.getCurrentVersionId());
            flowMapper.updateById(flow);
        }
    }

    /**
     * 删除流程（逻辑删除，级联删版本）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        if ("PUBLISHED".equals(require(id).getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "流程已发布，请先下线再删除");
        }
        flowMapper.deleteById(id);
        versionMapper.delete(Wrappers.<IntFlowVersion>lambdaQuery().eq(IntFlowVersion::getFlowId, id));
    }

    /**
     * 仅校验不落库（编辑器实时校验用）。
     */
    public FlowValidateResult validate(FlowDsl dsl) {
        return validator.validate(dsl);
    }

    /**
     * 发布：校验通过才允许发布，生成新版本并把旧版本置为 DEPRECATED。
     */
    @Transactional(rollbackFor = Exception.class)
    public FlowVO publish(Long id, String changeLog) {
        IntFlow flow = require(id);
        IntFlowVersion draft = versionMapper.selectOne(Wrappers.<IntFlowVersion>lambdaQuery()
                .eq(IntFlowVersion::getFlowId, id)
                .eq(IntFlowVersion::getStatus, "DRAFT")
                .orderByDesc(IntFlowVersion::getCreatedAt)
                .last("LIMIT 1"));
        if (draft == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "没有待发布的草稿版本");
        }
        FlowValidateResult result = validator.validate(parseDsl(draft.getDslJson()));
        if (!result.valid()) {
            String detail = result.errors().isEmpty() ? "DSL 校验失败"
                    : result.errors().get(0).message() + "（共 " + result.errors().size() + " 个问题）";
            throw new BizException(ErrorCode.BAD_REQUEST, "无法发布：" + detail);
        }

        // 旧版本降级
        List<IntFlowVersion> old = versionMapper.selectList(Wrappers.<IntFlowVersion>lambdaQuery()
                .eq(IntFlowVersion::getFlowId, id)
                .eq(IntFlowVersion::getStatus, "PUBLISHED"));
        for (IntFlowVersion v : old) {
            v.setStatus("DEPRECATED");
            versionMapper.updateById(v);
        }

        // 版本号沿用草稿自身的版本：首个版本就是 1.0.0，
        // 后续每次编辑会新建 1.1.0 / 1.2.0 的草稿，发布时直接沿用
        draft.setStatus("PUBLISHED");
        draft.setChangeLog(changeLog);
        draft.setPublishedAt(OffsetDateTime.now());
        draft.setPublishedBy(com.eeip.common.security.SecurityUtils.currentUsername());
        versionMapper.updateById(draft);

        flow.setStatus("PUBLISHED");
        flow.setCurrentVersionId(draft.getId());
        flowMapper.updateById(flow);
        log.info("发布流程 id={} version={}", id, draft.getVersion());

        return toVO(flow);
    }

    /**
     * 下线：状态回到 DRAFT，线上版本置 DEPRECATED。
     */
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        IntFlow flow = require(id);
        IntFlowVersion published = versionMapper.selectPublished(id);
        if (published != null) {
            published.setStatus("DEPRECATED");
            versionMapper.updateById(published);
        }
        flow.setStatus("OFFLINE");
        flowMapper.updateById(flow);
    }

    /**
     * 版本历史。
     */
    public List<IntFlowVersion> versions(Long id) {
        require(id);
        return versionMapper.selectList(Wrappers.<IntFlowVersion>lambdaQuery()
                .eq(IntFlowVersion::getFlowId, id)
                .orderByDesc(IntFlowVersion::getCreatedAt));
    }

    // ── 内部方法 ──────────────────────────────────────────────

    private IntFlow require(Long id) {
        IntFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程不存在");
        }
        return flow;
    }

    private IntFlowVersion currentVersion(IntFlow flow) {
        if (flow.getCurrentVersionId() != null) {
            IntFlowVersion v = versionMapper.selectById(flow.getCurrentVersionId());
            if (v != null) {
                return v;
            }
        }
        return versionMapper.selectOne(Wrappers.<IntFlowVersion>lambdaQuery()
                .eq(IntFlowVersion::getFlowId, flow.getId())
                .orderByDesc(IntFlowVersion::getCreatedAt)
                .last("LIMIT 1"));
    }

    private IntFlowVersion newDraftVersion(Long flowId, String version, FlowSaveRequest req) {
        FlowValidateResult result = validator.validate(req.dsl());
        IntFlowVersion ver = new IntFlowVersion();
        ver.setFlowId(flowId);
        ver.setVersion(version);
        ver.setDslJson(toJson(req.dsl()));
        ver.setDslHash(result.dslHash());
        ver.setNodeCount(result.nodeCount());
        ver.setStatus("DRAFT");
        ver.setChangeLog(req.changeLog());
        versionMapper.insert(ver);
        return ver;
    }

    /** 版本号只升次版本：1.0.0 → 1.1.0 */
    private String nextVersion(IntFlow flow) {
        IntFlowVersion latest = versionMapper.selectOne(Wrappers.<IntFlowVersion>lambdaQuery()
                .eq(IntFlowVersion::getFlowId, flow.getId())
                .orderByDesc(IntFlowVersion::getCreatedAt)
                .last("LIMIT 1"));
        if (latest == null) {
            return "1.0.0";
        }
        String[] parts = latest.getVersion().split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return major + "." + (minor + 1) + ".0";
        } catch (NumberFormatException e) {
            return "1.0.0";
        }
    }

    private String toJson(FlowDsl dsl) {
        try {
            return objectMapper.writeValueAsString(dsl);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "DSL 序列化失败");
        }
    }

    private FlowDsl parseDsl(String json) {
        try {
            return objectMapper.readValue(json, FlowDsl.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL, "DSL 解析失败");
        }
    }

    private FlowVO toVO(IntFlow f) {
        IntFlowVersion ver = currentVersion(f);
        String hash = ver == null || ver.getDslHash() == null ? null : ver.getDslHash().substring(0, 12);
        return new FlowVO(String.valueOf(f.getId()), f.getFlowCode(), f.getFlowName(),
                f.getTriggerType(), f.getStatus(), ver == null ? null : ver.getVersion(),
                ver == null ? 0 : ver.getNodeCount(), hash, f.getDescription(), f.getUpdatedAt());
    }
}
