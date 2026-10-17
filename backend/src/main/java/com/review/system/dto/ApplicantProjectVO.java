package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApplicantProjectVO {
    private Long projectId;
    private Long taskId;
    private String projectName;
    private String organization;
    private String taskName;
    private String reviewType;
    private LocalDateTime deadline;
    private String taskStatus;
    private Integer minReviewers;
    private long submittedCount;
    private long acceptedCount;
    private boolean canUpload;
    private Integer reviewRound;
    private String rectificationStatus;
    private RectificationVO rectification;
    private List<MaterialItem> materials;
    private ApplicantResultVO result;

    @Data
    @Builder
    public static class MaterialItem {
        private Long id;
        private String fileName;
        private Long fileSize;
        private LocalDateTime createdAt;
    }
}
