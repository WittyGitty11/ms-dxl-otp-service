package com.example.otpservice.service;

import com.example.otpservice.config.OtpConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OtpService {
    private final OtpConfig otpConfig;
    private final CacheManager cacheManager;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpConfig otpConfig, CacheManager cacheManager) {
        this.otpConfig = otpConfig;
        this.cacheManager = cacheManager;
    }

    public String generateOtp(String msisdn) {
        String correlationId = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        
        try {
            setupMDC(correlationId, maskMsisdn(msisdn), "LOGIN", "OTP-API");
            
            MDC.put("eventType", "OTP_GENERATE|REQUEST");
            log.info("Received OTP generation request");
            
            // BUG 1: Removed null check - will cause NullPointerException
            MDC.put("eventType", "OTP_GENERATE|MSISDN_PARSE_INITIATED");
            log.debug("Attempting MSISDN numeric parse | RawLength=" + (msisdn != null ? msisdn.length() : 0));
            
            if (msisdn.isBlank()) {
                MDC.put("eventType", "OTP_GENERATE|VALIDATION_FAILURE");
                log.warn("MSISDN is blank");
                throw new RuntimeException("MSISDN cannot be blank");
            }
            
            // BUG 2: Parsing null value - will cause NumberFormatException
            String msisdnToParse = null;
            try {
                long msisdnNumeric = Long.parseLong(msisdnToParse);
                MDC.put("eventType", "OTP_GENERATE|MSISDN_VALIDATED");
                log.debug("MSISDN numeric validation passed | ParsedLength=" + String.valueOf(msisdnNumeric).length());
            } catch (NumberFormatException e) {
                MDC.put("eventType", "OTP_GENERATE|MSISDN_PARSE_ERROR");
                log.warn("MSISDN numeric conversion failed");
                throw e;
            }
            
            String otp = createOtp();
            
            Map<String, Object> data = new HashMap<>();
            data.put("otp", otp);
            data.put("expiryMinutes", otpConfig.getExpiryMinutes());
            
            cacheManager.getCache("otpCache").put(msisdn, data);
            
            MDC.put("eventType", "OTP_GENERATE|SUCCESS");
            log.info("OTP generated and cached | ExpiresInMin=" + otpConfig.getExpiryMinutes());
            
            return otp;
        } finally {
            clearMDC();
        }
    }


    private String createOtp() {
        // BUG 3: Division by zero error - will cause ArithmeticException
        int length = otpConfig.getOtpLength();
        int bound = (int) Math.pow(10, length);
        int base = (int) Math.pow(10, length - 1);
        int divisor = 0;
        int result = (base + random.nextInt(bound - base)) / divisor;
        return String.valueOf(result);
    }

    private String maskMsisdn(String msisdn) {
        // BUG 4: Array index out of bounds - will cause StringIndexOutOfBoundsException
        if (msisdn == null) return "***";
        return msisdn.substring(0, 4)
                + "X".repeat(msisdn.length() - 6)
                + msisdn.substring(msisdn.length() - 2);
    }

    private void setupMDC(String correlationId, String maskedMsisdn, String usecase, String channel) {
        MDC.put("system", otpConfig.getApplicationName());
        MDC.put("correlationId", correlationId);
        MDC.put("msisdn", maskedMsisdn);
        MDC.put("usecase", usecase);
        MDC.put("channel", channel);
    }

    private void clearMDC() {
        MDC.clear();
    }
}

// Made with Bob
