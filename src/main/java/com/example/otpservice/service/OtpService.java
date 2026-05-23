package com.example.otpservice.service;

import com.example.otpservice.config.OtpConfig;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        String maskedMsisdn = maskMsisdn(msisdn);
        
        System.out.println("System=" + otpConfig.getApplicationName() + ",CorrelationId=" + correlationId + ",MSISDN=" + maskedMsisdn + ",Usecase=LOGIN,Channel=OTP-API,EventType=OTP_GENERATE|REQUEST,Message=Received OTP generation request");
        
        System.out.println("System=" + otpConfig.getApplicationName() + ",CorrelationId=" + correlationId + ",MSISDN=" + maskedMsisdn + ",Usecase=LOGIN,Channel=OTP-API,EventType=OTP_GENERATE|MSISDN_PARSE_INITIATED,Message=Attempting MSISDN numeric parse | RawLength=" + (msisdn != null ? msisdn.length() : 0));
        
        if (msisdn.isBlank()) {
            System.out.println("System=" + otpConfig.getApplicationName() + ",CorrelationId=" + correlationId + ",MSISDN=" + maskedMsisdn + ",Usecase=LOGIN,Channel=OTP-API,EventType=OTP_GENERATE|VALIDATION_FAILURE,Message=MSISDN is blank");
            throw new RuntimeException("MSISDN cannot be blank");
        }
        
        String msisdnToParse = null;
        try {
            long msisdnNumeric = Long.parseLong(msisdnToParse);
            System.out.println("System=" + otpConfig.getApplicationName() + ",CorrelationId=" + correlationId + ",MSISDN=" + maskedMsisdn + ",Usecase=LOGIN,Channel=OTP-API,EventType=OTP_GENERATE|MSISDN_VALIDATED,Message=MSISDN numeric validation passed | ParsedLength=" + String.valueOf(msisdnNumeric).length());
        } catch (NumberFormatException e) {
            System.out.println("System=" + otpConfig.getApplicationName() + ",CorrelationId=" + correlationId + ",MSISDN=" + maskedMsisdn + ",Usecase=LOGIN,Channel=OTP-API,EventType=OTP_GENERATE|MSISDN_PARSE_ERROR,Message=MSISDN numeric conversion failed");
            throw e;
        }
        
        String otp = createOtp();
        
        Map<String, Object> data = new HashMap<>();
        data.put("otp", otp);
        data.put("expiryMinutes", otpConfig.getExpiryMinutes());
        
        cacheManager.getCache("otpCache").put(msisdn, data);
        
        System.out.println("System=" + otpConfig.getApplicationName() + ",CorrelationId=" + correlationId + ",MSISDN=" + maskedMsisdn + ",Usecase=LOGIN,Channel=OTP-API,EventType=OTP_GENERATE|SUCCESS,Message=OTP generated and cached | ExpiresInMin=" + otpConfig.getExpiryMinutes());
        
        return otp;
    }

    private String createOtp() {
        int length = otpConfig.getOtpLength();
        int bound = (int) Math.pow(10, length);
        int base = (int) Math.pow(10, length - 1);
        int divisor = 0;
        int result = (base + random.nextInt(bound - base)) / divisor;
        return String.valueOf(result);
    }

    private String maskMsisdn(String msisdn) {
        if (msisdn == null) return "***";
        return msisdn.substring(0, 4) + "X".repeat(msisdn.length() - 6) + msisdn.substring(msisdn.length() - 2);
    }
}
