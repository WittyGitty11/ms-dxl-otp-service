package com.example.otpservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

@Configuration
public class OtpConfig {

    // OTP expiry in minutes (default: 5)
    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    // Maximum allowed verify attempts before lockout
    @Value("${otp.max-attempts:3}")
    private int maxAttempts;

    // OTP length (default: 6 digits)
    @Value("${otp.length:6}")
    private int otpLength;

    public int getExpiryMinutes() { return expiryMinutes; }
    public int getMaxAttempts()   { return maxAttempts; }
    public int getOtpLength()     { return otpLength; }

    /**
     * In-memory cache that automatically evicts OTP entries after expiry.
     * Replace with Redis CacheManager in production for distributed setups.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("otpCache");
        manager.setCaffeine(
            Caffeine.newBuilder()
                    .expireAfterWrite(expiryMinutes, TimeUnit.MINUTES)
                    .maximumSize(10_000)
        );
        return manager;
    }
}
