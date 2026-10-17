package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.dto.ApplicantProjectVO;
import com.review.system.dto.ApplicantResultVO;
import com.review.system.entity.ReviewMaterial;
import com.review.system.dto.RectificationVO;
import com.review.system.service.ApplicantService;
import com.review.system.service.RectificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applicant")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantService applicantService;
    private final RectificationService rectificationService;

    @GetMapping("/projects")
    @PreAuthorize("hasRole('APPLICANT')")
    public Result<List<ApplicantProjectVO>> myProjects() {
        return Result.ok(applicantService.listMyProjects());
    }

    @GetMapping("/projects/{projectId}")
    @PreAuthorize("hasRole('APPLICANT')")
    public Result<ApplicantProjectVO> projectDetail(@PathVariable Long projectId) {
        return Result.ok(applicantService.projectDetail(projectId));
    }

    @GetMapping("/projects/{projectId}/result")
    @PreAuthorize("hasRole('APPLICANT')")
    public Result<ApplicantResultVO> projectResult(@PathVariable Long projectId) {
        return Result.ok(applicantService.getProjectResult(projectId));
    }

    @PostMapping("/tasks/{taskId}/materials")
    @PreAuthorize("hasRole('APPLICANT')")
    public Result<ReviewMaterial> uploadMaterial(@PathVariable Long taskId,
                                                  @RequestParam Long projectId,
                                                  @RequestParam("file") MultipartFile file) {
        return Result.ok(applicantService.uploadMaterial(taskId, projectId, file));
    }

    @PostMapping("/projects/{projectId}/rectification/submit")
    @PreAuthorize("hasRole('APPLICANT')")
    public Result<RectificationVO> submitRectification(@PathVariable Long projectId,
                                                         @RequestParam(required = false) String reply) {
        return Result.ok(rectificationService.submitRectification(projectId, reply));
    }

    @PostMapping("/projects/{projectId}/rectification/materials")
    @PreAuthorize("hasRole('APPLICANT')")
    public Result<com.review.system.entity.ProjectRectificationMaterial> uploadRectificationMaterial(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {
        return Result.ok(rectificationService.uploadMaterial(projectId, file));
    }

    @GetMapping("/projects/{projectId}/rectification/materials/{materialId}/download")
    @PreAuthorize("hasRole('APPLICANT')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadRectificationMaterial(
            @PathVariable Long projectId,
            @PathVariable Long materialId) {
        return rectificationService.downloadMaterial(projectId, materialId, true);
    }
}
