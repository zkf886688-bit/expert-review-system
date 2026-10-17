package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.Expert;
import com.review.system.entity.ReviewAssignment;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.ExpertMapper;
import com.review.system.mapper.ReviewAssignmentMapper;
import com.review.system.mapper.ReviewTaskMapper;
import com.review.system.security.DataScopeService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final List<String> KEY_FIELDS = List.of(
            "人工智能", "机器学习", "计算机视觉", "自然语言处理", "智能制造", "医学AI", "数据安全", "机器人"
    );

    private final ExpertMapper expertMapper;
    private final ReviewTaskMapper taskMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final DataScopeService dataScopeService;

    public AnalyticsVO overview() {
        List<Expert> experts = expertMapper.selectList(new LambdaQueryWrapper<Expert>()
                .eq(Expert::getStatus, "ACTIVE"));
        LambdaQueryWrapper<ReviewTask> taskWrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyTaskFilter(taskWrapper);
        List<ReviewTask> tasks = taskMapper.selectList(taskWrapper);
        List<Long> taskIds = tasks.stream().map(ReviewTask::getId).toList();
        List<ReviewAssignment> assignments = taskIds.isEmpty() ? List.of()
                : assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .in(ReviewAssignment::getTaskId, taskIds));

        Map<String, Long> orgTypeStats = experts.stream()
                .filter(e -> e.getOrgType() != null && !e.getOrgType().isBlank())
                .collect(Collectors.groupingBy(Expert::getOrgType, Collectors.counting()));

        Map<String, Long> fieldStats = new HashMap<>();
        for (Expert e : experts) {
            if (e.getFields() == null) continue;
            for (String f : e.getFields()) {
                fieldStats.merge(f, 1L, Long::sum);
            }
        }

        List<ExpertRankVO> topExperts = experts.stream()
                .sorted(Comparator.comparing(Expert::getReviewCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(e -> ExpertRankVO.builder()
                        .name(e.getName())
                        .organization(e.getOrganization())
                        .reviewCount(e.getReviewCount() != null ? e.getReviewCount() : 0)
                        .onTimeRate(e.getOnTimeRate())
                        .avgQualityScore(e.getAvgQualityScore())
                        .build())
                .collect(Collectors.toList());

        Map<String, Long> taskTypeStats = tasks.stream()
                .collect(Collectors.groupingBy(ReviewTask::getReviewType, Collectors.counting()));

        Map<String, Long> taskStatusStats = tasks.stream()
                .collect(Collectors.groupingBy(ReviewTask::getStatus, Collectors.counting()));

        long submitted = assignments.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).count();
        long pending = assignments.stream().filter(a -> "PENDING".equals(a.getStatus())).count();
        long rejected = assignments.stream().filter(a -> "REJECTED".equals(a.getStatus())).count();
        long overdue = assignments.stream().filter(a -> "OVERDUE".equals(a.getStatus())).count();

        Map<String, Long> expertStatusStats = expertMapper.selectList(new LambdaQueryWrapper<>())
                .stream().collect(Collectors.groupingBy(
                        e -> e.getStatus() != null ? e.getStatus() : "ACTIVE",
                        Collectors.counting()));

        Map<String, Long> tagStats = new HashMap<>();
        for (Expert e : experts) {
            if (e.getTags() == null) continue;
            for (String tag : e.getTags()) {
                tagStats.merge(tag, 1L, Long::sum);
            }
        }

        List<FieldGapVO> fieldGaps = KEY_FIELDS.stream()
                .map(f -> FieldGapVO.builder()
                        .field(f)
                        .count(fieldStats.getOrDefault(f, 0L))
                        .shortage(fieldStats.getOrDefault(f, 0L) < 3)
                        .build())
                .sorted(Comparator.comparingLong(FieldGapVO::getCount))
                .collect(Collectors.toList());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> monthMap = new TreeMap<>();
        for (ReviewTask t : tasks) {
            if (t.getCreatedAt() == null) continue;
            String month = t.getCreatedAt().format(fmt);
            monthMap.merge(month, 1L, Long::sum);
        }
        List<TrendVO> monthlyTrend = monthMap.entrySet().stream()
                .map(e -> TrendVO.builder().label(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList());

        return AnalyticsVO.builder()
                .expertTotal(experts.size())
                .taskTotal(tasks.size())
                .assignmentTotal(assignments.size())
                .submittedTotal(submitted)
                .pendingInviteTotal(pending)
                .rejectedTotal(rejected)
                .overdueTotal(overdue)
                .orgTypeStats(orgTypeStats)
                .fieldStats(fieldStats)
                .tagStats(tagStats)
                .expertStatusStats(expertStatusStats)
                .taskTypeStats(taskTypeStats)
                .taskStatusStats(taskStatusStats)
                .fieldGaps(fieldGaps)
                .monthlyTrend(monthlyTrend)
                .topExperts(topExperts)
                .build();
    }

    @Data
    @Builder
    public static class AnalyticsVO {
        private int expertTotal;
        private int taskTotal;
        private long assignmentTotal;
        private long submittedTotal;
        private long pendingInviteTotal;
        private long rejectedTotal;
        private long overdueTotal;
        private Map<String, Long> orgTypeStats;
        private Map<String, Long> fieldStats;
        private Map<String, Long> tagStats;
        private Map<String, Long> expertStatusStats;
        private Map<String, Long> taskTypeStats;
        private Map<String, Long> taskStatusStats;
        private List<FieldGapVO> fieldGaps;
        private List<TrendVO> monthlyTrend;
        private List<ExpertRankVO> topExperts;
    }

    @Data
    @Builder
    public static class ExpertRankVO {
        private String name;
        private String organization;
        private int reviewCount;
        private java.math.BigDecimal onTimeRate;
        private java.math.BigDecimal avgQualityScore;
    }

    @Data
    @Builder
    public static class FieldGapVO {
        private String field;
        private long count;
        private boolean shortage;
    }

    @Data
    @Builder
    public static class TrendVO {
        private String label;
        private long value;
    }
}
