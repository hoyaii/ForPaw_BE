package com.hong.ForPaw.core.errors;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.core.utils.EnumUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> customError(CustomException e) {
        return new ResponseEntity<>(e.body(), e.status());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unknownServerError(Exception e){
        String message = e.getMessage();
        ApiUtils.ApiResult<?> apiResult = ApiUtils.error(message, HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(apiResult, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        String errorMessage = result.getFieldError().getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
    }

    // 잘못된 데이터 형식에 대한 예외처리 (JSON 파싱 시 발생하는 에러 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String errorMessage = "요청 데이터 형식이 올바르지 않습니다.";

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            if (ife.getTargetType().isEnum()) {
                String enumValues = EnumUtils.getEnumValuesAsString((Class<? extends Enum<?>>) ife.getTargetType());
                errorMessage = "유효하지 않은 값입니다. 허용된 값은 " + enumValues + " 입니다.";
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
            }
        } else if (ex.getMessage().contains("Required request body is missing")) {
            errorMessage = "요청 본문이 누락되었습니다.";
        }

        return ResponseEntity.badRequest().body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        String errorMessage = "요청 파라미터 " + ex.getParameterName() + "가 누락되었습니다.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
    }

    // IOException 처리
    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleIOException(IOException ex) {
        String errorMessage = "입출력 오류가 발생했습니다: " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiUtils.error(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        String errorMessage = "잘못된 요청입니다: " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiUtils.error(errorMessage, HttpStatus.BAD_REQUEST));
    }
}