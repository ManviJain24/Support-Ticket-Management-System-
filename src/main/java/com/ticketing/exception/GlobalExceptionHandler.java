package com.ticketing.exception;

import com.ticketing.dto.response.ErrorDetail;
import com.ticketing.dto.response.ErrorResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.name(), exception.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return validationResponse(details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception) {
        return validationResponse(List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected request failure", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_ERROR.name(),
                        "An unexpected error occurred",
                        List.of()));
    }

    private ResponseEntity<ErrorResponse> validationResponse(List<ErrorDetail> details) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.VALIDATION_FAILED.name(),
                        "Request validation failed",
                        details));
    }
}
