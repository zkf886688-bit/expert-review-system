package com.review.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TaskCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String reviewType;
    private String batchName;
    private String dataDomain;
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime deadline;
    private Integer minReviewers;
    private Boolean blindReview;
    private Boolean avoidSameOrg;
    private Boolean avoidDuplicateExpert;
    private List<Map<String, Object>> formConfig;
    private List<ProjectItem> projects;

    @Data
    public static class ProjectItem {
        @NotBlank
        private String projectName;
        private String applicant;
        private String organization;
        private Long applicantUserId;
    }
}
