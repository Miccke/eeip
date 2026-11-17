package com.eeip.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置。
 *
 * <p>访问地址：</p>
 * <ul>
 *   <li>Swagger UI：/swagger-ui.html</li>
 *   <li>OpenAPI JSON：/v3/api-docs</li>
 * </ul>
 *
 * <p>点击右上角 Authorize 填入登录接口返回的 accessToken 即可调试需要权限的接口。</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI eeipOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EEIP 企业级系统集成平台 API")
                        .version("v0.1")
                        .description("""
                                当前版本已实现：认证（JWT）、RBAC 权限（用户/角色/权限点）、操作审计、数据范围。

                                调试步骤：
                                1. 调用 `POST /api/v1/auth/login`（admin / Admin@123）拿到 accessToken；
                                2. 点击右上角 Authorize，粘贴 token（不需要加 Bearer 前缀）；
                                3. 其余接口即可正常调用。
                                """)
                        .contact(new Contact().name("集成平台项目组").email("eeip@example.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录接口返回的 JWT")));
    }
}
