package com.example.otpservice;

import com.example.otpservice.config.OtpConfig;
import com.example.otpservice.exception.OtpException;
import com.example.otpservice.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OtpServiceTest {

    @Autowired
    private OtpService otpService;

    @Autowired
    private OtpConfig otpConfig;

    private static final String IDENTIFIER = "test@example.com";

    @Test
    void shouldGenerateOtpOfCorrectLength() {
        String otp = otpService.generateOtp(IDENTIFIER);
        assertNotNull(otp);
        assertEquals(otpConfig.getOtpLength(), otp.length());
    }

    @Test
    void shouldVerifyCorrectOtp() {
        String otp = otpService.generateOtp(IDENTIFIER);
        assertDoesNotThrow(() -> otpService.verifyOtp(IDENTIFIER, otp));
    }

    @Test
    void shouldThrowOnInvalidOtp() {
        otpService.generateOtp(IDENTIFIER);
        OtpException ex = assertThrows(OtpException.class,
                () -> otpService.verifyOtp(IDENTIFIER, "000000"));
        assertTrue(ex.getMessage().contains("Invalid OTP"));
    }

    @Test
    void shouldThrowWhenOtpNotFound() {
        assertThrows(OtpException.class,
                () -> otpService.verifyOtp("nonexistent@example.com", "123456"));
    }

    @Test
    void shouldGenerateNewOtpOnResend() {
        String first  = otpService.generateOtp(IDENTIFIER);
        String second = otpService.resendOtp(IDENTIFIER);
        // Both are valid OTPs; second should be freshly stored
        assertNotNull(second);
        assertEquals(otpConfig.getOtpLength(), second.length());
    }
}
