package com.review.system.dto;

import com.review.system.entity.ReviewMaterial;
import com.review.system.entity.ReviewTask;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExpertAssignmentVO {
    private Long assignmentId;
    private Long taskId;
    private String taskName;
    private String reviewType;
    private String deadline;
    private String assignmentStatus;
    private Long projectId;
    private String projectName;
    private String applicant;
    private String organization;
    private Boolean blindReview;
    private ReviewTask task;
    private List<ReviewMaterial> materials;
    private ReviewSubmissionVO submission;
}
