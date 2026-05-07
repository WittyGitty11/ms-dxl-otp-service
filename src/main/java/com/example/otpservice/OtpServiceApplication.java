package com.example.otpservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class OtpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtpServiceApplication.class, args);
    }
}
