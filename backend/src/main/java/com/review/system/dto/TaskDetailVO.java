package com.review.system.dto;

import com.review.system.entity.ReviewMaterial;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewTask;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TaskDetailVO {
    private ReviewTask task;
    private List<ReviewProject> projects;
    private List<ReviewMaterial> materials;
    private List<AssignmentVO> assignments;
    private List<ProjectStatVO> projectStats;
    private AssignSummaryVO assignSummary;

    @Data
    @Builder
    public static class ProjectStatVO {
        private Long projectId;
        private int assignedCount;
        private int submittedCount;
        private int minReviewers;
        private boolean completed;
    }

    @Data
    @Builder
    public static class AssignSummaryVO {
        private int totalProjects;
        private int completedProjects;
        private int totalAssignments;
        private int submittedAssignments;
    }

    @Data
    @Builder
    public static class AssignmentVO {
        private Long id;
        private Long projectId;
        private String projectName;
        private Long expertId;
        private String expertName;
        private String status;
        private String rejectReason;
    }
}
