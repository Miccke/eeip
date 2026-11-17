package com.eeip.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * 国际化配置：加载 classpath:i18n/messages{_区域}.properties。
 *
 * <p>使用 ResourceBundleMessageSource（而非 Reloadable 版本），
 * 它对 classpath 下的 properties 命名规则支持最稳；默认编码 UTF-8。</p>
 */
@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(false);
        return source;
    }
}
