package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.Expert;
import com.review.system.entity.ReviewAssignment;
import com.review.system.mapper.ReviewAssignmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExpertAssignmentScorer {

    private final ReviewAssignmentMapper assignmentMapper;

    public List<Long> pickWeighted(List<Expert> pool, int count) {
        if (pool.size() <= count) {
            return pool.stream().map(Expert::getId).toList();
        }
        Map<Long, Long> activeLoads = loadActiveAssignmentCounts(pool);
        Map<Long, Long> rejectCounts = loadRejectCounts(pool);
        List<Scored> scored = pool.stream()
                .map(e -> new Scored(e.getId(), score(e, activeLoads, rejectCounts)))
                .toList();
        List<Long> picked = new ArrayList<>();
        List<Scored> remaining = new ArrayList<>(scored);
        Random random = new Random();
        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            double total = remaining.stream().mapToDouble(Scored::weight).sum();
            double r = random.nextDouble() * total;
            double acc = 0;
            Scored chosen = remaining.get(remaining.size() - 1);
            for (Scored s : remaining) {
                acc += s.weight();
                if (r <= acc) {
                    chosen = s;
                    break;
                }
            }
            picked.add(chosen.expertId());
            final long chosenId = chosen.expertId();
            remaining.removeIf(s -> s.expertId() == chosenId);
        }
        return picked;
    }

    private double score(Expert expert, Map<Long, Long> activeLoads, Map<Long, Long> rejectCounts) {
        double score = 100.0;
        score -= activeLoads.getOrDefault(expert.getId(), 0L) * 12.0;
        score -= rejectCounts.getOrDefault(expert.getId(), 0L) * 8.0;
        if (expert.getOnTimeRate() != null) {
            score += expert.getOnTimeRate().doubleValue() * 0.35;
        }
        if (expert.getAvgQualityScore() != null) {
            score += expert.getAvgQualityScore().doubleValue() * 4.0;
        }
        if (expert.getReviewCount() != null && expert.getReviewCount() > 0) {
            score += Math.min(expert.getReviewCount(), 20) * 0.5;
        }
        return Math.max(score, 1.0);
    }

    private Map<Long, Long> loadActiveAssignmentCounts(List<Expert> pool) {
        Set<Long> ids = pool.stream().map(Expert::getId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        for (ReviewAssignment a : assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .in(ReviewAssignment::getExpertId, ids)
                .in(ReviewAssignment::getStatus, List.of("PENDING", "ACCEPTED")))) {
            map.merge(a.getExpertId(), 1L, Long::sum);
        }
        return map;
    }

    private Map<Long, Long> loadRejectCounts(List<Expert> pool) {
        Set<Long> ids = pool.stream().map(Expert::getId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        for (ReviewAssignment a : assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .in(ReviewAssignment::getExpertId, ids)
                .eq(ReviewAssignment::getStatus, "REJECTED"))) {
            map.merge(a.getExpertId(), 1L, Long::sum);
        }
        return map;
    }

    private record Scored(Long expertId, double weight) {
    }
}
