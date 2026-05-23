package com.example.otpservice.controller;

import com.example.otpservice.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
            String requestBody = objectMapper.writeValueAsString(request);
            System.out.println("System=ms-dxl-otp-service,CorrelationId=" + correlationId + ",Usecase=LOGIN,Channel=OTP-API,EventType=OTP_GENERATE|REQUEST_RECEIVED,Message=Incoming request body: " + requestBody);
            
            String otp = otpService.generateOtp(request.get("msisdn"));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("otp", otp);
            response.put("msisdn", request.get("msisdn"));
            
            String responseBody = objectMapper.writeValueAsString(response);
            System.out.println("System=ms-dxl-otp-service,CorrelationId=" + correlationId + ",Usecase=LOGIN,Channel=OTP-API,EventType=OTP_GENERATE|RESPONSE_SENT,Message=Outgoing response body: " + responseBody);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("System=ms-dxl-otp-service,CorrelationId=" + correlationId + ",Usecase=LOGIN,Channel=OTP-API,EventType=OTP_GENERATE|REQUEST_ERROR,Message=Error processing generate request: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
