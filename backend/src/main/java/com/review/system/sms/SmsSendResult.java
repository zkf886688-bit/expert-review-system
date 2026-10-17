package com.review.system.sms;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmsSendResult {
    private boolean success;
    private String provider;
    private String mode;
    private String message;
    private String requestId;
}
