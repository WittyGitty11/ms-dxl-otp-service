package com.example.otpservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class OtpConfig {
    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager("otpCache");
        mgr.setCaffeine(Caffeine.newBuilder().expireAfterWrite(expiryMinutes, TimeUnit.MINUTES).maximumSize(10000));
        return mgr;
    }

    public int getExpiryMinutes() { return expiryMinutes; }
    public int getOtpLength() { return otpLength; }
}
