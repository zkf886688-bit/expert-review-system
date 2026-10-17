package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.*;
import com.review.system.mapper.*;
import com.review.system.security.DataScopeService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewAnomalyService {

    private static final int MIN_OPINION_LEN = 20;
    private static final double SCORE_SPREAD_THRESHOLD = 25.0;

    private final ReviewTaskMapper taskMapper;
    private final ReviewProjectMapper projectMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewSubmissionMapper submissionMapper;
    private final DataScopeService dataScopeService;

    public List<AnomalyVO> listForTask(Long taskId) {
        ReviewTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return List.of();
        }
        dataScopeService.assertTaskAccess(task);
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId));
        List<AnomalyVO> list = new ArrayList<>();
        for (ReviewProject project : projects) {
            list.addAll(detectProjectAnomalies(task, project));
        }
        long rejected = assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getTaskId, taskId)
                .eq(ReviewAssignment::getStatus, "REJECTED"));
        if (rejected >= 3) {
            list.add(AnomalyVO.builder()
                    .type("MASS_REJECT")
                    .level("WARN")
                    .message("本任务累计 " + rejected + " 次专家拒绝/回避")
                    .build());
        }
        return list;
    }

    public List<AnomalyVO> listGlobal() {
        LambdaQueryWrapper<ReviewTask> wrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyTaskFilter(wrapper);
        List<ReviewTask> tasks = taskMapper.selectList(wrapper);
        List<AnomalyVO> list = new ArrayList<>();
        for (ReviewTask task : tasks) {
            if ("ARCHIVED".equals(task.getStatus())) {
                continue;
            }
            list.addAll(listForTask(task.getId()));
        }
        return list.stream().limit(100).toList();
    }

    private List<AnomalyVO> detectProjectAnomalies(ReviewTask task, ReviewProject project) {
        List<AnomalyVO> list = new ArrayList<>();
        long submitted = assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getProjectId, project.getId())
                .eq(ReviewAssignment::getStatus, "SUBMITTED"));
        int min = task.getMinReviewers() != null ? task.getMinReviewers() : 3;
        if (submitted < min && !"ARCHIVED".equals(task.getStatus()) && !"SUMMARIZED".equals(task.getStatus())) {
            list.add(AnomalyVO.builder()
                    .taskId(task.getId())
                    .taskName(task.getName())
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .type("INSUFFICIENT_REVIEWERS")
                    .level("ERROR")
                    .message("已提交 " + submitted + "/" + min + "，未达最低评审人数")
                    .build());
        }

        List<ReviewSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<ReviewSubmission>()
                .eq(ReviewSubmission::getProjectId, project.getId())
                .eq(ReviewSubmission::getStatus, "SUBMITTED"));
        for (ReviewSubmission s : submissions) {
            if (s.getOpinion() == null || s.getOpinion().trim().length() < MIN_OPINION_LEN) {
                list.add(AnomalyVO.builder()
                        .taskId(task.getId())
                        .taskName(task.getName())
                        .projectId(project.getId())
                        .projectName(project.getProjectName())
                        .type("SHORT_OPINION")
                        .level("WARN")
                        .message("存在意见过短的评审提交（少于 " + MIN_OPINION_LEN + " 字）")
                        .build());
                break;
            }
        }

        List<BigDecimal> scores = submissions.stream()
                .map(ReviewSubmission::getTotalScore)
                .filter(Objects::nonNull)
                .toList();
        if (scores.size() >= 2) {
            BigDecimal max = scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal minScore = scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            double spread = max.subtract(minScore).doubleValue();
            if (spread >= SCORE_SPREAD_THRESHOLD) {
                list.add(AnomalyVO.builder()
                        .taskId(task.getId())
                        .taskName(task.getName())
                        .projectId(project.getId())
                        .projectName(project.getProjectName())
                        .type("SCORE_SPREAD")
                        .level("WARN")
                        .message(String.format("专家评分差异较大（极差 %.1f 分）", spread))
                        .build());
            }
        }
        return list;
    }

    @Data
    @Builder
    public static class AnomalyVO {
        private Long taskId;
        private String taskName;
        private Long projectId;
        private String projectName;
        private String type;
        private String level;
        private String message;
    }
}
