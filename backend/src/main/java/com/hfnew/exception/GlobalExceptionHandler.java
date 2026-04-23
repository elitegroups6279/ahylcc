package com.hfnew.exception;

import com.hfnew.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Object>> handleBizException(BizException ex) {
        ApiResponse<Object> resp = ApiResponse.fail(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(resp);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ApiResponse<Object> resp = ApiResponse.fail(400, msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: ", ex);
        String raw = ex.getMessage() != null ? ex.getMessage() : "";
        String msg;
        if (raw.contains("t_user.username")) {
            msg = "用户名已存在";
        } else if (raw.contains("t_organization.org_code")) {
            msg = "机构编码已存在";
        } else if (raw.contains("Duplicate entry")) {
            msg = "数据已存在，请检查是否有重复值";
        } else {
            msg = "数据约束冲突，请检查输入数据";
        }
        ApiResponse<Object> resp = ApiResponse.fail(400, msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception ex) {
        log.error("Unhandled exception: ", ex);
        String msg = ex.getMessage() != null ? ex.getMessage() : "internal error";
        ApiResponse<Object> resp = ApiResponse.fail(500, msg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }
}
