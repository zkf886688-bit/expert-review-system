package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.dto.ExpertAssignmentVO;
import com.review.system.dto.InviteResponseRequest;
import com.review.system.dto.ReviewSubmissionVO;
import com.review.system.dto.ReviewSubmitRequest;
import com.review.system.entity.*;
import com.review.system.mapper.*;
import com.review.system.security.LoginUser;
import com.review.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpertReviewService {

    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewSubmissionMapper submissionMapper;
    private final ReviewTaskMapper taskMapper;
    private final ReviewProjectMapper projectMapper;
    private final ReviewMaterialMapper materialMapper;
    private final ExpertMapper expertMapper;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;
    private final RectificationService rectificationService;
    private final ExpertCreditService expertCreditService;
    private final FileService fileService;

    public List<ExpertAssignmentVO> myAssignments(String statusFilter, String workbench) {
        LoginUser user = SecurityUtils.currentUser();
        if (user.getExpertId() == null) {
            throw new BusinessException("当前账号未关联专家信息");
        }
        LambdaQueryWrapper<ReviewAssignment> wrapper = new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getExpertId, user.getExpertId())
                .orderByDesc(ReviewAssignment::getInvitedAt);
        if (StringUtils.hasText(workbench)) {
            switch (workbench.toUpperCase()) {
                case "PENDING_ACCEPT" -> wrapper.eq(ReviewAssignment::getStatus, "PENDING");
                case "PENDING_REVIEW" -> wrapper.in(ReviewAssignment::getStatus, "ACCEPTED", "OVERDUE");
                case "DEADLINE_SOON" -> wrapper.notIn(ReviewAssignment::getStatus, "SUBMITTED", "REJECTED");
                default -> { /* ALL */ }
            }
        } else if (statusFilter != null && !statusFilter.isBlank()) {
            wrapper.eq(ReviewAssignment::getStatus, statusFilter);
        }
        List<ExpertAssignmentVO> list = assignmentMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
        if ("DEADLINE_SOON".equalsIgnoreCase(workbench)) {
            LocalDateTime now = LocalDateTime.now();
            list = list.stream()
                    .filter(vo -> isDeadlineSoon(vo.getDeadline(), now))
                    .toList();
        }
        return list;
    }

    private boolean isDeadlineSoon(String deadlineStr, LocalDateTime now) {
        if (!StringUtils.hasText(deadlineStr)) {
            return false;
        }
        try {
            LocalDateTime deadline = LocalDateTime.parse(deadlineStr.replace(' ', 'T'));
            long days = ChronoUnit.DAYS.between(now, deadline);
            return days <= 3;
        } catch (Exception e) {
            return false;
        }
    }

    public ExpertAssignmentVO assignmentDetail(Long assignmentId) {
        ReviewAssignment assignment = getOwnedAssignment(assignmentId);
        return toDetailVO(assignment);
    }

    @Transactional
    public void respondInvite(InviteResponseRequest req) {
        ReviewAssignment assignment = getOwnedAssignment(req.getAssignmentId());
        if (!"PENDING".equals(assignment.getStatus())) {
            throw new BusinessException("当前状态不可响应邀请");
        }
        ReviewTask task = taskMapper.selectById(assignment.getTaskId());

        if (req.isAccept()) {
            assignment.setStatus("ACCEPTED");
            assignment.setRespondedAt(LocalDateTime.now());
            assignment.setRejectReason(null);
            if (task != null && "PENDING_CONFIRM".equals(task.getStatus())) {
                task.setStatus("IN_REVIEW");
                taskMapper.updateById(task);
            }
            auditService.log("ACCEPT_INVITE", "ASSIGNMENT", assignment.getId(), "专家接受评审邀请");
        } else {
            if (!StringUtils.hasText(req.getReason())) {
                throw new BusinessException("拒绝或回避须填写原因");
            }
            assignment.setStatus("REJECTED");
            assignment.setRespondedAt(LocalDateTime.now());
            assignment.setRejectReason(req.getReason());
            auditService.log("REJECT_INVITE", "ASSIGNMENT", assignment.getId(), "专家拒绝: " + req.getReason());
            expertCreditService.recordReject(assignment.getExpertId(), assignment.getId(), req.getReason());
        }
        assignmentMapper.updateById(assignment);
    }

    @Transactional
    public ReviewSubmissionVO saveReview(ReviewSubmitRequest req) {
        ReviewAssignment assignment = getOwnedAssignment(req.getAssignmentId());
        if ("SUBMITTED".equals(assignment.getStatus())) {
            throw new BusinessException("评审已提交，如需修改请联系管理员");
        }
        if (!"ACCEPTED".equals(assignment.getStatus())) {
            throw new BusinessException("请先接受评审邀请后再填写");
        }
        ReviewTask task = taskMapper.selectById(assignment.getTaskId());
        if (task.getDeadline().isBefore(LocalDateTime.now())) {
            assignment.setStatus("OVERDUE");
            assignmentMapper.updateById(assignment);
            throw new BusinessException("已超过评审截止时间");
        }

        ReviewSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<ReviewSubmission>()
                .eq(ReviewSubmission::getAssignmentId, assignment.getId()));
        if (submission == null) {
            submission = new ReviewSubmission();
            submission.setAssignmentId(assignment.getId());
            submission.setTaskId(assignment.getTaskId());
            submission.setProjectId(assignment.getProjectId());
            submission.setExpertId(assignment.getExpertId());
            submission.setVersion(1);
        }
        submission.setScores(req.getScores() != null ? req.getScores() : Map.of());
        submission.setTotalScore(calcTotalScore(task.getFormConfig(), submission.getScores()));
        submission.setOpinion(req.getOpinion());
        submission.setConclusion(req.getConclusion());

        if (req.isSubmit()) {
            submission.setStatus("SUBMITTED");
            submission.setSubmittedAt(LocalDateTime.now());
            assignment.setStatus("SUBMITTED");
            assignment.setSubmittedAt(LocalDateTime.now());
            assignmentMapper.updateById(assignment);
            Expert expert = expertMapper.selectById(assignment.getExpertId());
            if (expert != null) {
                expert.setReviewCount((expert.getReviewCount() != null ? expert.getReviewCount() : 0) + 1);
                expertMapper.updateById(expert);
            }
            auditService.log("SUBMIT_REVIEW", "ASSIGNMENT", assignment.getId(), "专家提交评审");
        } else {
            submission.setStatus("DRAFT");
        }

        if (submission.getId() == null) {
            submissionMapper.insert(submission);
        } else {
            submissionMapper.updateById(submission);
        }
        if (req.isSubmit() && expertCreditService.isSubmittedOnTime(assignment.getId(), submission.getSubmittedAt())) {
            expertCreditService.recordOnTime(assignment.getExpertId(), assignment.getId(), submission.getId());
        }
        if (req.isSubmit()) {
            notifyApplicantProgress(task, assignment.getProjectId());
            rectificationService.tryCompleteReReview(assignment.getProjectId());
        }
        return toSubmissionVO(submission);
    }

    private void notifyApplicantProgress(ReviewTask task, Long projectId) {
        ReviewProject project = projectMapper.selectById(projectId);
        if (project == null || project.getApplicantUserId() == null) {
            return;
        }
        long submitted = assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getProjectId, projectId)
                .eq(ReviewAssignment::getStatus, "SUBMITTED"));
        Map<String, String> vars = Map.of(
                "projectName", project.getProjectName(),
                "submittedCount", String.valueOf(submitted),
                "minReviewers", String.valueOf(task.getMinReviewers()));
        notificationService.notifyUser(project.getApplicantUserId(),
                notificationTemplateService.applicantProgressTitle(vars),
                notificationTemplateService.applicantProgressContent(vars),
                "APPLICANT_PROGRESS", projectId);
    }

    @Transactional
    public void returnForRevision(Long assignmentId) {
        ReviewAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("分配记录不存在");
        }
        if (!"SUBMITTED".equals(assignment.getStatus())) {
            throw new BusinessException("仅已提交的评审可退回");
        }
        assignment.setStatus("ACCEPTED");
        assignment.setSubmittedAt(null);
        assignmentMapper.updateById(assignment);

        ReviewSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<ReviewSubmission>()
                .eq(ReviewSubmission::getAssignmentId, assignmentId));
        if (submission != null) {
            submission.setStatus("DRAFT");
            submission.setSubmittedAt(null);
            submission.setVersion(submission.getVersion() + 1);
            submissionMapper.updateById(submission);
        }
        Map<String, String> vars = Map.of();
        notificationService.notifyExpert(assignment.getExpertId(),
                notificationTemplateService.returnTitle(vars),
                notificationTemplateService.returnContent(vars),
                "SYSTEM", assignmentId);
        auditService.log("RETURN_REVIEW", "ASSIGNMENT", assignmentId, "管理员退回评审");
    }

    @Transactional
    public ReviewSubmissionVO uploadAttachment(Long assignmentId, org.springframework.web.multipart.MultipartFile file) {
        ReviewAssignment assignment = getOwnedAssignment(assignmentId);
        if (!"ACCEPTED".equals(assignment.getStatus()) && !"SUBMITTED".equals(assignment.getStatus())) {
            throw new BusinessException("当前状态不可上传附件");
        }
        String path = fileService.save(file, "review/" + assignment.getId());
        ReviewSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<ReviewSubmission>()
                .eq(ReviewSubmission::getAssignmentId, assignmentId));
        if (submission == null) {
            submission = new ReviewSubmission();
            submission.setAssignmentId(assignment.getId());
            submission.setTaskId(assignment.getTaskId());
            submission.setProjectId(assignment.getProjectId());
            submission.setExpertId(assignment.getExpertId());
            submission.setScores(Map.of());
            submission.setStatus("DRAFT");
            submission.setVersion(1);
        }
        submission.setAttachmentPath(path);
        submission.setAttachmentName(file.getOriginalFilename());
        if (submission.getId() == null) {
            submissionMapper.insert(submission);
        } else {
            submissionMapper.updateById(submission);
        }
        auditService.log("UPLOAD_REVIEW_ATTACHMENT", "ASSIGNMENT", assignmentId, file.getOriginalFilename());
        return toSubmissionVO(submission);
    }

    public List<ExpertAssignmentVO> myHistory() {
        return myAssignments("SUBMITTED", null);
    }

    private ReviewAssignment getOwnedAssignment(Long assignmentId) {
        LoginUser user = SecurityUtils.currentUser();
        ReviewAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("分配记录不存在");
        }
        if (user.getExpertId() == null || !user.getExpertId().equals(assignment.getExpertId())) {
            throw new BusinessException("无权访问该评审任务");
        }
        return assignment;
    }

    private ExpertAssignmentVO toVO(ReviewAssignment a) {
        ReviewTask task = taskMapper.selectById(a.getTaskId());
        ReviewProject project = projectMapper.selectById(a.getProjectId());
        ExpertAssignmentVO vo = ExpertAssignmentVO.builder()
                .assignmentId(a.getId())
                .taskId(a.getTaskId())
                .taskName(task != null ? task.getName() : null)
                .reviewType(task != null ? task.getReviewType() : null)
                .deadline(task != null ? task.getDeadline().toString() : null)
                .assignmentStatus(a.getStatus())
                .projectId(a.getProjectId())
                .projectName(project != null ? project.getProjectName() : null)
                .applicant(project != null ? project.getApplicant() : null)
                .organization(project != null ? project.getOrganization() : null)
                .blindReview(task != null && Boolean.TRUE.equals(task.getBlindReview()))
                .build();
        applyBlindMask(vo, task);
        return vo;
    }

    private ExpertAssignmentVO toDetailVO(ReviewAssignment a) {
        ReviewTask task = taskMapper.selectById(a.getTaskId());
        ReviewProject project = projectMapper.selectById(a.getProjectId());
        List<ReviewMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<ReviewMaterial>()
                .eq(ReviewMaterial::getTaskId, a.getTaskId())
                .and(w -> w.eq(ReviewMaterial::getProjectId, a.getProjectId())
                        .or().isNull(ReviewMaterial::getProjectId)));
        ReviewSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<ReviewSubmission>()
                .eq(ReviewSubmission::getAssignmentId, a.getId()));
        ExpertAssignmentVO vo = ExpertAssignmentVO.builder()
                .assignmentId(a.getId())
                .taskId(a.getTaskId())
                .taskName(task != null ? task.getName() : null)
                .reviewType(task != null ? task.getReviewType() : null)
                .deadline(task != null ? task.getDeadline().toString() : null)
                .assignmentStatus(a.getStatus())
                .projectId(a.getProjectId())
                .projectName(project != null ? project.getProjectName() : null)
                .applicant(project != null ? project.getApplicant() : null)
                .organization(project != null ? project.getOrganization() : null)
                .blindReview(task != null && Boolean.TRUE.equals(task.getBlindReview()))
                .task(task)
                .materials(materials)
                .submission(submission != null ? toSubmissionVO(submission) : null)
                .build();
        applyBlindMask(vo, task);
        return vo;
    }

    private void applyBlindMask(ExpertAssignmentVO vo, ReviewTask task) {
        if (task != null && Boolean.TRUE.equals(task.getBlindReview())) {
            vo.setApplicant("***");
            vo.setOrganization("***");
            if (vo.getTask() != null) {
                vo.getTask().setCreatedBy(null);
            }
        }
    }

    private ReviewSubmissionVO toSubmissionVO(ReviewSubmission s) {
        return ReviewSubmissionVO.builder()
                .id(s.getId())
                .scores(s.getScores())
                .totalScore(s.getTotalScore())
                .opinion(s.getOpinion())
                .conclusion(s.getConclusion())
                .status(s.getStatus())
                .attachmentName(s.getAttachmentName())
                .hasAttachment(s.getAttachmentPath() != null)
                .build();
    }

    private BigDecimal calcTotalScore(List<Map<String, Object>> formConfig, Map<String, Object> scores) {
        if (formConfig == null || scores == null) {
            return BigDecimal.ZERO;
        }
        double total = 0;
        for (Map<String, Object> item : formConfig) {
            String key = String.valueOf(item.get("key"));
            Object scoreVal = scores.get(key);
            if (scoreVal == null) continue;
            double score = Double.parseDouble(String.valueOf(scoreVal));
            double weight = item.get("weight") != null
                    ? Double.parseDouble(String.valueOf(item.get("weight"))) : 0;
            total += score * weight;
        }
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }
}
