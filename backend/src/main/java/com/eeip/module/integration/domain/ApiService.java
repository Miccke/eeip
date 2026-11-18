package com.eeip.module.integration.domain;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.common.security.SecurityUtils;
import com.eeip.module.integration.entity.IntConnection;
import com.eeip.module.integration.expose.ApiExpose;
import com.eeip.module.integration.mapper.ApiExposeMapper;
import com.eeip.module.integration.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 接口台账服务：接口身份 + 契约版本 + 连接器绑定。
 *
 * <p>版本语义与流程一致：新建落 1.0.0 草稿，发布后旧版本置 DEPRECATED。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiService {

    private final ApiDefinitionMapper apiMapper;
    private final ApiVersionMapper versionMapper;
    private final ApiConnectorMapper connectorMapper;
    private final IntConnectionMapper connectionMapper;
    private final ApiExposeMapper exposeMapper;
    private final SystemService systemService;

    /** 列表（按系统过滤可选） */
    public List<ApiVO> list(String systemCode, String keyword) {
        return apiMapper.selectList(Wrappers.<ApiDefinition>lambdaQuery()
                        .eq(StringUtils.hasText(systemCode), ApiDefinition::getSystemCode, systemCode)
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(ApiDefinition::getCode, keyword)
                                .or().like(ApiDefinition::getName, keyword))
                        .orderByAsc(ApiDefinition::getSystemCode)
                        .orderByAsc(ApiDefinition::getCode))
                .stream().map(this::toVO).toList();
    }

    /** 新建接口：落身份 + 1.0.0 草稿契约 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApiSaveRequest req) {
        systemService.requireEnabled(req.systemCode());
        long dup = apiMapper.selectCount(Wrappers.<ApiDefinition>lambdaQuery()
                .eq(ApiDefinition::getSystemCode, req.systemCode())
                .eq(ApiDefinition::getCode, req.code()));
        if (dup > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该系统下接口编码已存在");
        }
        validateConnectors(req.systemCode(), req.connectorCodes());

        ApiDefinition api = new ApiDefinition();
        api.setSystemCode(req.systemCode());
        api.setCode(req.code());
        api.setName(req.name());
        api.setStatus("DRAFT");
        api.setDescription(req.description());
        api.setProtocol(req.protocol() == null ? "HTTP" : req.protocol().toUpperCase());
        api.setSlaLevel(req.slaLevel() == null ? "L2" : req.slaLevel());
        api.setTargetType(req.targetType() == null ? "CONNECTOR" : req.targetType());
        api.setFlowCode(req.flowCode());
        api.setDomainCode(systemService.requireEnabled(req.systemCode()).getDomainCode());
        api.setApiNo(generateApiNo(api.getDomainCode(), api.getSystemCode()));
        apiMapper.insert(api);

        ApiVersion ver = newVersion(api.getId(), "1.0.0", req);
        api.setCurrentVersionId(ver.getId());
        apiMapper.updateById(api);
        return api.getId();
    }

    /** 编辑：更新身份 + 当前草稿契约；已发布则新建下一个版本草稿 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ApiSaveRequest req) {
        ApiDefinition api = require(id);
        api.setName(req.name());
        api.setDescription(req.description());
        if (req.protocol() != null) {
            api.setProtocol(req.protocol().toUpperCase());
        }
        if (req.slaLevel() != null) {
            api.setSlaLevel(req.slaLevel());
        }
        if (req.targetType() != null) {
            api.setTargetType(req.targetType());
        }
        api.setFlowCode(req.flowCode());
        apiMapper.updateById(api);

        validateConnectors(api.getSystemCode(), req.connectorCodes());

        ApiVersion draft = versionMapper.selectOne(Wrappers.<ApiVersion>lambdaQuery()
                .eq(ApiVersion::getApiId, id)
                .eq(ApiVersion::getStatus, "DRAFT")
                .orderByDesc(ApiVersion::getCreatedAt)
                .last("LIMIT 1"));
        if (draft != null) {
            draft.setMethod(req.method() == null ? "GET" : req.method().toUpperCase());
            draft.setPath(req.path());
            draft.setChangeLog(req.changeLog());
            versionMapper.updateById(draft);
            bindConnectors(draft.getId(), req.connectorCodes());
        } else {
            String next = nextVersion(api.getId());
            ApiVersion v = newVersion(id, next, req);
            log.info("接口 {} 生成新草稿版本 {}", api.getCode(), next);
            if (api.getCurrentVersionId() == null) {
                api.setCurrentVersionId(v.getId());
                apiMapper.updateById(api);
            }
        }
    }

    /** 发布：校验通过后置 PUBLISHED，旧版本 DEPRECATED */
    @Transactional(rollbackFor = Exception.class)
    public ApiVO publish(Long id) {
        ApiDefinition api = require(id);
        ApiVersion draft = versionMapper.selectOne(Wrappers.<ApiVersion>lambdaQuery()
                .eq(ApiVersion::getApiId, id)
                .eq(ApiVersion::getStatus, "DRAFT")
                .orderByDesc(ApiVersion::getCreatedAt)
                .last("LIMIT 1"));
        if (draft == null) {
            // 没有草稿（例如下线后重新发布）：取最新版本重新发布
            draft = versionMapper.selectOne(Wrappers.<ApiVersion>lambdaQuery()
                    .eq(ApiVersion::getApiId, id)
                    .orderByDesc(ApiVersion::getCreatedAt).last("LIMIT 1"));
        }
        if (draft == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "接口还没有任何版本");
        }
        List<ApiConnector> conns = connectorMapper.selectList(Wrappers.<ApiConnector>lambdaQuery()
                .eq(ApiConnector::getApiVersionId, draft.getId())
                .eq(ApiConnector::getStatus, 1)
                .orderByAsc(ApiConnector::getId));
        // 直连型接口必须绑连接器；走流程的接口不需要
        boolean flowImpl = "FLOW".equals(api.getTargetType());
        if (conns.isEmpty() && !flowImpl) {
            throw new BizException(ErrorCode.BAD_REQUEST, "发布前请至少绑定一个连接器");
        }
        if (flowImpl && !StringUtils.hasText(api.getFlowCode())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该接口走流程实现，请先填写流程编码");
        }

        for (ApiVersion old : versionMapper.selectList(Wrappers.<ApiVersion>lambdaQuery()
                .eq(ApiVersion::getApiId, id).eq(ApiVersion::getStatus, "PUBLISHED"))) {
            old.setStatus("DEPRECATED");
            versionMapper.updateById(old);
        }
        draft.setStatus("PUBLISHED");
        draft.setPublishedAt(OffsetDateTime.now());
        draft.setPublishedBy(SecurityUtils.currentUsername());
        versionMapper.updateById(draft);

        api.setStatus("PUBLISHED");
        api.setCurrentVersionId(draft.getId());
        apiMapper.updateById(api);

        // 发布即开放：自动生成对外路径并同步发布记录
        upsertExpose(api, draft, conns);
        return toVO(api);
    }

    /** 下线 */
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        ApiDefinition api = require(id);
        for (ApiVersion v : versionMapper.selectList(Wrappers.<ApiVersion>lambdaQuery()
                .eq(ApiVersion::getApiId, id).eq(ApiVersion::getStatus, "PUBLISHED"))) {
            v.setStatus("DEPRECATED");
            versionMapper.updateById(v);
        }
        api.setStatus("OFFLINE");
        apiMapper.updateById(api);
    }

    /** 删除 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        apiMapper.deleteById(id);
        versionMapper.delete(Wrappers.<ApiVersion>lambdaQuery().eq(ApiVersion::getApiId, id));
    }

    /** 接口编号：API-{领域}-{四位序号}，按领域内已有数量递增 */
    private String generateApiNo(String domainCode, String systemCode) {
        String d = domainCode == null ? "GEN" : domainCode;
        long n = apiMapper.selectCount(Wrappers.<ApiDefinition>lambdaQuery()
                .eq(ApiDefinition::getDomainCode, d)) + 1;
        return String.format("API-%s-%04d", d, n);
    }

    /** 对外路径自动生成：/api/open/v{version}/{systemCode}/{apiCode} */
    public String publicPath(String systemCode, String apiCode) {
        return "/api/open/v1/" + systemCode + "/" + apiCode;
    }

    /**
     * 对外发布：路径由系统编码 + 接口编码自动生成，不可手工编辑。
     * 已存在发布记录时原地更新（保留其鉴权方式与 Key）。
     */
    private void upsertExpose(ApiDefinition api, ApiVersion ver, List<ApiConnector> conns) {
        String rel = "/v1/" + api.getSystemCode() + "/" + api.getCode();
        String full = "/api/open" + rel;
        ApiExpose e = exposeMapper.selectOne(Wrappers.<ApiExpose>lambdaQuery()
                .eq(ApiExpose::getApiId, api.getId()).last("LIMIT 1"));
        boolean isNew = e == null;
        if (isNew) {
            e = new ApiExpose();
            e.setCode(api.getCode());
            // 登记到台账的接口默认走"应用签名"鉴权（JWT + MD5），符合对外开放的安全要求
            e.setAuthType("SIGNATURE");
            e.setTargetType("PROXY");
            e.setQps(0);
            e.setVersion("v1");
        }
        e.setApiId(api.getId());
        e.setApiVersionId(ver.getId());
        e.setName(api.getName());
        e.setMethod(ver.getMethod());
        e.setPath(rel);
        e.setPublicPath(full);
        e.setUpstreamPath(ver.getPath());
        e.setUpstreamMethod(ver.getMethod());
        e.setConnectionCode(conns.isEmpty() ? null : conns.get(0).getConnectorCode());
        // 接口若声明走流程，则对外发布类型为 FLOW
        if ("FLOW".equals(api.getTargetType())) {
            e.setTargetType("FLOW");
            e.setFlowCode(api.getFlowCode());
        } else {
            e.setTargetType("PROXY");
            e.setFlowCode(null);
        }
        e.setStatus(1);
        if (isNew) {
            e.setApiKey("ak_" + java.util.UUID.randomUUID().toString().replace("-", ""));
            exposeMapper.insert(e);
        } else {
            exposeMapper.updateById(e);
        }
    }

    // ── 内部 ────────────────────────────────────────────

    private ApiVersion newVersion(Long apiId, String version, ApiSaveRequest req) {
        ApiVersion v = new ApiVersion();
        v.setApiId(apiId);
        v.setVersion(version);
        v.setMethod(req.method() == null ? "GET" : req.method().toUpperCase());
        v.setPath(req.path());
        v.setStatus("DRAFT");
        v.setChangeLog(req.changeLog());
        versionMapper.insert(v);
        bindConnectors(v.getId(), req.connectorCodes());
        return v;
    }

    /** 覆盖式绑定连接器：第一个为主、其余为备 */
    private void bindConnectors(Long versionId, List<String> codes) {
        connectorMapper.delete(Wrappers.<ApiConnector>lambdaQuery()
                .eq(ApiConnector::getApiVersionId, versionId));
        if (codes == null) {
            return;
        }
        int i = 0;
        for (String code : codes) {
            if (!StringUtils.hasText(code)) {
                continue;
            }
            ApiConnector c = new ApiConnector();
            c.setApiVersionId(versionId);
            c.setConnectorCode(code);
            c.setRole(i == 0 ? "MAIN" : "BACKUP");
            c.setWeight(i == 0 ? 100 : 10);
            c.setStatus(1);
            connectorMapper.insert(c);
            i++;
        }
    }

    private void validateConnectors(String systemCode, List<String> codes) {
        if (codes == null) {
            return;
        }
        for (String code : codes) {
            if (!StringUtils.hasText(code)) {
                continue;
            }
            IntConnection c = connectionMapper.selectOne(Wrappers.<IntConnection>lambdaQuery()
                    .eq(IntConnection::getCode, code).last("LIMIT 1"));
            if (c == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "连接器不存在：" + code);
            }
            if (StringUtils.hasText(c.getSystemCode())
                    && !c.getSystemCode().equals(systemCode)) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "连接器 " + code + " 属于系统 " + c.getSystemCode() + "，不能绑到 " + systemCode);
            }
        }
    }

    private String nextVersion(Long apiId) {
        ApiVersion latest = versionMapper.selectOne(Wrappers.<ApiVersion>lambdaQuery()
                .eq(ApiVersion::getApiId, apiId)
                .orderByDesc(ApiVersion::getCreatedAt).last("LIMIT 1"));
        if (latest == null) {
            return "1.0.0";
        }
        String[] p = latest.getVersion().split("\\.");
        try {
            return p[0] + "." + (Integer.parseInt(p[1]) + 1) + ".0";
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private ApiDefinition require(Long id) {
        ApiDefinition api = apiMapper.selectById(id);
        if (api == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "接口不存在");
        }
        return api;
    }

    private ApiVO toVO(ApiDefinition api) {
        ApiVersion ver = api.getCurrentVersionId() == null ? null
                : versionMapper.selectById(api.getCurrentVersionId());
        // 兜底：迁移数据可能没有 current_version_id，取已发布版本
        if (ver == null) {
            ver = versionMapper.selectOne(Wrappers.<ApiVersion>lambdaQuery()
                    .eq(ApiVersion::getApiId, api.getId())
                    .orderByDesc(ApiVersion::getCreatedAt).last("LIMIT 1"));
        }
        List<String> conns = ver == null ? List.of()
                : connectorMapper.selectList(Wrappers.<ApiConnector>lambdaQuery()
                        .eq(ApiConnector::getApiVersionId, ver.getId())
                        .orderByAsc(ApiConnector::getId))
                .stream().map(ApiConnector::getConnectorCode).toList();
        return new ApiVO(String.valueOf(api.getId()), api.getApiNo(), api.getSystemCode(),
                api.getProtocol(), api.getSlaLevel(), api.getDomainCode(),
                api.getTargetType(), api.getFlowCode(), api.getCode(), api.getName(),
                api.getStatus(), ver == null ? null : ver.getVersion(),
                ver == null ? null : ver.getMethod(), ver == null ? null : ver.getPath(),
                conns, publicPath(api.getSystemCode(), api.getCode()),
                api.getDescription(), api.getUpdatedAt());
    }
}
