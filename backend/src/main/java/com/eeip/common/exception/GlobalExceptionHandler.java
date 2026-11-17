package com.eeip.common.exception;

import com.eeip.common.api.ErrorCode;
import com.eeip.common.api.R;
import com.eeip.common.i18n.MessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageResolver messages;

    public GlobalExceptionHandler(MessageResolver messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        ErrorCode ec = e.getErrorCode();
        log.warn("biz error code={} msg={}", ec.code(), e.getMessage());
        // 有国际化文案时优先使用；BizException 带的自定义明细优先于通用文案
        String localized = messages.resolve(ec.code(), ec.message());
        String detail = e.getMessage();
        String message = (detail != null && !detail.equals(ec.message())) ? detail : localized;
        return ResponseEntity.status(ec.status()).body(R.fail(ec, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(R.fail(ErrorCode.BAD_REQUEST, detail));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException e) {
        ErrorCode ec = ErrorCode.FORBIDDEN;
        return ResponseEntity.status(ec.status())
                .body(R.fail(ec, messages.resolve(ec.code(), ec.message())));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuth(AuthenticationException e) {
        ErrorCode ec = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(ec.status())
                .body(R.fail(ec, messages.resolve(ec.code(), ec.message())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnknown(Exception e, HttpServletRequest req) {
        log.error("unexpected error path={}", req.getRequestURI(), e);
        ErrorCode ec = ErrorCode.INTERNAL;
        return ResponseEntity.status(ec.status())
                .body(R.fail(ec, messages.resolve(ec.code(), ec.message())));
    }

    private String formatFieldError(FieldError f) {
        return f.getField() + ": " + f.getDefaultMessage();
    }
}
