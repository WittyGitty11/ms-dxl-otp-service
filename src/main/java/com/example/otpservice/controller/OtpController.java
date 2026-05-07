package com.example.otpservice.controller;

import com.example.otpservice.model.ApiResponse;
import com.example.otpservice.model.GenerateOtpRequest;
import com.example.otpservice.model.VerifyOtpRequest;
import com.example.otpservice.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generate(
            @RequestBody GenerateOtpRequest request) {

        String otp = otpService.generateOtp(request);

        Map<String, String> data = Map.of(
                "msisdn",  request.getMsisdn(),
                "usecase", request.getUsecase(),
                "email",   request.getEmail(),
                "otp",     otp   // DEMO ONLY — remove in production
        );

        return ResponseEntity.ok(
                ApiResponse.success("OTP generated successfully. Valid for 5 minutes.", data));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @RequestBody VerifyOtpRequest request) {

        otpService.verifyOtp(request.getMsisdn(), request.getOtp());

        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully!", null));
    }

    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<Map<String, String>>> resend(
            @RequestBody GenerateOtpRequest request) {

        String otp = otpService.resendOtp(request);

        Map<String, String> data = Map.of(
                "msisdn",  request.getMsisdn(),
                "usecase", request.getUsecase(),
                "otp",     otp   // DEMO ONLY
        );

        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully.", data));
    }
}