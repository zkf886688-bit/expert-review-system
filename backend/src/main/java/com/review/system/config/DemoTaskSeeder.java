package com.review.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.*;
import com.review.system.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 启动时维护演示评审任务（名称以【演示】开头）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoTaskSeeder {

    static final String DEMO_PREFIX = "【演示】";
    static final String DEMO_BATCH = "演示批次";
    /** 历史汇总演示专家手机号前缀，启动时清理 */
    static final String SUMMARY_EXPERT_PHONE_PREFIX = "1380000290";

    private final ReviewTaskMapper taskMapper;
    private final ReviewProjectMapper projectMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewSubmissionMapper submissionMapper;
    private final ReviewSubmissionArchiveMapper submissionArchiveMapper;
    private final ReviewMaterialMapper materialMapper;
    private final ProjectRectificationMaterialMapper rectificationMaterialMapper;
    private final ExpertMapper expertMapper;
    private final SysUserMapper sysUserMapper;
    private final ReviewTypeRegistry reviewTypeRegistry;

    @Value("${app.demo.seed-tasks:true}")
    private boolean seedTasks;

    public void seedIfNeeded() {
        removeSummaryDemoExperts();
        if (!seedTasks) {
            log.debug("演示任务种子已关闭 (app.demo.seed-tasks=false)");
            return;
        }
        pruneObsoleteDemoTasks();
        pruneNonDemoTasks();
        seedMissingDemoTasks();
        ensureCompletedDemoAssignments();
        refreshDemoSchedules();
    }

    /** 仅清空「进行中」演示任务的分配，保留已汇总任务的完整数据。 */
    public void clearLiveDemoAssignments() {
        List<Long> taskIds = taskMapper.selectList(new LambdaQueryWrapper<ReviewTask>()
                        .likeRight(ReviewTask::getName, DEMO_PREFIX)
                        .in(ReviewTask::getStatus, "IN_REVIEW", "PUBLISHED", "PENDING_CONFIRM", "UNASSIGNED"))
                .stream()
                .map(ReviewTask::getId)
                .toList();
        if (taskIds.isEmpty()) {
            return;
        }
        submissionMapper.delete(new LambdaQueryWrapper<ReviewSubmission>()
                .in(ReviewSubmission::getTaskId, taskIds));
        assignmentMapper.delete(new LambdaQueryWrapper<ReviewAssignment>()
                .in(ReviewAssignment::getTaskId, taskIds));
        log.info("已清空 {} 条进行中演示任务的专家分配（已汇总任务不受影响）", taskIds.size());
    }

    /** @deprecated 使用 {@link #clearLiveDemoAssignments()} */
    public void clearDemoAssignments() {
        clearLiveDemoAssignments();
    }

    /** 移除历史「汇总演示专家1~9」，汇总样例任务改用专家库现有专家。 */
    public void removeSummaryDemoExperts() {
        List<Expert> experts = expertMapper.selectList(new LambdaQueryWrapper<Expert>()
                .likeRight(Expert::getPhone, SUMMARY_EXPERT_PHONE_PREFIX));
        if (experts.isEmpty()) {
            return;
        }
        List<Long> expertIds = experts.stream().map(Expert::getId).toList();
        submissionMapper.delete(new LambdaQueryWrapper<ReviewSubmission>()
                .in(ReviewSubmission::getExpertId, expertIds));
        assignmentMapper.delete(new LambdaQueryWrapper<ReviewAssignment>()
                .in(ReviewAssignment::getExpertId, expertIds));
        expertMapper.delete(new LambdaQueryWrapper<Expert>()
                .in(Expert::getId, expertIds));
        log.info("已移除 {} 名汇总演示专家（1380000290*）", experts.size());
    }

    private List<Long> activeExpertIds() {
        return expertMapper.selectList(new LambdaQueryWrapper<Expert>()
                        .eq(Expert::getStatus, "ACTIVE")
                        .orderByAsc(Expert::getId)
                        .last("LIMIT 20"))
                .stream()
                .map(Expert::getId)
                .toList();
    }

    private void seedMissingDemoTasks() {
        List<Long> expertIds = activeExpertIds();
        if (expertIds.size() < 3) {
            log.warn("演示任务跳过：专家库不足 3 人");
            return;
        }
        SysUser admin = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "admin"));
        if (admin == null) {
            log.warn("演示任务跳过：未找到 admin 账号");
            return;
        }
        Long applicantId = null;
        SysUser applicant = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "applicant"));
        if (applicant != null) {
            applicantId = applicant.getId();
        }

        int created = 0;
        for (DemoTaskDef def : demoTaskDefs()) {
            if (taskMapper.selectCount(new LambdaQueryWrapper<ReviewTask>()
                    .eq(ReviewTask::getName, def.name())) > 0) {
                continue;
            }
            createDemoTask(def, admin.getId(), applicantId, expertIds);
            created++;
        }
        if (created > 0) {
            log.info("已写入 {} 条演示评审任务（前缀 {}）", created, DEMO_PREFIX);
        }
    }

    /** 已汇总演示任务若缺少分配记录，自动补全（重启后仍可看汇总/PDF）。 */
    private void ensureCompletedDemoAssignments() {
        List<Long> expertIds = activeExpertIds();
        if (expertIds.size() < 3) {
            return;
        }
        int rebuilt = 0;
        for (DemoTaskDef def : demoTaskDefs()) {
            if (!"SUMMARIZED".equals(def.status())) {
                continue;
            }
            ReviewTask task = taskMapper.selectOne(new LambdaQueryWrapper<ReviewTask>()
                    .eq(ReviewTask::getName, def.name()));
            if (task == null) {
                continue;
            }
            long assignmentCount = assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                    .eq(ReviewAssignment::getTaskId, task.getId()));
            if (assignmentCount > 0) {
                continue;
            }
            List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                    .eq(ReviewProject::getTaskId, task.getId())
                    .orderByAsc(ReviewProject::getSortOrder));
            if (projects.isEmpty()) {
                continue;
            }
            seedAssignmentsForTask(task, def, projects, expertIds);
            rebuilt++;
        }
        if (rebuilt > 0) {
            log.info("已补全 {} 条已完成演示任务的专家意见数据", rebuilt);
        }
    }

    private void pruneObsoleteDemoTasks() {
        Set<String> keepNames = demoTaskDefs().stream().map(DemoTaskDef::name).collect(Collectors.toSet());
        List<ReviewTask> obsolete = taskMapper.selectList(new LambdaQueryWrapper<ReviewTask>()
                .likeRight(ReviewTask::getName, DEMO_PREFIX));
        int removed = 0;
        for (ReviewTask task : obsolete) {
            if (keepNames.contains(task.getName())) {
                continue;
            }
            deleteDemoTask(task.getId());
            removed++;
        }
        if (removed > 0) {
            log.info("已清理 {} 条多余演示任务，当前保留 {} 条", removed, keepNames.size());
        }
    }

    private void pruneNonDemoTasks() {
        List<ReviewTask> extras = taskMapper.selectList(new LambdaQueryWrapper<ReviewTask>()
                .notLikeRight(ReviewTask::getName, DEMO_PREFIX));
        int removed = 0;
        for (ReviewTask task : extras) {
            deleteDemoTask(task.getId());
            removed++;
            log.info("已删除非演示样本任务: {}", task.getName());
        }
        if (removed > 0) {
            log.info("演示环境已移除非【演示】任务 {} 条，列表仅保留演示数据", removed);
        }
    }

    private void deleteDemoTask(Long taskId) {
        List<Long> projectIds = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                        .eq(ReviewProject::getTaskId, taskId))
                .stream()
                .map(ReviewProject::getId)
                .toList();
        submissionMapper.delete(new LambdaQueryWrapper<ReviewSubmission>()
                .eq(ReviewSubmission::getTaskId, taskId));
        assignmentMapper.delete(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getTaskId, taskId));
        materialMapper.delete(new LambdaQueryWrapper<ReviewMaterial>()
                .eq(ReviewMaterial::getTaskId, taskId));
        if (!projectIds.isEmpty()) {
            submissionArchiveMapper.delete(new LambdaQueryWrapper<ReviewSubmissionArchive>()
                    .in(ReviewSubmissionArchive::getProjectId, projectIds));
            rectificationMaterialMapper.delete(new LambdaQueryWrapper<ProjectRectificationMaterial>()
                    .in(ProjectRectificationMaterial::getProjectId, projectIds));
        }
        projectMapper.delete(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId));
        taskMapper.deleteById(taskId);
    }

    private void refreshDemoSchedules() {
        LocalDateTime now = LocalDateTime.now();
        int updated = 0;
        for (DemoTaskDef def : demoTaskDefs()) {
            ReviewTask task = taskMapper.selectOne(new LambdaQueryWrapper<ReviewTask>()
                    .eq(ReviewTask::getName, def.name()));
            if (task == null) {
                continue;
            }
            LocalDateTime start = now.minusDays(10);
            LocalDateTime deadline = now.plusDays(def.deadlineDays());
            if (task.getDeadline() == null
                    || task.getDeadline().isBefore(now)
                    || !start.equals(task.getStartTime())
                    || !deadline.equals(task.getDeadline())) {
                task.setStartTime(start);
                task.setDeadline(deadline);
                taskMapper.updateById(task);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("已刷新 {} 条演示任务截止时间为相对当前日期（便于现场演示）", updated);
        }
    }

    private void createDemoTask(DemoTaskDef def, Long adminId, Long applicantUserId, List<Long> expertIds) {
        LocalDateTime now = LocalDateTime.now();
        ReviewTask task = new ReviewTask();
        task.setName(def.name());
        task.setReviewType(def.reviewType());
        task.setBatchName(DEMO_BATCH);
        task.setDataDomain("DEFAULT");
        task.setStartTime(now.minusDays(10));
        task.setDeadline(now.plusDays(def.deadlineDays()));
        task.setMinReviewers(3);
        task.setFormConfig(reviewTypeRegistry.defaultFormConfig(def.reviewType()));
        task.setBlindReview(def.blindReview());
        task.setAvoidSameOrg(true);
        task.setAvoidDuplicateExpert(true);
        task.setStatus(def.status());
        task.setCreatedBy(adminId);
        taskMapper.insert(task);

        List<ReviewProject> projects = new ArrayList<>();
        for (int i = 0; i < def.projectCount(); i++) {
            ReviewProject project = new ReviewProject();
            project.setTaskId(task.getId());
            project.setProjectName(def.projectLabel(i + 1));
            project.setApplicant("申报人" + (char) ('A' + i));
            project.setOrganization(def.orgs()[i % def.orgs().length]);
            if (i == 0 && applicantUserId != null) {
                project.setApplicantUserId(applicantUserId);
            }
            project.setSortOrder(i);
            projectMapper.insert(project);
            projects.add(project);
        }

        if ("UNASSIGNED".equals(def.status())) {
            return;
        }
        seedAssignmentsForTask(task, def, projects, expertIds);
    }

    private void seedAssignmentsForTask(ReviewTask task, DemoTaskDef def, List<ReviewProject> projects, List<Long> expertIds) {
        LocalDateTime now = LocalDateTime.now();
        int expertCursor = 0;
        int assignmentSeq = 0;
        for (ReviewProject project : projects) {
            for (int e = 0; e < 3; e++) {
                Long expertId = expertIds.get((expertCursor + e) % expertIds.size());
                ReviewAssignment assignment = new ReviewAssignment();
                assignment.setTaskId(task.getId());
                assignment.setProjectId(project.getId());
                assignment.setExpertId(expertId);
                assignment.setInvitedAt(now.minusDays(5));
                String assignStatus = resolveAssignmentStatus(def, assignmentSeq);
                assignment.setStatus(assignStatus);
                if ("ACCEPTED".equals(assignStatus) || "SUBMITTED".equals(assignStatus)) {
                    assignment.setRespondedAt(now.minusDays(3));
                }
                if ("SUBMITTED".equals(assignStatus)) {
                    assignment.setSubmittedAt(now.minusDays(1));
                }
                assignmentMapper.insert(assignment);
                if ("SUBMITTED".equals(assignStatus)) {
                    insertSubmission(task, project, assignment, def.reviewType(), assignmentSeq);
                }
                assignmentSeq++;
            }
            expertCursor += 3;
        }
    }

    private static String resolveAssignmentStatus(DemoTaskDef def, int seq) {
        return switch (def.assignmentPattern()) {
            case ALL_PENDING -> "PENDING";
            case ALL_SUBMITTED -> "SUBMITTED";
            case MIXED_REVIEW -> (seq % 3 == 0) ? "SUBMITTED" : ((seq % 3 == 1) ? "ACCEPTED" : "PENDING");
        };
    }

    private void insertSubmission(ReviewTask task, ReviewProject project, ReviewAssignment assignment,
                                String reviewType, int seq) {
        List<Map<String, Object>> formConfig = reviewTypeRegistry.defaultFormConfig(reviewType);
        Map<String, Object> scores = new LinkedHashMap<>();
        double factor = 0.78 + (seq % 5) * 0.04;
        for (Map<String, Object> item : formConfig) {
            Object key = item.get("key");
            Object maxScore = item.get("maxScore");
            if (key != null && maxScore != null) {
                int max = Integer.parseInt(String.valueOf(maxScore));
                scores.put(String.valueOf(key), Math.max(1, (int) (max * factor)));
            }
        }
        double total = factor * 100;
        ReviewSubmission sub = new ReviewSubmission();
        sub.setAssignmentId(assignment.getId());
        sub.setTaskId(task.getId());
        sub.setProjectId(project.getId());
        sub.setExpertId(assignment.getExpertId());
        sub.setScores(scores);
        sub.setTotalScore(BigDecimal.valueOf(total).setScale(2, java.math.RoundingMode.HALF_UP));
        sub.setOpinion("演示数据：项目目标明确、方案可行，建议通过。");
        List<String> conclusions = reviewTypeRegistry.conclusionOptions(reviewType);
        sub.setConclusion(conclusions.isEmpty() ? "通过" : conclusions.get(0));
        sub.setStatus("SUBMITTED");
        sub.setVersion(1);
        sub.setSubmittedAt(LocalDateTime.now().minusHours(12 + seq));
        submissionMapper.insert(sub);
    }

    private static List<DemoTaskDef> demoTaskDefs() {
        return List.of(
                new DemoTaskDef(DEMO_PREFIX + "2026年度项目立项评审", "项目立项", "IN_REVIEW", 45, 3,
                        AssignmentPattern.MIXED_REVIEW, false,
                        new String[]{"东南理工大学", "材料科学研究院", "华信电子股份有限公司"}),
                new DemoTaskDef(DEMO_PREFIX + "结题验收汇总样例", "结题验收", "SUMMARIZED", 30, 2,
                        AssignmentPattern.ALL_SUBMITTED, false,
                        new String[]{"管理学院", "材料科学研究院"})
        );
    }

    private record DemoTaskDef(
            String name,
            String reviewType,
            String status,
            int deadlineDays,
            int projectCount,
            AssignmentPattern assignmentPattern,
            boolean blindReview,
            String[] orgs) {

        String projectLabel(int index) {
            return name().replace(DEMO_PREFIX, "").trim() + " — 项目" + index;
        }
    }

    private enum AssignmentPattern {
        ALL_PENDING, ALL_SUBMITTED, MIXED_REVIEW
    }
}
