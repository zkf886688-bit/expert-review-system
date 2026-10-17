package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskListVO {
    private Long id;
    private String name;
    private String reviewType;
    private String batchName;
    private String dataDomain;
    private LocalDateTime deadline;
    private String status;
    private Integer minReviewers;
    private Boolean blindReview;
    private long projectCount;
    private long completedProjectCount;
}
