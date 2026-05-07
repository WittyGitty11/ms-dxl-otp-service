package com.example.otpservice.model;

import lombok.Builder;
import lombok.Data;

/**
 * Carries per-request metadata threaded through the call stack
 * and stamped into every structured log line.
 */
@Data
@Builder
public class RequestContext {
    private String correlationId;
    private String msisdn;        // masked in logs: 9876XXXXX3210
    private String usecase;
    private String channel;
    private String requestUrl;
    private String reqMethod;
    private String thread;
}