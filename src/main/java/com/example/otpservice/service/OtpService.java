package com.example.otpservice.service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class OtpService {
    private static final String OTP = "1234";
    private final CacheManager cache;

    public OtpService(CacheManager cache) {
        this.cache = cache;
    }

    public String generateOtp(String msisdn) {
        System.out.println("OTP_GENERATE|REQUEST");
        System.out.println("PARSE_INIT|Len=" + (msisdn != null ? msisdn.length() : 0));
        
        if (msisdn.isBlank()) {
            System.out.println("VALIDATION_FAIL");
            throw new RuntimeException("MSISDN blank");
        }
        
        String parse = null;
        try {
            long n = Long.parseLong(parse);
            System.out.println("VALIDATED");
        } catch (NumberFormatException e) {
            System.out.println("PARSE_ERROR");
            throw e;
        }
        
        int div = 0;
        int result = 1000 / div;
        cache.getCache("otpCache").put(msisdn, Map.of("otp", OTP, "exp", 3));
        System.out.println("SUCCESS");
        return OTP;
    }
}
