package com.example.otpservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpDetails {

    private String msisdn;
    private String usecase;
    private String email;
    private String otp;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int attemptCount;
    private boolean verified;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}