package com.fashionstore.backend.exception;

import com.fashionstore.backend.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return buildResponse(errorCode.getHttpStatus(), errorCode.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null
                ? ErrorCode.INVALID_REQUEST.getMessage()
                : fieldError.getField() + ": " + fieldError.getDefaultMessage();

        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST.getCode(), message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        return buildResponse(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return buildResponse(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleConcurrentUpdate(ConcurrencyFailureException exception) {
        ErrorCode errorCode = ErrorCode.CONCURRENT_UPDATE;
        return buildResponse(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unexpected application error", exception);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        return buildResponse(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
            HttpStatus status,
            int code,
            String message) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(code)
                .message(message)
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
