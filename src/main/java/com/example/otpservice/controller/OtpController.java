package com.example.otpservice.controller;

import com.example.otpservice.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/otp")
public class OtpController {
    private final OtpService otpService;
    private final ObjectMapper objectMapper;

    public OtpController(OtpService otpService, ObjectMapper objectMapper) {
        this.otpService = otpService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(@RequestBody Map<String, String> request) {
        String correlationId = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        
        try {
            // Setup MDC for request logging
            MDC.put("system", "ms-dxl-otp-service");
            MDC.put("correlationId", correlationId);
            MDC.put("usecase", "LOGIN");
            MDC.put("channel", "OTP-API");
            MDC.put("eventType", "OTP_GENERATE|REQUEST_RECEIVED");
            
            // Log request body
            String requestBody = objectMapper.writeValueAsString(request);
            log.info("Incoming request body: " + requestBody);
            
            String otp = otpService.generateOtp(request.get("msisdn"));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("otp", otp);
            response.put("msisdn", request.get("msisdn"));
            
            MDC.put("eventType", "OTP_GENERATE|RESPONSE_SENT");
            String responseBody = objectMapper.writeValueAsString(response);
            log.info("Outgoing response body: " + responseBody);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            MDC.put("eventType", "OTP_GENERATE|REQUEST_ERROR");
            log.error("Error processing generate request: " + e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            MDC.clear();
        }
    }
}
