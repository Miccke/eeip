package com.eeip.module.integration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 本地 mock 服务：给连接器与流程引擎做稳定演示，避免依赖外网。
 *
 * <p>免认证（{@code /api/mock/**} 在安全配置里放行），只返回构造数据，不触达任何内部资源。</p>
 */
@Tag(name = "演示 · Mock 服务", description = "本地可造的响应：回显、延迟、指定状态码、随机失败")
@RestController
@RequestMapping("/api/mock")
public class MockController {

    @Operation(summary = "回显", description = "原样返回请求参数与头，用于验证连接器转发是否正确。")
    @RequestMapping(value = "/echo", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
    public Map<String, Object> echo(
            @Parameter(description = "任意查询参数") @RequestParam(required = false) Map<String, String> query,
            @Parameter(description = "任意 JSON 体") @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("query", query == null ? Map.of() : query);
        resp.put("body", body == null ? Map.of() : body);
        resp.put("serverTime", OffsetDateTime.now().toString());
        resp.put("traceId", "mock-" + Long.toHexString(ThreadLocalRandom.current().nextLong(0, 0xffffff)));
        return resp;
    }

    @Operation(summary = "订单样例", description = "固定结构的业务数据，供字段映射演示。")
    @GetMapping("/orders/{orderNo}")
    public Map<String, Object> order(
            @Parameter(description = "订单号", example = "SO-20261118-001") @PathVariable String orderNo) {
        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("value", 128000.50);
        amount.put("currency", "CNY");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("customerCode", "C-10086");
        data.put("customerLevel", "VIP");
        data.put("amount", amount);
        data.put("status", "CREATED");
        data.put("createdAt", OffsetDateTime.now().toString());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", 0);
        resp.put("message", "ok");
        resp.put("data", data);
        return resp;
    }

    @Operation(summary = "指定状态码", description = "返回指定 HTTP 状态码，用于验证失败分支与重试策略。")
    @GetMapping("/status/{code}")
    public ResponseEntity<Map<String, Object>> status(
            @Parameter(description = "HTTP 状态码", example = "503") @PathVariable int code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", "mock status " + code);
        body.put("serverTime", OffsetDateTime.now().toString());
        return ResponseEntity.status(code).body(body);
    }

    @Operation(summary = "延迟响应", description = "延迟指定毫秒后返回，用于验证超时配置。")
    @GetMapping("/delay")
    public Map<String, Object> delay(
            @Parameter(description = "延迟毫秒，上限 60000", example = "3000")
            @RequestParam(defaultValue = "1000") int ms) throws InterruptedException {
        int wait = Math.min(Math.max(ms, 0), 60000);
        Thread.sleep(wait);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("delayedMs", wait);
        return resp;
    }

    @Operation(summary = "按概率失败", description = "用于压测与重试演示；rate=0.3 表示 30% 概率返回 500。")
    @GetMapping("/flaky")
    public ResponseEntity<Map<String, Object>> flaky(
            @Parameter(description = "失败概率 0–1", example = "0.3")
            @RequestParam(defaultValue = "0.3") double rate) {
        boolean fail = ThreadLocalRandom.current().nextDouble() < Math.min(Math.max(rate, 0), 1);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", !fail);
        body.put("message", fail ? "mock failure" : "ok");
        return ResponseEntity.status(fail ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK).body(body);
    }
}
