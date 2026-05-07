package com.example.otpservice.service;

import com.example.otpservice.config.OtpConfig;
import com.example.otpservice.exception.OtpException;
import com.example.otpservice.model.GenerateOtpRequest;
import com.example.otpservice.model.OtpDetails;
import com.example.otpservice.model.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String CACHE_NAME = "otpCache";
    private static final String SYSTEM     = "OTP-SVC";

    private final OtpConfig    otpConfig;
    private final CacheManager cacheManager;
    private final SecureRandom secureRandom = new SecureRandom();

    // ─────────────────────────────────────────────────────────────────
    // GENERATE
    // ─────────────────────────────────────────────────────────────────

    public String generateOtp(GenerateOtpRequest request) {

        String correlationId = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        RequestContext ctx = RequestContext.builder()
                .correlationId(correlationId)
                .msisdn(maskMsisdn(request.getMsisdn()))
                .usecase(request.getUsecase())
                .channel("OTP-API")
                .reqMethod("POST")
                .thread(Thread.currentThread().getName())
                .build();

        logInfo(ctx, "OTP_GENERATE", "REQUEST", "Received OTP generation request");

        // BUG 1: Manual mandatory-field check — no Bean Validation annotation used.
        // Exception message is intentionally vague (no field name disclosed).
        // Log also omits the exact missing field — requires code + log correlation to debug.
        validateMandatoryFields(request, ctx);

        // BUG 2: MSISDN parsed as long — NumberFormatException on alphanumeric input.
        // No try-catch here; exception escapes raw to GlobalExceptionHandler.
        // The DEBUG log "MSISDN_PARSE_INITIATED" appears but no follow-up log exists
        // when the parse fails — you must notice the missing SUCCESS log to find the bug.
        long msisdnNumeric = parseMsisdnAsLong(request.getMsisdn(), ctx);

        logDebug(ctx, "OTP_GENERATE", "MSISDN_VALIDATED",
                "MSISDN numeric validation passed | ParsedLength=" + String.valueOf(msisdnNumeric).length());

        String otp = createOtp();

        OtpDetails details = OtpDetails.builder()
                .msisdn(request.getMsisdn())
                .usecase(request.getUsecase())
                .email(request.getEmail())
                .otp(otp)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(otpConfig.getExpiryMinutes()))
                .attemptCount(0)
                .verified(false)
                .build();

        storeOtp(request.getMsisdn(), details);

        logInfo(ctx, "OTP_GENERATE", "SUCCESS",
                "OTP generated and cached | ExpiresInMin=" + otpConfig.getExpiryMinutes()
                        + " | Usecase=" + request.getUsecase()
                        + " | Email=" + request.getEmail());

        return otp;
    }

    // ─────────────────────────────────────────────────────────────────
    // VERIFY
    // ─────────────────────────────────────────────────────────────────

    public void verifyOtp(String msisdn, String otp) {

        String correlationId = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        RequestContext ctx = RequestContext.builder()
                .correlationId(correlationId)
                .msisdn(maskMsisdn(msisdn))
                .channel("OTP-API")
                .reqMethod("POST")
                .thread(Thread.currentThread().getName())
                .build();

        logInfo(ctx, "OTP_VERIFY", "REQUEST", "Received OTP verification request");

        OtpDetails details = getOtpDetails(msisdn, ctx);

        if (details.isExpired()) {
            evictOtp(msisdn);
            logWarn(ctx, "OTP_VERIFY", "EXPIRED",
                    "OTP has expired | Usecase=" + details.getUsecase());
            throw new OtpException("OTP has expired. Please request a new one.");
        }

        if (details.getAttemptCount() >= otpConfig.getMaxAttempts()) {
            evictOtp(msisdn);
            logWarn(ctx, "OTP_VERIFY", "MAX_ATTEMPTS",
                    "Max attempts breached | AttemptCount=" + details.getAttemptCount()
                            + " | MaxAllowed=" + otpConfig.getMaxAttempts());
            throw new OtpException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        details.setAttemptCount(details.getAttemptCount() + 1);
        storeOtp(msisdn, details);

        if (!details.getOtp().equals(otp)) {
            int remaining = otpConfig.getMaxAttempts() - details.getAttemptCount();
            logWarn(ctx, "OTP_VERIFY", "INVALID_OTP",
                    "OTP mismatch | AttemptCount=" + details.getAttemptCount()
                            + " | AttemptsRemaining=" + remaining);
            throw new OtpException("Invalid OTP. " + remaining + " attempt(s) remaining.");
        }

        details.setVerified(true);
        evictOtp(msisdn);

        logInfo(ctx, "OTP_VERIFY", "SUCCESS",
                "OTP verified successfully | Usecase=" + details.getUsecase()
                        + " | Email=" + details.getEmail());
    }

    // ─────────────────────────────────────────────────────────────────
    // RESEND
    // ─────────────────────────────────────────────────────────────────

    public String resendOtp(GenerateOtpRequest request) {
        evictOtp(request.getMsisdn());
        return generateOtp(request);
    }

    // ─────────────────────────────────────────────────────────────────
    // BUG 1 — Manual validation, vague error, field name hidden in code
    // ─────────────────────────────────────────────────────────────────

    private void validateMandatoryFields(GenerateOtpRequest request, RequestContext ctx) {
        if (request.getMsisdn() == null || request.getMsisdn().isBlank()
                || request.getUsecase() == null || request.getUsecase().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()) {

            // Intentionally logs only masked MSISDN and usecase — not which field is missing
            logWarn(ctx, "OTP_GENERATE", "VALIDATION_FAILURE",
                    "Mandatory request parameter missing | Usecase=" + request.getUsecase()
                            + " | MSISDN=" + maskMsisdn(request.getMsisdn()));

            throw new OtpException("Mandatory request parameter missing in OTP generate request.");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // BUG 2 — Raw Long.parseLong, no try-catch, escapes as NFE
    // ─────────────────────────────────────────────────────────────────

    private long parseMsisdnAsLong(String msisdn, RequestContext ctx) {
        logDebug(ctx, "OTP_GENERATE", "MSISDN_PARSE_INITIATED",
                "Attempting MSISDN numeric parse | RawLength=" + (msisdn != null ? msisdn.length() : 0));

        // No try-catch — alphanumeric MSISDN causes NumberFormatException to escape raw.
        // The next log line (MSISDN_VALIDATED) will be ABSENT in logs on failure,
        // which is the only clue in logs. Root cause requires reading this code.
        return Long.parseLong(msisdn);
    }

    // ─────────────────────────────────────────────────────────────────
    // Cache helpers
    // ─────────────────────────────────────────────────────────────────

    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) throw new IllegalStateException("Cache '" + CACHE_NAME + "' not configured");
        return cache;
    }

    private void storeOtp(String msisdn, OtpDetails details) {
        getCache().put(msisdn, details);
    }

    private OtpDetails getOtpDetails(String msisdn, RequestContext ctx) {
        Cache.ValueWrapper wrapper = getCache().get(msisdn);
        if (wrapper == null || wrapper.get() == null) {
            logWarn(ctx, "OTP_VERIFY", "OTP_NOT_FOUND",
                    "No active OTP found in cache | MSISDN=" + maskMsisdn(msisdn));
            throw new OtpException("No OTP found for this MSISDN. Please request a new OTP.");
        }
        return (OtpDetails) wrapper.get();
    }

    private void evictOtp(String msisdn) {
        getCache().evict(msisdn);
    }

    // ─────────────────────────────────────────────────────────────────
    // OTP generator
    // ─────────────────────────────────────────────────────────────────

    private String createOtp() {
        int length = otpConfig.getOtpLength();
        int bound  = (int) Math.pow(10, length);
        int base   = (int) Math.pow(10, length - 1);
        return String.valueOf(base + secureRandom.nextInt(bound - base));
    }

    // ─────────────────────────────────────────────────────────────────
    // Structured log helpers
    // ─────────────────────────────────────────────────────────────────

    private String buildLogLine(RequestContext ctx, String eventType, String status, String msg) {
        return String.format(
                "System=%s,Thread=%s,CorrelationId=%s,MSISDN=%s,Usecase=%s,Channel=%s,EventType=%s|%s,Message=%s",
                SYSTEM,
                ctx.getThread(),
                ctx.getCorrelationId(),
                ctx.getMsisdn() != null ? ctx.getMsisdn() : "-",
                ctx.getUsecase()  != null ? ctx.getUsecase()  : "-",
                ctx.getChannel()  != null ? ctx.getChannel()  : "-",
                eventType, status,
                msg
        );
    }

    private void logInfo(RequestContext ctx, String eventType, String status, String msg) {
        log.info(buildLogLine(ctx, eventType, status, msg));
    }

    private void logDebug(RequestContext ctx, String eventType, String status, String msg) {
        log.debug(buildLogLine(ctx, eventType, status, msg));
    }

    private void logWarn(RequestContext ctx, String eventType, String status, String msg) {
        log.warn(buildLogLine(ctx, eventType, status, msg));
    }

    // ─────────────────────────────────────────────────────────────────
    // Masking utility
    // ─────────────────────────────────────────────────────────────────

    private String maskMsisdn(String msisdn) {
        if (msisdn == null || msisdn.length() < 6) return "***";
        // Keep first 4 and last 2 chars, mask the rest: 9876XXXXX10
        return msisdn.substring(0, 4)
                + "X".repeat(msisdn.length() - 6)
                + msisdn.substring(msisdn.length() - 2);
    }
}