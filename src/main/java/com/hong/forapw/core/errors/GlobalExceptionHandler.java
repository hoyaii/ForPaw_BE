package com.hong.forapw.core.errors;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.hong.forapw.core.utils.ApiUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException ex) {
        String traceId = getTraceId();
        log.error("[Trace ID: {}] CustomException 발생: {}", traceId, ex.getMessage(), ex);
        return ResponseEntity.status(ex.status()).body(ApiUtils.error(ex.getMessage(), ex.status()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String traceId = getTraceId();
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();
        String errorMessage = String.join(", ", errors);

        log.warn("[Trace ID: {}] 유효성 검사 실패: {}", traceId, errorMessage);
        return ResponseEntity.badRequest().body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String errorMessage = getLocalizedMessage("error.invalid.data.format");

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
            Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) ife.getTargetType();
            String enumValues = Arrays.stream(enumType.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            errorMessage = String.format(getLocalizedMessage("error.invalid.enum.value"), enumValues);
        } else if (ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")) {
            errorMessage = getLocalizedMessage("error.missing.body");
        }

        log.warn("[Trace ID: {}] 읽을 수 없는 메시지: {} | 요청 정보: {}", traceId, errorMessage, getRequestDetails(request), ex);
        return ResponseEntity.badRequest().body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String errorMessage = String.format(getLocalizedMessage("error.missing.parameter"), ex.getParameterName());

        log.warn("[Trace ID: {}] 요청 파라미터 누락: {} | 요청 정보: {}", traceId, ex.getParameterName(), getRequestDetails(request), ex);
        return ResponseEntity.badRequest().body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        String traceId = getTraceId();
        String errorMessage = getLocalizedMessage("error.illegal.argument");

        log.warn("[Trace ID: {}] 잘못된 인자: {}", traceId, ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException ex) {
        String traceId = getTraceId();
        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("[Trace ID: {}] 유효성 검사 실패: {}", traceId, errorMessage, ex);
        return ResponseEntity.badRequest().body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String errorMessage = getLocalizedMessage("error.runtime");

        log.error("[Trace ID: {}] 런타임 예외 발생: {} | 요청 정보: {}", traceId, ex.getMessage(), getRequestDetails(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiUtils.error(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private String getTraceId() {
        return MDC.get("traceId");
    }

    private String getLocalizedMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    private String getRequestDetails(HttpServletRequest request) {
        return String.format("URL: %s, Method: %s", request.getRequestURL(), request.getMethod());
    }
}