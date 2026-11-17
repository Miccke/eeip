package com.eeip.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 统一配置。
 *
 * <p>雪花 ID 是 19 位长整型，超过 JavaScript 的 Number.MAX_SAFE_INTEGER(2^53-1)，
 * 直接以数字返回会被前端四舍五入（例如 ...737 变成 ...700），
 * 导致"按 ID 查询/更新"找不到数据。因此统一把 Long 序列化为字符串。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
