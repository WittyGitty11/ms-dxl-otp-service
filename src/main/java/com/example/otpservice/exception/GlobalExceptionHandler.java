package com.example.otpservice.exception;

import com.example.otpservice.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SYSTEM = "OTP-SVC";

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtpException(OtpException ex) {
        log.warn("System={},EventType=OTP_ERROR|BUSINESS_EXCEPTION,Message={}",
                SYSTEM, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // Intentionally sparse log — no correlationId or MSISDN context.
    // You must cross-reference the preceding MSISDN_PARSE_INITIATED debug log
    // and read OtpService#parseMsisdnAsLong to understand the root cause.
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleNumberFormat(NumberFormatException ex) {
        log.warn("System={},EventType=OTP_GENERATE|MSISDN_PARSE_ERROR,Message=MSISDN numeric conversion failed",
                SYSTEM);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Invalid MSISDN format. MSISDN must be numeric."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + "=" + fe.getDefaultMessage())
                .collect(Collectors.joining(","));
        log.warn("System={},EventType=OTP_ERROR|VALIDATION_EXCEPTION,Fields={}", SYSTEM, errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Validation failed: " + errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("System={},EventType=OTP_ERROR|UNHANDLED_EXCEPTION,ExceptionType={},Message={}",
                SYSTEM, ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred."));
    }
}