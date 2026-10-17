package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.dto.CreditRateRequest;
import com.review.system.entity.*;
import com.review.system.mapper.*;
import com.review.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertCreditService {

    private final ExpertCreditRecordMapper creditRecordMapper;
    private final ExpertMapper expertMapper;
    private final ReviewSubmissionMapper submissionMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewTaskMapper taskMapper;
    private final AuditService auditService;

    public List<ExpertCreditRecord> listByExpert(Long expertId) {
        return creditRecordMapper.selectList(new LambdaQueryWrapper<ExpertCreditRecord>()
                .eq(ExpertCreditRecord::getExpertId, expertId)
                .orderByDesc(ExpertCreditRecord::getCreatedAt));
    }

    @Transactional
    public void rateQuality(CreditRateRequest req) {
        ReviewSubmission submission = submissionMapper.selectById(req.getSubmissionId());
        if (submission == null || !"SUBMITTED".equals(submission.getStatus())) {
            throw new BusinessException("只能对已提交的评审进行质量评分");
        }
        ExpertCreditRecord record = new ExpertCreditRecord();
        record.setExpertId(submission.getExpertId());
        record.setAssignmentId(submission.getAssignmentId());
        record.setSubmissionId(submission.getId());
        record.setRecordType("QUALITY");
        record.setScore(BigDecimal.valueOf(req.getScore()));
        record.setComment(req.getComment());
        record.setCreatedBy(SecurityUtils.currentUserId());
        record.setCreatedAt(LocalDateTime.now());
        creditRecordMapper.insert(record);
        recalculateQuality(submission.getExpertId());
        auditService.log("RATE_QUALITY", "EXPERT", submission.getExpertId(),
                "质量评分" + req.getScore());
    }

    @Transactional
    public void recordOnTime(Long expertId, Long assignmentId, Long submissionId) {
        ExpertCreditRecord record = new ExpertCreditRecord();
        record.setExpertId(expertId);
        record.setAssignmentId(assignmentId);
        record.setSubmissionId(submissionId);
        record.setRecordType("ON_TIME");
        record.setScore(BigDecimal.valueOf(5));
        record.setComment("按时提交");
        record.setCreatedAt(LocalDateTime.now());
        creditRecordMapper.insert(record);
        recalculateOnTimeRate(expertId);
    }

    @Transactional
    public void recordReject(Long expertId, Long assignmentId, String reason) {
        ExpertCreditRecord record = new ExpertCreditRecord();
        record.setExpertId(expertId);
        record.setAssignmentId(assignmentId);
        record.setRecordType("REJECT");
        record.setComment(reason);
        record.setCreatedBy(SecurityUtils.currentUserId());
        record.setCreatedAt(LocalDateTime.now());
        creditRecordMapper.insert(record);
        recalculateOnTimeRate(expertId);
    }

    @Transactional
    public void recordOverdue(Long expertId, Long assignmentId) {
        ExpertCreditRecord record = new ExpertCreditRecord();
        record.setExpertId(expertId);
        record.setAssignmentId(assignmentId);
        record.setRecordType("OVERDUE");
        record.setComment("逾期未提交");
        record.setCreatedAt(LocalDateTime.now());
        creditRecordMapper.insert(record);
        recalculateOnTimeRate(expertId);
    }

    private void recalculateQuality(Long expertId) {
        List<ExpertCreditRecord> list = creditRecordMapper.selectList(new LambdaQueryWrapper<ExpertCreditRecord>()
                .eq(ExpertCreditRecord::getExpertId, expertId)
                .eq(ExpertCreditRecord::getRecordType, "QUALITY"));
        if (list.isEmpty()) return;
        double avg = list.stream().mapToDouble(r -> r.getScore().doubleValue()).average().orElse(0);
        Expert expert = expertMapper.selectById(expertId);
        if (expert != null) {
            expert.setAvgQualityScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            expertMapper.updateById(expert);
        }
    }

    public void recalculateOnTimeRate(Long expertId) {
        List<ReviewAssignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getExpertId, expertId)
                .in(ReviewAssignment::getStatus, "SUBMITTED", "OVERDUE", "REJECTED"));
        if (assignments.isEmpty()) return;
        long onTime = creditRecordMapper.selectCount(new LambdaQueryWrapper<ExpertCreditRecord>()
                .eq(ExpertCreditRecord::getExpertId, expertId)
                .eq(ExpertCreditRecord::getRecordType, "ON_TIME"));
        long overdue = creditRecordMapper.selectCount(new LambdaQueryWrapper<ExpertCreditRecord>()
                .eq(ExpertCreditRecord::getExpertId, expertId)
                .eq(ExpertCreditRecord::getRecordType, "OVERDUE"));
        long reject = creditRecordMapper.selectCount(new LambdaQueryWrapper<ExpertCreditRecord>()
                .eq(ExpertCreditRecord::getExpertId, expertId)
                .eq(ExpertCreditRecord::getRecordType, "REJECT"));
        long total = onTime + overdue + reject;
        if (total == 0) return;
        BigDecimal rate = BigDecimal.valueOf(onTime * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        Expert expert = expertMapper.selectById(expertId);
        if (expert != null) {
            expert.setOnTimeRate(rate);
            expertMapper.updateById(expert);
        }
    }

    public boolean isSubmittedOnTime(Long assignmentId, LocalDateTime submittedAt) {
        ReviewAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null || submittedAt == null) return false;
        ReviewTask task = taskMapper.selectById(assignment.getTaskId());
        return task != null && !submittedAt.isAfter(task.getDeadline());
    }
}
