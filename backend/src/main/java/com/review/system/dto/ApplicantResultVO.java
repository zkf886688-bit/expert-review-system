package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ApplicantResultVO {
    private boolean available;
    private String message;
    private int submittedCount;
    private int minReviewers;
    private BigDecimal avgScore;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private String recommendation;
    private boolean meetsMinimum;
    private List<OpinionItem> opinions;

    @Data
    @Builder
    public static class OpinionItem {
        private String expertLabel;
        private BigDecimal totalScore;
        private String conclusion;
        private String opinion;
    }
}
