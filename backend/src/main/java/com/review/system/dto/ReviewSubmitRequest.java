package com.review.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ReviewSubmitRequest {
    @NotNull
    private Long assignmentId;
    private Map<String, Object> scores;
    private String opinion;
    private String conclusion;
    private boolean submit;
}
