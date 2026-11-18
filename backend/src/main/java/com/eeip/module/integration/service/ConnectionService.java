package com.eeip.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.exception.BizException;
import com.eeip.module.integration.connector.HttpConnector;
import com.eeip.module.integration.dto.ConnectionSaveRequest;
import com.eeip.module.integration.entity.IntConnection;
import com.eeip.module.integration.mapper.IntConnectionMapper;
import com.eeip.module.integration.vo.ConnectionTestResult;
import com.eeip.module.integration.vo.ConnectionVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 连接管理服务：CRUD 与连通性测试。
 *
 * <p>凭证（authConfig）只进不出：保存时写入，查询时一律不返回。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final IntConnectionMapper connectionMapper;
    private final HttpConnector httpConnector;
    private final ObjectMapper objectMapper;

    /**
     * 连接列表（按编码排序）。
     */
    public List<ConnectionVO> list(String keyword) {
        return connectionMapper.selectList(Wrappers.<IntConnection>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(IntConnection::getCode, keyword)
                                .or().like(IntConnection::getName, keyword))
                        .orderByAsc(IntConnection::getCode))
                .stream().map(this::toVO).toList();
    }

    /**
     * 新增连接。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ConnectionSaveRequest req) {
        long exists = connectionMapper.selectCount(Wrappers.<IntConnection>lambdaQuery()
                .eq(IntConnection::getCode, req.code()));
        if (exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "连接编码已存在");
        }
        IntConnection conn = new IntConnection();
        fill(conn, req, true);
        connectionMapper.insert(conn);
        return conn.getId();
    }

    /**
     * 编辑连接。authConfig 留空表示沿用原凭证。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ConnectionSaveRequest req) {
        IntConnection conn = require(id);
        fill(conn, req, false);
        connectionMapper.updateById(conn);
    }

    /**
     * 删除连接（逻辑删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        connectionMapper.deleteById(id);
    }

    /**
     * 测试连通性：发起真实请求，结果回写到连接记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public ConnectionTestResult test(Long id) {
        IntConnection conn = require(id);
        HttpConnector.HttpResult result = httpConnector.invoke(new HttpConnector.HttpSpec(
                conn.getEndpoint(),
                conn.getMethod() == null ? "GET" : conn.getMethod(),
                parseMap(conn.getHeadersJson()),
                null,
                conn.getTimeoutMs(),
                conn.getAuthType(),
                parseMap(conn.getAuthConfig())));

        // 用 UpdateWrapper 显式 set：updateById 会跳过 null 字段，
        // 导致"这次成功了但上次的错误信息还留着"
        connectionMapper.update(null, Wrappers.<IntConnection>lambdaUpdate()
                .set(IntConnection::getLastCheckAt, OffsetDateTime.now())
                .set(IntConnection::getLastCheckStatus, result.success() ? "SUCCESS" : "FAILED")
                .set(IntConnection::getLastCheckMs, (int) result.costMs())
                .set(IntConnection::getLastCheckError,
                        result.error() == null ? null : truncate(result.error(), 500))
                .eq(IntConnection::getId, id));

        return new ConnectionTestResult(result.success(), result.status(), result.costMs(),
                truncate(result.body(), 500), result.headers(), result.error());
    }

    /**
     * 保存前预检：不落库，直接按请求参数发起一次调用。
     */
    public ConnectionTestResult testDraft(ConnectionSaveRequest req) {
        HttpConnector.HttpResult result = httpConnector.invoke(new HttpConnector.HttpSpec(
                req.endpoint(),
                req.method() == null ? "GET" : req.method(),
                parseMap(req.headersJson()),
                null,
                req.timeoutMs(),
                req.authType(),
                parseMap(req.authConfig())));
        return new ConnectionTestResult(result.success(), result.status(), result.costMs(),
                truncate(result.body(), 500), result.headers(), result.error());
    }

    private IntConnection require(Long id) {
        IntConnection conn = connectionMapper.selectById(id);
        if (conn == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "连接不存在");
        }
        return conn;
    }

    /** 公共字段填充；isCreate 时校验 authConfig 必填规则 */
    private void fill(IntConnection conn, ConnectionSaveRequest req, boolean isCreate) {
        conn.setCode(req.code());
        conn.setSystemCode(req.systemCode());
        conn.setProtocol(req.protocol() == null ? "HTTP" : req.protocol());
        conn.setName(req.name());
        conn.setType("HTTP");
        conn.setEndpoint(req.endpoint());
        conn.setMethod(req.method() == null ? "GET" : req.method().toUpperCase());
        conn.setAuthType(req.authType() == null ? "NONE" : req.authType().toUpperCase());
        conn.setHeadersJson(req.headersJson());
        conn.setTimeoutMs(req.timeoutMs() == null ? 5000 : req.timeoutMs());
        conn.setStatus(req.status() == null ? 1 : req.status());
        conn.setRemark(req.remark());
        if (StringUtils.hasText(req.authConfig())) {
            conn.setAuthConfig(req.authConfig());
        } else if (isCreate) {
            conn.setAuthConfig("{}");
        }
    }

    private ConnectionVO toVO(IntConnection c) {
        boolean hasAuth = StringUtils.hasText(c.getAuthConfig()) && !"{}".equals(c.getAuthConfig().trim());
        return new ConnectionVO(c.getId(), c.getCode(), c.getName(), c.getType(), c.getEndpoint(),
                c.getMethod(), c.getAuthType(), hasAuth, c.getStatus(), c.getLastCheckStatus(),
                c.getLastCheckMs(), c.getLastCheckError(), c.getLastCheckAt(), c.getRemark());
    }

    private Map<String, String> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "JSON 配置格式不合法");
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
