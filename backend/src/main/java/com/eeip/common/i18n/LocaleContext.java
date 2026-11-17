package com.eeip.common.i18n;

import java.util.Locale;

/**
 * 请求语言上下文（ThreadLocal）。
 *
 * <p>由 {@link LocaleFilter} 依据 Accept-Language 写入，请求结束清理。</p>
 */
public final class LocaleContext {

    /** 支持的三种语言：简体中文 / 繁体中文 / 英文 */
    public static final Locale ZH_CN = Locale.SIMPLIFIED_CHINESE;
    public static final Locale ZH_TW = Locale.TRADITIONAL_CHINESE;
    public static final Locale EN = Locale.US;

    private static final ThreadLocal<Locale> CURRENT = new ThreadLocal<>();

    private LocaleContext() {
    }

    public static void set(Locale locale) {
        CURRENT.set(locale);
    }

    public static Locale get() {
        Locale l = CURRENT.get();
        return l == null ? ZH_CN : l;
    }

    /** 当前语言代码，如 zh-CN / zh-TW / en-US */
    public static String code() {
        return toCode(get());
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** 把任意 Accept-Language 值归一化为我们支持的三种之一 */
    public static Locale normalize(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return ZH_CN;
        }
        String v = acceptLanguage.toLowerCase();
        if (v.startsWith("en")) {
            return EN;
        }
        // zh-TW / zh-HK / zh-Hant → 繁体
        if (v.startsWith("zh-tw") || v.startsWith("zh-hk") || v.contains("hant")) {
            return ZH_TW;
        }
        return ZH_CN;
    }

    public static String toCode(Locale locale) {
        if (EN.equals(locale)) {
            return "en-US";
        }
        if (ZH_TW.equals(locale)) {
            return "zh-TW";
        }
        return "zh-CN";
    }
}
