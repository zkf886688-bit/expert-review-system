package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.dto.RectificationVO;
import com.review.system.entity.ProjectRectificationMaterial;
import com.review.system.entity.ReviewAssignment;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewSubmission;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.ProjectRectificationMaterialMapper;
import com.review.system.mapper.ReviewAssignmentMapper;
import com.review.system.mapper.ReviewProjectMapper;
import com.review.system.mapper.ReviewSubmissionMapper;
import com.review.system.mapper.ReviewTaskMapper;
import com.review.system.security.DataScopeService;
import com.review.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RectificationService {

    public static final String STATUS_NONE = "NONE";
    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_RE_REVIEWING = "RE_REVIEWING";

    private static final Set<String> CAN_REQUEST_FROM = Set.of(STATUS_NONE, STATUS_RE_REVIEWING);

    private final ReviewProjectMapper projectMapper;
    private final ReviewTaskMapper taskMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewSubmissionMapper submissionMapper;
    private final ProjectRectificationMaterialMapper rectMaterialMapper;
    private final FileService fileService;
    private final DataScopeService dataScopeService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SubmissionArchiveService submissionArchiveService;

    public RectificationVO getRectification(Long taskId, Long projectId) {
        ReviewProject project = requireTaskProject(taskId, projectId);
        dataScopeService.assertTaskAccess(taskMapper.selectById(taskId));
        return toVo(project);
    }

    @Transactional
    public RectificationVO requestRectification(Long taskId, Long projectId, String note) {
        ReviewProject project = requireTaskProject(taskId, projectId);
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        assertTaskNotArchived(task);
        if (project.getApplicantUserId() == null) {
            throw new BusinessException("请先绑定申报人");
        }
        if (!StringUtils.hasText(note)) {
            throw new BusinessException("请填写整改要求");
        }
        String status = normalizeStatus(project.getRectificationStatus());
        if (STATUS_REQUESTED.equals(status) || STATUS_SUBMITTED.equals(status)) {
            throw new BusinessException("该项目已有进行中的整改流程");
        }
        if (!CAN_REQUEST_FROM.contains(status)) {
            throw new BusinessException("当前状态不可发起整改");
        }
        project.setRectificationStatus(STATUS_REQUESTED);
        project.setRectificationNote(note.trim());
        project.setRectificationReply(null);
        project.setRectificationRequestedAt(LocalDateTime.now());
        project.setRectificationSubmittedAt(null);
        projectMapper.updateById(project);

        notificationService.notifyUser(project.getApplicantUserId(),
                "项目整改通知",
                "您的项目「" + project.getProjectName() + "」需要整改：" + note.trim(),
                "RECTIFICATION", projectId);
        auditService.log("REQUEST_RECTIFICATION", "PROJECT", projectId, note.trim());
        return toVo(project);
    }

    @Transactional
    public RectificationVO cancelRectification(Long taskId, Long projectId) {
        ReviewProject project = requireTaskProject(taskId, projectId);
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        if (!STATUS_REQUESTED.equals(normalizeStatus(project.getRectificationStatus()))) {
            throw new BusinessException("仅「待整改」状态可取消");
        }
        resetRectificationFields(project);
        projectMapper.updateById(project);
        if (project.getApplicantUserId() != null) {
            notificationService.notifyUser(project.getApplicantUserId(),
                    "整改已取消",
                    "项目「" + project.getProjectName() + "」的整改要求已由管理员取消。",
                    "RECTIFICATION", projectId);
        }
        auditService.log("CANCEL_RECTIFICATION", "PROJECT", projectId, "取消整改");
        return toVo(project);
    }

    @Transactional
    public ProjectRectificationMaterial uploadMaterial(Long projectId, MultipartFile file) {
        ReviewProject project = requireOwnProject(projectId);
        if (!STATUS_REQUESTED.equals(normalizeStatus(project.getRectificationStatus()))) {
            throw new BusinessException("当前不可上传整改材料");
        }
        String path = fileService.save(file, "rectification/" + projectId);
        ProjectRectificationMaterial material = new ProjectRectificationMaterial();
        material.setProjectId(projectId);
        material.setFileName(file.getOriginalFilename());
        material.setFilePath(path);
        material.setFileSize(file.getSize());
        material.setUploadedBy(SecurityUtils.currentUserId());
        material.setCreatedAt(LocalDateTime.now());
        rectMaterialMapper.insert(material);
        auditService.log("UPLOAD_RECTIFICATION", "PROJECT", projectId,
                "上传整改材料: " + material.getFileName());
        return material;
    }

    @Transactional
    public RectificationVO submitRectification(Long projectId, String reply) {
        ReviewProject project = requireOwnProject(projectId);
        if (!STATUS_REQUESTED.equals(normalizeStatus(project.getRectificationStatus()))) {
            throw new BusinessException("当前不可提交整改（状态：" + project.getRectificationStatus() + "）");
        }
        long materialCount = rectMaterialMapper.selectCount(new LambdaQueryWrapper<ProjectRectificationMaterial>()
                .eq(ProjectRectificationMaterial::getProjectId, projectId));
        if (!StringUtils.hasText(reply) && materialCount == 0) {
            throw new BusinessException("请填写整改说明或上传材料");
        }
        if (StringUtils.hasText(reply)) {
            project.setRectificationReply(reply.trim());
        }
        project.setRectificationStatus(STATUS_SUBMITTED);
        project.setRectificationSubmittedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        ReviewTask task = taskMapper.selectById(project.getTaskId());
        if (task != null && task.getCreatedBy() != null) {
            notificationService.notifyUser(task.getCreatedBy(),
                    "整改材料已提交",
                    "项目「" + project.getProjectName() + "」申报人已提交整改，请查看并启动复审。",
                    "RECTIFICATION", projectId);
        }
        auditService.log("SUBMIT_RECTIFICATION", "PROJECT", projectId, "申报人提交整改");
        return toVo(projectMapper.selectById(projectId));
    }

    @Transactional
    public RectificationVO startReReview(Long taskId, Long projectId) {
        ReviewProject project = requireTaskProject(taskId, projectId);
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        assertTaskNotArchived(task);
        if (!STATUS_SUBMITTED.equals(normalizeStatus(project.getRectificationStatus()))) {
            throw new BusinessException("请先等待申报人提交整改材料");
        }

        List<ReviewAssignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getProjectId, projectId));
        int notifyCount = 0;
        for (ReviewAssignment assignment : assignments) {
            if ("SUBMITTED".equals(assignment.getStatus()) || "ACCEPTED".equals(assignment.getStatus())) {
                assignment.setStatus("ACCEPTED");
                assignment.setSubmittedAt(null);
                assignmentMapper.updateById(assignment);
                ReviewSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<ReviewSubmission>()
                        .eq(ReviewSubmission::getAssignmentId, assignment.getId()));
                if (submission != null) {
                    int currentRound = project.getReviewRound() == null ? 1 : project.getReviewRound();
                    submissionArchiveService.archiveIfSubmitted(submission, currentRound);
                    submission.setStatus("DRAFT");
                    submission.setSubmittedAt(null);
                    submission.setVersion(submission.getVersion() + 1);
                    submissionMapper.updateById(submission);
                }
                notificationService.notifyExpert(assignment.getExpertId(),
                        "复审通知",
                        "项目「" + project.getProjectName() + "」已提交整改，请重新评审（第 "
                                + (project.getReviewRound() == null ? 2 : project.getReviewRound() + 1) + " 轮）。",
                        "RE_REVIEW", assignment.getId());
                notifyCount++;
            }
        }
        if (notifyCount == 0) {
            throw new BusinessException("该项目尚无已接受/已提交的专家分配，请先完成专家分配");
        }

        int round = project.getReviewRound() == null ? 1 : project.getReviewRound();
        project.setReviewRound(round + 1);
        project.setRectificationStatus(STATUS_RE_REVIEWING);
        projectMapper.updateById(project);

        if ("SUMMARIZED".equals(task.getStatus()) || "ARCHIVED".equals(task.getStatus())) {
            task.setStatus("IN_REVIEW");
            taskMapper.updateById(task);
        } else if ("UNASSIGNED".equals(task.getStatus()) || "PENDING_CONFIRM".equals(task.getStatus())) {
            task.setStatus("IN_REVIEW");
            taskMapper.updateById(task);
        }

        auditService.log("START_RE_REVIEW", "PROJECT", projectId,
                "启动第 " + project.getReviewRound() + " 轮复审，通知 " + notifyCount + " 位专家");
        return toVo(project);
    }

    /**
     * 复审轮次中，当项目再次达到最低评审人数时自动结束整改状态。
     */
    @Transactional
    public void tryCompleteReReview(Long projectId) {
        ReviewProject project = projectMapper.selectById(projectId);
        if (project == null || !STATUS_RE_REVIEWING.equals(normalizeStatus(project.getRectificationStatus()))) {
            return;
        }
        ReviewTask task = taskMapper.selectById(project.getTaskId());
        if (task == null) {
            return;
        }
        long submitted = assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getProjectId, projectId)
                .eq(ReviewAssignment::getStatus, "SUBMITTED"));
        if (submitted < task.getMinReviewers()) {
            return;
        }
        project.setRectificationStatus(STATUS_NONE);
        projectMapper.updateById(project);
        if (project.getApplicantUserId() != null) {
            notificationService.notifyUser(project.getApplicantUserId(),
                    "复审已完成",
                    "项目「" + project.getProjectName() + "」第 " + project.getReviewRound() + " 轮复审意见已收齐。",
                    "RE_REVIEW", projectId);
        }
        auditService.log("COMPLETE_RE_REVIEW", "PROJECT", projectId,
                "第 " + project.getReviewRound() + " 轮复审完成");
    }

    public ResponseEntity<Resource> downloadMaterial(Long projectId, Long materialId, boolean applicantAccess) {
        ReviewProject project = requireProject(projectId);
        if (applicantAccess) {
            dataScopeService.assertApplicantProject(project);
        } else {
            dataScopeService.assertTaskAccess(taskMapper.selectById(project.getTaskId()));
        }
        ProjectRectificationMaterial material = rectMaterialMapper.selectById(materialId);
        if (material == null || !projectId.equals(material.getProjectId())) {
            throw new BusinessException("材料不存在");
        }
        try {
            Path filePath = fileService.resolve(material.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new BusinessException("文件不存在");
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + encodeFilename(material.getFileName()))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("下载失败");
        }
    }

    public RectificationVO toVo(ReviewProject project) {
        if (project == null) {
            return null;
        }
        List<ProjectRectificationMaterial> materials = rectMaterialMapper.selectList(
                new LambdaQueryWrapper<ProjectRectificationMaterial>()
                        .eq(ProjectRectificationMaterial::getProjectId, project.getId())
                        .orderByDesc(ProjectRectificationMaterial::getCreatedAt));
        String status = normalizeStatus(project.getRectificationStatus());
        return RectificationVO.builder()
                .status(status)
                .reviewRound(project.getReviewRound() == null ? 1 : project.getReviewRound())
                .note(project.getRectificationNote())
                .reply(project.getRectificationReply())
                .requestedAt(project.getRectificationRequestedAt())
                .submittedAt(project.getRectificationSubmittedAt())
                .canSubmit(STATUS_REQUESTED.equals(status))
                .canCancel(STATUS_REQUESTED.equals(status))
                .canStartReReview(STATUS_SUBMITTED.equals(status))
                .materials(materials.stream().map(m -> RectificationVO.MaterialItem.builder()
                        .id(m.getId())
                        .fileName(m.getFileName())
                        .fileSize(m.getFileSize())
                        .createdAt(m.getCreatedAt())
                        .build()).toList())
                .build();
    }

    private void resetRectificationFields(ReviewProject project) {
        project.setRectificationStatus(STATUS_NONE);
        project.setRectificationNote(null);
        project.setRectificationReply(null);
        project.setRectificationRequestedAt(null);
        project.setRectificationSubmittedAt(null);
    }

    private ReviewTask requireTask(Long taskId) {
        ReviewTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    private void assertTaskNotArchived(ReviewTask task) {
        if ("ARCHIVED".equals(task.getStatus())) {
            throw new BusinessException("任务已归档，不可操作整改");
        }
    }

    private ReviewProject requireProject(Long projectId) {
        ReviewProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        return project;
    }

    private ReviewProject requireTaskProject(Long taskId, Long projectId) {
        ReviewProject project = requireProject(projectId);
        if (!project.getTaskId().equals(taskId)) {
            throw new BusinessException("项目与任务不匹配");
        }
        return project;
    }

    private ReviewProject requireOwnProject(Long projectId) {
        ReviewProject project = requireProject(projectId);
        dataScopeService.assertApplicantProject(project);
        return project;
    }

    private static String normalizeStatus(String status) {
        return status == null || status.isBlank() ? STATUS_NONE : status;
    }

    private static String encodeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        return java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
    }
}
