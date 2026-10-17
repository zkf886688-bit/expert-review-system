package com.review.system.observability;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.ReviewAssignment;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.ReviewAssignmentMapper;
import com.review.system.mapper.ReviewTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewMetricsCollector {

    private final ReviewTaskMapper taskMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final AppMetrics appMetrics;

    @Scheduled(fixedDelayString = "${app.metrics.refresh-ms:60000}", initialDelay = 5000)
    public void refresh() {
        try {
            List<ReviewTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<>());
            List<ReviewAssignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<>());
            long inReview = tasks.stream().filter(t -> "IN_REVIEW".equals(t.getStatus())).count();
            long overdue = assignments.stream().filter(this::isOverdue).count();
            appMetrics.updateReviewSnapshot(tasks.size(), inReview, overdue);
        } catch (Exception e) {
            log.warn("Failed to refresh review metrics: {}", e.getMessage());
        }
    }

    private boolean isOverdue(ReviewAssignment assignment) {
        if ("SUBMITTED".equals(assignment.getStatus()) || "REJECTED".equals(assignment.getStatus())) {
            return false;
        }
        if ("OVERDUE".equals(assignment.getStatus())) {
            return true;
        }
        ReviewTask task = taskMapper.selectById(assignment.getTaskId());
        return task != null && task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now());
    }
}
