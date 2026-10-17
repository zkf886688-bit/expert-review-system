package com.review.system.mail;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailSendResult {
    private boolean success;
    private String mode;
    private String message;
}
