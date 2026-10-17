package com.review.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreditRateRequest {
    @NotNull
    private Long submissionId;
    @NotNull
    @Min(1)
    @Max(5)
    private Double score;
    private String comment;
}
