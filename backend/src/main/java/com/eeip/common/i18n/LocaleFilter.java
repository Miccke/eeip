package com.eeip.common.i18n;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 语言过滤器：把 Accept-Language 解析成受支持的语言写入上下文。
 *
 * <p>必须最先执行（早于安全过滤器），保证异常信息也能按请求语言返回。</p>
 */
@Component
@Order(Integer.MIN_VALUE + 10)
public class LocaleFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            LocaleContext.set(LocaleContext.normalize(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)));
            chain.doFilter(request, response);
        } finally {
            LocaleContext.clear();
        }
    }
}
