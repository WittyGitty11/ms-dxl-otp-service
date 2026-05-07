package com.example.otpservice.model;

import lombok.Data;

@Data
public class GenerateOtpRequest {

    // Deliberately NO @NotBlank — missing field bugs are caught manually in service
    private String msisdn;    // mobile number, must be numeric (deliberate bug: parsed as long)
    private String usecase;   // e.g. "LOGIN", "REGISTRATION", "PAYMENT"
    private String email;     // e.g. user@example.com
}