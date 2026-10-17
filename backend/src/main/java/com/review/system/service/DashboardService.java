package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.ReviewAssignment;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.ReviewAssignmentMapper;
import com.review.system.mapper.ReviewProjectMapper;
import com.review.system.mapper.ReviewTaskMapper;
import com.review.system.security.DataScopeService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ReviewTaskMapper taskMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewProjectMapper projectMapper;
    private final DataScopeService dataScopeService;

    public DashboardVO overview() {
        LambdaQueryWrapper<ReviewTask> taskWrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyTaskFilter(taskWrapper);
        List<ReviewTask> tasks = taskMapper.selectList(taskWrapper);
        List<Long> taskIds = tasks.stream().map(ReviewTask::getId).toList();
        List<ReviewAssignment> assignments = taskIds.isEmpty() ? List.of()
                : assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .in(ReviewAssignment::getTaskId, taskIds));
        long totalTasks = tasks.size();
        long inReview = tasks.stream().filter(t -> "IN_REVIEW".equals(t.getStatus())).count();
        long summarized = tasks.stream().filter(t -> "SUMMARIZED".equals(t.getStatus())).count();
        long unassigned = tasks.stream().filter(t -> "UNASSIGNED".equals(t.getStatus())).count();
        long published = tasks.stream().filter(t -> "PUBLISHED".equals(t.getStatus())).count();
        long pendingSummary = tasks.stream()
                .filter(t -> "IN_REVIEW".equals(t.getStatus()) || "PUBLISHED".equals(t.getStatus()))
                .count();
        long pending = assignments.stream().filter(a -> "PENDING".equals(a.getStatus())).count();
        long accepted = assignments.stream().filter(a -> "ACCEPTED".equals(a.getStatus())).count();
        long submitted = assignments.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).count();
        long overdue = assignments.stream().filter(a -> "OVERDUE".equals(a.getStatus())).count();
        long overdueAssignments = assignments.stream().filter(a -> {
            if ("SUBMITTED".equals(a.getStatus())) return false;
            ReviewTask task = taskMapper.selectById(a.getTaskId());
            return task != null && task.getDeadline().isBefore(LocalDateTime.now());
        }).count();

        return DashboardVO.builder()
                .totalTasks(totalTasks)
                .unassignedTasks(unassigned)
                .publishedTasks(published)
                .pendingSummaryTasks(pendingSummary)
                .inReviewTasks(inReview)
                .summarizedTasks(summarized)
                .pendingConfirm(pending)
                .reviewing(accepted)
                .submitted(submitted)
                .overdue(overdue + overdueAssignments)
                .build();
    }

    public List<TaskProgressVO> taskProgress() {
        LambdaQueryWrapper<ReviewTask> wrapper = new LambdaQueryWrapper<ReviewTask>()
                .orderByDesc(ReviewTask::getCreatedAt);
        dataScopeService.applyTaskFilter(wrapper);
        return taskMapper.selectList(wrapper)
                .stream()
                .map(task -> {
                    List<ReviewAssignment> list = assignmentMapper.selectList(
                            new LambdaQueryWrapper<ReviewAssignment>()
                                    .eq(ReviewAssignment::getTaskId, task.getId()));
                    long total = list.size();
                    long done = list.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).count();
                    long projectCount = projectMapper.selectCount(new LambdaQueryWrapper<ReviewProject>()
                            .eq(ReviewProject::getTaskId, task.getId()));
                    int min = task.getMinReviewers() != null ? task.getMinReviewers() : 3;
                    List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                            .eq(ReviewProject::getTaskId, task.getId()));
                    Map<Long, Long> submittedByProject = list.stream()
                            .filter(a -> "SUBMITTED".equals(a.getStatus()))
                            .collect(java.util.stream.Collectors.groupingBy(ReviewAssignment::getProjectId,
                                    java.util.stream.Collectors.counting()));
                    long completedProjects = projects.stream()
                            .filter(p -> submittedByProject.getOrDefault(p.getId(), 0L) >= min)
                            .count();
                    return TaskProgressVO.builder()
                            .taskId(task.getId())
                            .taskName(task.getName())
                            .status(task.getStatus())
                            .deadline(task.getDeadline())
                            .totalAssignments(total)
                            .submittedCount(done)
                            .projectCount(projectCount)
                            .completedProjectCount(completedProjects)
                            .build();
                })
                .toList();
    }

    @Data
    @Builder
    public static class DashboardVO {
        private long totalTasks;
        private long unassignedTasks;
        private long publishedTasks;
        private long pendingSummaryTasks;
        private long inReviewTasks;
        private long summarizedTasks;
        private long pendingConfirm;
        private long reviewing;
        private long submitted;
        private long overdue;
    }

    @Data
    @Builder
    public static class TaskProgressVO {
        private Long taskId;
        private String taskName;
        private String status;
        private LocalDateTime deadline;
        private long totalAssignments;
        private long submittedCount;
        private long projectCount;
        private long completedProjectCount;
    }
}
