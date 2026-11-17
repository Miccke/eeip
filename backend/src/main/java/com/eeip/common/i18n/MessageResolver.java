package com.eeip.common.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

/**
 * 文案解析：按错误码从 i18n/messages*.properties 取当前语言的文案，
 * 取不到时回落到 {@code ErrorCode} 里内置的中文默认值。
 */
@Component
@RequiredArgsConstructor
public class MessageResolver {

    private final MessageSource messageSource;

    /**
     * @param key      文案键（此处用错误码本身，如 AUTH_401_BAD_CREDENTIALS）
     * @param fallback 兜底文案
     */
    public String resolve(String key, String fallback) {
        try {
            return messageSource.getMessage(key, null, LocaleContext.get());
        } catch (NoSuchMessageException e) {
            return fallback;
        }
    }
}
