package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.Expert;
import com.review.system.entity.ReviewAssignment;
import com.review.system.mapper.ExpertMapper;
import com.review.system.mapper.ReviewAssignmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpertInactiveService {

    private final ExpertMapper expertMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final AuditService auditService;

    @Value("${app.expert.inactive-enabled:true}")
    private boolean enabled;

    @Value("${app.expert.inactive-years:3}")
    private int inactiveYears;

    @Transactional
    public int markInactiveExperts() {
        if (!enabled || inactiveYears <= 0) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusYears(inactiveYears);
        Map<Long, LocalDateTime> lastActivityByExpert = loadLastActivityByExpert();
        List<Expert> activeExperts = expertMapper.selectList(new LambdaQueryWrapper<Expert>()
                .eq(Expert::getStatus, "ACTIVE"));

        int count = 0;
        for (Expert expert : activeExperts) {
            LocalDateTime lastActivity = lastActivityByExpert.get(expert.getId());
            if (lastActivity == null) {
                lastActivity = expert.getCreatedAt() != null ? expert.getCreatedAt() : expert.getUpdatedAt();
            }
            if (lastActivity == null || !lastActivity.isBefore(cutoff)) {
                continue;
            }
            expert.setStatus("DISABLED");
            expertMapper.updateById(expert);
            auditService.log("AUTO_DISABLE_EXPERT", "EXPERT", expert.getId(),
                    "超过 " + inactiveYears + " 年未参与评审，系统自动停用（末次活动: " + lastActivity + "）");
            count++;
        }
        return count;
    }

    private Map<Long, LocalDateTime> loadLastActivityByExpert() {
        Map<Long, LocalDateTime> map = new HashMap<>();
        for (ReviewAssignment assignment : assignmentMapper.selectList(new LambdaQueryWrapper<>())) {
            LocalDateTime activity = latestActivity(assignment);
            if (activity == null) {
                continue;
            }
            map.merge(assignment.getExpertId(), activity, ExpertInactiveService::later);
        }
        return map;
    }

    static LocalDateTime latestActivity(ReviewAssignment assignment) {
        LocalDateTime latest = assignment.getInvitedAt();
        if (assignment.getRespondedAt() != null
                && (latest == null || assignment.getRespondedAt().isAfter(latest))) {
            latest = assignment.getRespondedAt();
        }
        if (assignment.getSubmittedAt() != null
                && (latest == null || assignment.getSubmittedAt().isAfter(latest))) {
            latest = assignment.getSubmittedAt();
        }
        return latest;
    }

    private static LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}
