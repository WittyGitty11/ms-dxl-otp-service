# OTP Service

Simple Spring Boot OTP generation and verification service.

## Features
- Generate 6-digit OTP
- Verify OTP
- In-memory cache (5 min expiry)

## Tech Stack
- Spring Boot 3.2
- Caffeine Cache
- Java 17

## Run
```bash
mvn spring-boot:run
```

## API

### Generate OTP
```bash
POST /api/v1/otp/generate
{
  "msisdn": "9876543210"
}
```

### Verify OTP
```bash
POST /api/v1/otp/verify
{
  "msisdn": "9876543210",
  "otp": "123456"
}
```

## Configuration
Edit `application.properties`:
```properties
otp.expiry-minutes=5
otp.length=6
