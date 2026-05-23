package com.example.otpservice.controller;

import com.example.otpservice.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/otp")
public class OtpController {
    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(@RequestBody Map<String, String> request) {
        String otp = otpService.generateOtp(request.get("msisdn"));
        return ResponseEntity.ok(Map.of("success", true, "otp", otp, "msisdn", request.get("msisdn")));
    }
}
