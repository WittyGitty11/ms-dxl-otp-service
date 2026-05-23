package com.example.otpservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        // Log the exception with MDC context if available
        String eventType = MDC.get("eventType");
        if (eventType != null && !eventType.isEmpty()) {
            MDC.put("eventType", eventType + "|ERROR");
        } else {
            MDC.put("eventType", "EXCEPTION|ERROR");
        }
        
        log.error("Exception occurred: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

// Made with Bob
