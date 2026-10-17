package com.review.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteResponseRequest {
    @NotNull
    private Long assignmentId;
    private boolean accept;
    private String reason;
}
