package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.dto.ApplicantProjectVO;
import com.review.system.dto.ApplicantResultVO;
import com.review.system.entity.ReviewAssignment;
import com.review.system.entity.ReviewMaterial;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.ReviewAssignmentMapper;
import com.review.system.mapper.ReviewMaterialMapper;
import com.review.system.mapper.ReviewProjectMapper;
import com.review.system.mapper.ReviewTaskMapper;
import com.review.system.security.DataScopeService;
import com.review.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicantService {

    private final ReviewProjectMapper projectMapper;
    private final ReviewTaskMapper taskMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewMaterialMapper materialMapper;
    private final FileService fileService;
    private final SummaryService summaryService;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final RectificationService rectificationService;

    public List<ApplicantProjectVO> listMyProjects() {
        Long userId = SecurityUtils.currentUserId();
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getApplicantUserId, userId)
                .orderByDesc(ReviewProject::getCreatedAt));
        return projects.stream().map(this::toSummary).toList();
    }

    public ApplicantProjectVO projectDetail(Long projectId) {
        ReviewProject project = requireOwnProject(projectId);
        return toDetail(project);
    }

    @Transactional
    public ReviewMaterial uploadMaterial(Long taskId, Long projectId, MultipartFile file) {
        ReviewProject project = requireOwnProject(projectId);
        if (!project.getTaskId().equals(taskId)) {
            throw new BusinessException("项目与任务不匹配");
        }
        ReviewTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if ("SUMMARIZED".equals(task.getStatus()) || "ARCHIVED".equals(task.getStatus())) {
            throw new BusinessException("任务已结束，无法上传材料");
        }
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("已过截止时间，无法上传材料");
        }
        String path = fileService.save(file, "task/" + taskId);
        ReviewMaterial material = new ReviewMaterial();
        material.setTaskId(taskId);
        material.setProjectId(projectId);
        material.setFileName(file.getOriginalFilename());
        material.setFilePath(path);
        material.setFileSize(file.getSize());
        material.setUploadedBy(SecurityUtils.currentUserId());
        materialMapper.insert(material);
        auditService.log("APPLICANT_UPLOAD", "PROJECT", projectId, "申报人上传材料: " + material.getFileName());
        return material;
    }

    private ReviewProject requireOwnProject(Long projectId) {
        ReviewProject project = projectMapper.selectById(projectId);
        dataScopeService.assertApplicantProject(project);
        return project;
    }

    private ApplicantProjectVO toSummary(ReviewProject project) {
        ReviewTask task = taskMapper.selectById(project.getTaskId());
        if (task == null) {
            return null;
        }
        long submitted = countAssignments(project, "SUBMITTED");
        long accepted = countAssignments(project, "ACCEPTED") + submitted;
        return ApplicantProjectVO.builder()
                .projectId(project.getId())
                .taskId(task.getId())
                .projectName(project.getProjectName())
                .organization(project.getOrganization())
                .taskName(task.getName())
                .reviewType(task.getReviewType())
                .deadline(task.getDeadline())
                .taskStatus(task.getStatus())
                .minReviewers(task.getMinReviewers())
                .submittedCount(submitted)
                .acceptedCount(accepted)
                .canUpload(canUpload(task))
                .reviewRound(project.getReviewRound() == null ? 1 : project.getReviewRound())
                .rectificationStatus(project.getRectificationStatus() == null ? "NONE" : project.getRectificationStatus())
                .build();
    }

    private ApplicantProjectVO toDetail(ReviewProject project) {
        ApplicantProjectVO vo = toSummary(project);
        if (vo == null) {
            throw new BusinessException("任务不存在");
        }
        List<ReviewMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<ReviewMaterial>()
                .eq(ReviewMaterial::getTaskId, project.getTaskId())
                .and(w -> w.eq(ReviewMaterial::getProjectId, project.getId())
                        .or().isNull(ReviewMaterial::getProjectId))
                .orderByDesc(ReviewMaterial::getCreatedAt));
        vo.setMaterials(materials.stream().map(m -> ApplicantProjectVO.MaterialItem.builder()
                .id(m.getId())
                .fileName(m.getFileName())
                .fileSize(m.getFileSize())
                .createdAt(m.getCreatedAt())
                .build()).toList());
        vo.setResult(buildResult(project));
        vo.setRectification(rectificationService.toVo(project));
        return vo;
    }

    public ApplicantResultVO getProjectResult(Long projectId) {
        ReviewProject project = requireOwnProject(projectId);
        return buildResult(project);
    }

    private ApplicantResultVO buildResult(ReviewProject project) {
        ReviewTask task = taskMapper.selectById(project.getTaskId());
        if (task == null) {
            return ApplicantResultVO.builder().available(false).message("任务不存在").build();
        }
        long submitted = countAssignments(project, "SUBMITTED");
        boolean taskDone = "SUMMARIZED".equals(task.getStatus()) || "ARCHIVED".equals(task.getStatus());
        if (!taskDone && submitted < task.getMinReviewers()) {
            return ApplicantResultVO.builder()
                    .available(false)
                    .message("评审进行中，达到最低评审人数或任务汇总后可查看结果")
                    .submittedCount((int) submitted)
                    .minReviewers(task.getMinReviewers())
                    .build();
        }
        SummaryService.ProjectSummaryVO summary = summaryService.summarizeProject(project.getId(), true);
        List<ApplicantResultVO.OpinionItem> opinions = summary.getOpinions() == null ? List.of()
                : summary.getOpinions().stream()
                .map(o -> ApplicantResultVO.OpinionItem.builder()
                        .expertLabel(o.getExpertName())
                        .totalScore(o.getTotalScore())
                        .conclusion(o.getConclusion())
                        .opinion(o.getOpinion())
                        .build())
                .toList();
        return ApplicantResultVO.builder()
                .available(true)
                .message(taskDone ? "评审已汇总" : "评审意见已出齐")
                .submittedCount(summary.getSubmittedCount())
                .minReviewers(summary.getMinReviewers())
                .avgScore(summary.getAvgScore())
                .maxScore(summary.getMaxScore())
                .minScore(summary.getMinScore())
                .recommendation(summary.getRecommendation())
                .meetsMinimum(summary.isMeetsMinimum())
                .opinions(opinions)
                .build();
    }

    private long countAssignments(ReviewProject project, String status) {
        return assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getProjectId, project.getId())
                .eq(ReviewAssignment::getStatus, status));
    }

    private boolean canUpload(ReviewTask task) {
        if ("SUMMARIZED".equals(task.getStatus()) || "ARCHIVED".equals(task.getStatus())) {
            return false;
        }
        return task.getDeadline() == null || !task.getDeadline().isBefore(LocalDateTime.now());
    }
}
