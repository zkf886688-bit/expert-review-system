package com.review.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.ReviewAssignment;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.ReviewAssignmentMapper;
import com.review.system.mapper.ReviewTaskMapper;
import com.review.system.service.ExpertCreditService;
import com.review.system.service.NotificationService;
import com.review.system.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineReminderScheduler {

    private final ReviewTaskMapper taskMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;
    private final ExpertCreditService expertCreditService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void remindUpcomingDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inThreeDays = now.plusDays(3);
        List<ReviewTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<ReviewTask>()
                .eq(ReviewTask::getStatus, "IN_REVIEW")
                .between(ReviewTask::getDeadline, now, inThreeDays));
        for (ReviewTask task : tasks) {
            long days = ChronoUnit.DAYS.between(now.toLocalDate(), task.getDeadline().toLocalDate());
            List<ReviewAssignment> pending = assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                    .eq(ReviewAssignment::getTaskId, task.getId())
                    .in(ReviewAssignment::getStatus, "PENDING", "ACCEPTED"));
            for (ReviewAssignment a : pending) {
                Map<String, String> vars = Map.of(
                        "taskName", task.getName(),
                        "days", String.valueOf(days));
                notificationService.notifyExpert(a.getExpertId(),
                        notificationTemplateService.deadlineTitle(vars),
                        notificationTemplateService.deadlineContent(vars),
                        "DEADLINE", a.getId());
            }
        }
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void markOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<ReviewTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<ReviewTask>()
                .lt(ReviewTask::getDeadline, now)
                .in(ReviewTask::getStatus, "IN_REVIEW", "UNASSIGNED"));
        for (ReviewTask task : tasks) {
            List<ReviewAssignment> list = assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                    .eq(ReviewAssignment::getTaskId, task.getId())
                    .in(ReviewAssignment::getStatus, "PENDING", "ACCEPTED"));
            for (ReviewAssignment a : list) {
                a.setStatus("OVERDUE");
                assignmentMapper.updateById(a);
                expertCreditService.recordOverdue(a.getExpertId(), a.getId());
                Map<String, String> vars = Map.of("taskName", task.getName());
                notificationService.notifyExpert(a.getExpertId(),
                        notificationTemplateService.overdueTitle(vars),
                        notificationTemplateService.overdueContent(vars),
                        "OVERDUE", a.getId());
            }
        }
    }
}
