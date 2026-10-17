package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.entity.*;
import com.review.system.mapper.*;
import com.review.system.security.DataScopeService;
import com.review.system.security.LoginUser;
import com.review.system.security.RoleUtils;
import com.review.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class FileAccessService {

    private final FileService fileService;
    private final ReviewMaterialMapper reviewMaterialMapper;
    private final ExpertMaterialMapper expertMaterialMapper;
    private final ReviewSubmissionMapper submissionMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewTaskMapper taskMapper;
    private final ReviewProjectMapper projectMapper;
    private final DataScopeService dataScopeService;

    public ResponseEntity<Resource> downloadReviewMaterial(Long materialId) {
        ReviewMaterial material = reviewMaterialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(404, "材料不存在");
        }
        assertCanAccessReviewMaterial(material);
        return buildDownload(material.getFilePath(), material.getFileName());
    }

    public ResponseEntity<Resource> downloadExpertMaterial(Long materialId) {
        ExpertMaterial material = expertMaterialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(404, "材料不存在");
        }
        if (!RoleUtils.isFullAdmin()) {
            throw new BusinessException(403, "无权下载专家资质材料");
        }
        return buildDownload(material.getFilePath(), material.getFileName());
    }

    public ResponseEntity<Resource> downloadSubmissionAttachment(Long submissionId) {
        ReviewSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null || submission.getAttachmentPath() == null) {
            throw new BusinessException(404, "附件不存在");
        }
        LoginUser user = SecurityUtils.currentUser();
        if (RoleUtils.isFullAdmin() || RoleUtils.isLeader()) {
            if (RoleUtils.hasRole("REVIEW_ADMIN") && !RoleUtils.isSystemAdmin()) {
                ReviewTask task = taskMapper.selectById(submission.getTaskId());
                if (task != null) {
                    dataScopeService.assertTaskAccess(task);
                }
            }
        } else if (user.getExpertId() != null && user.getExpertId().equals(submission.getExpertId())) {
            // expert owns submission
        } else {
            throw new BusinessException(403, "无权下载该附件");
        }
        return buildDownload(submission.getAttachmentPath(),
                submission.getAttachmentName() != null ? submission.getAttachmentName() : "attachment");
    }

    /** @deprecated 仅兼容旧链接，管理员可用 */
    public ResponseEntity<Resource> downloadByPath(String path) {
        if (!RoleUtils.isFullAdmin()) {
            throw new BusinessException(403, "请使用材料编号下载");
        }
        Path filePath = fileService.resolve(path);
        return buildDownload(path, filePath.getFileName().toString());
    }

    private void assertCanAccessReviewMaterial(ReviewMaterial material) {
        LoginUser user = SecurityUtils.currentUser();
        if (RoleUtils.isFullAdmin()) {
            ReviewTask task = taskMapper.selectById(material.getTaskId());
            if (task != null) {
                dataScopeService.assertTaskAccess(task);
            }
            return;
        }
        if (user.getExpertId() != null) {
            Long count = assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                    .eq(ReviewAssignment::getExpertId, user.getExpertId())
                    .eq(ReviewAssignment::getTaskId, material.getTaskId())
                    .eq(material.getProjectId() != null, ReviewAssignment::getProjectId, material.getProjectId())
                    .in(ReviewAssignment::getStatus, "ACCEPTED", "SUBMITTED", "OVERDUE"));
            if (count == 0) {
                throw new BusinessException(403, "无权下载该材料");
            }
            return;
        }
        if (RoleUtils.isApplicant() && material.getProjectId() != null) {
            ReviewProject project = projectMapper.selectById(material.getProjectId());
            if (project == null || !user.getUserId().equals(project.getApplicantUserId())) {
                throw new BusinessException(403, "无权下载该材料");
            }
            return;
        }
        throw new BusinessException(403, "无权下载该材料");
    }

    private ResponseEntity<Resource> buildDownload(String relativePath, String filename) {
        try {
            Path filePath = fileService.resolve(relativePath);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new BusinessException(404, "文件不存在");
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("下载失败");
        }
    }
}
