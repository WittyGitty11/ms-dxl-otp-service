# 🔐 OTP Service — Spring Boot

A clean, production-ready **One-Time Password (OTP)** microservice built with Spring Boot 3.

---

## ✨ Features

- Generate a 6-digit (configurable) secure OTP
- Verify OTP with attempt-limit protection
- Resend OTP (invalidates old one)
- Auto-expiry using Caffeine in-memory cache
- Global exception handling with structured JSON responses
- Fully configurable via `application.properties`

---

## 🏗️ Tech Stack

| Layer      | Technology                     |
|------------|--------------------------------|
| Framework  | Spring Boot 3.2                |
| Cache      | Caffeine (in-memory)           |
| Validation | Jakarta Bean Validation        |
| Build      | Maven                          |
| Java       | 17+                            |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Run the app

```bash
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

---

## 📡 API Endpoints

### 1. Generate OTP

```http
POST /api/v1/otp/generate
Content-Type: application/json

{
  "identifier": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP generated successfully. Valid for 5 minutes.",
  "data": {
    "identifier": "user@example.com",
    "otp": "482910"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

> ⚠️ **Production Note:** Remove the `otp` field from the response and send it via SMS/Email instead.

---

### 2. Verify OTP

```http
POST /api/v1/otp/verify
Content-Type: application/json

{
  "identifier": "user@example.com",
  "otp": "482910"
}
```

**Response (success):**
```json
{
  "success": true,
  "message": "OTP verified successfully!",
  "timestamp": "2024-01-15T10:31:00"
}
```

**Response (failure):**
```json
{
  "success": false,
  "message": "Invalid OTP. 2 attempt(s) remaining.",
  "timestamp": "2024-01-15T10:31:00"
}
```

---

### 3. Resend OTP

```http
POST /api/v1/otp/resend
Content-Type: application/json

{
  "identifier": "user@example.com"
}
```

---

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
otp.expiry-minutes=5    # OTP validity window
otp.max-attempts=3      # Max wrong attempts before lockout
otp.length=6            # OTP digit length
```

---

## 🧪 Running Tests

```bash
mvn test
```

---

## 📁 Project Structure

```
src/
├── main/java/com/example/otpservice/
│   ├── OtpServiceApplication.java
│   ├── config/
│   │   └── OtpConfig.java          # Cache + property config
│   ├── controller/
│   │   └── OtpController.java      # REST endpoints
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── OtpException.java
│   ├── model/
│   │   ├── ApiResponse.java
│   │   ├── GenerateOtpRequest.java
│   │   ├── OtpDetails.java
│   │   └── VerifyOtpRequest.java
│   └── service/
│       └── OtpService.java         # Core OTP logic
└── test/
    └── OtpServiceTest.java
```

---

## 🔒 Production Checklist

- [ ] Remove OTP from API response — send via SMS/Email only
- [ ] Swap Caffeine cache with Redis for distributed deployments
- [ ] Add rate limiting (e.g. Bucket4j or API Gateway)
- [ ] Use HTTPS in production
- [ ] Add authentication (JWT/OAuth2) to protect endpoints

---

## 📄 License

MIT
