package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchRandomAssignResultVO {
    private int processedProjects;
    private int skippedProjects;
    private int totalAssigned;
    private List<ProjectResult> details;
    private List<String> errors;

    @Data
    @Builder
    public static class ProjectResult {
        private Long projectId;
        private String projectName;
        private List<Long> expertIds;
        private List<String> expertNames;
    }
}
