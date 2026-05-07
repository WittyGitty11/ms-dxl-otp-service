package com.example.otpservice.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "MSISDN must not be blank")
    private String msisdn;

    @NotBlank(message = "OTP must not be blank")
    private String otp;
}