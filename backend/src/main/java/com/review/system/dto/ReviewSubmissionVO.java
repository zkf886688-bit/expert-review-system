package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class ReviewSubmissionVO {
    private Long id;
    private Map<String, Object> scores;
    private BigDecimal totalScore;
    private String opinion;
    private String conclusion;
    private String status;
    private String attachmentName;
    private Boolean hasAttachment;
}
