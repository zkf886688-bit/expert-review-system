package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.dto.*;
import com.review.system.entity.ReviewMaterial;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewTask;
import com.review.system.service.RectificationService;
import com.review.system.service.ReviewTaskService;
import jakarta.validation.Valid;
import com.review.system.util.FileDownloadHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','REVIEW_ADMIN')")
public class ReviewTaskController {

    private final ReviewTaskService reviewTaskService;
    private final RectificationService rectificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','REVIEW_ADMIN','LEADER')")
    public Result<PageResult<TaskListVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(reviewTaskService.page(page, size, status, keyword));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return FileDownloadHelper.attachment(reviewTaskService.exportTaskList(status, keyword),
                "review_tasks.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @PostMapping
    public Result<ReviewTask> create(@Valid @RequestBody TaskCreateRequest req) {
        return Result.ok(reviewTaskService.create(req));
    }

    @GetMapping("/{id}")
    public Result<TaskDetailVO> detail(@PathVariable Long id) {
        return Result.ok(reviewTaskService.detail(id));
    }

    @PostMapping("/{id}/projects")
    public Result<ReviewProject> addProject(@PathVariable Long id,
                                            @Valid @RequestBody TaskCreateRequest.ProjectItem item) {
        return Result.ok(reviewTaskService.addProject(id, item));
    }

    @PostMapping("/{id}/materials")
    public Result<ReviewMaterial> uploadMaterial(@PathVariable Long id,
                                                 @RequestParam(required = false) Long projectId,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(required = false) String materialType) {
        return Result.ok(reviewTaskService.uploadMaterial(id, projectId, file, materialType));
    }

    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        reviewTaskService.assignExperts(id, req);
        return Result.ok();
    }

    @PostMapping("/{id}/random-assign")
    public Result<List<Long>> randomAssign(@PathVariable Long id, @Valid @RequestBody RandomAssignRequest req) {
        return Result.ok(reviewTaskService.randomAssign(id, req));
    }

    @PostMapping("/{id}/batch-random-assign")
    public Result<BatchRandomAssignResultVO> batchRandomAssign(@PathVariable Long id,
                                                               @Valid @RequestBody BatchRandomAssignRequest req) {
        return Result.ok(reviewTaskService.batchRandomAssign(id, req));
    }

    @PutMapping("/{id}/deadline")
    public Result<ReviewTask> updateDeadline(@PathVariable Long id, @Valid @RequestBody TaskDeadlineRequest req) {
        return Result.ok(reviewTaskService.updateDeadline(id, req));
    }

    @PutMapping("/{id}/form-config")
    public Result<ReviewTask> updateFormConfig(@PathVariable Long id,
                                                @Valid @RequestBody TaskFormConfigUpdateRequest req) {
        return Result.ok(reviewTaskService.updateFormConfig(id, req));
    }

    @PostMapping("/{id}/remind-pending")
    public Result<RemindResultVO> remindPending(@PathVariable Long id,
                                                 @RequestBody(required = false) RemindRequest req) {
        return Result.ok(reviewTaskService.remindPending(id, req));
    }

    @GetMapping("/{id}/assignments/export")
    public ResponseEntity<byte[]> exportAssignments(@PathVariable Long id) {
        return FileDownloadHelper.attachment(reviewTaskService.exportAssignments(id),
                "task_" + id + "_assignments.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @PostMapping("/{id}/summarize")
    public Result<Void> summarize(@PathVariable Long id) {
        reviewTaskService.summarize(id);
        return Result.ok();
    }

    @PostMapping("/{id}/archive")
    public Result<Void> archive(@PathVariable Long id) {
        reviewTaskService.archive(id);
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        reviewTaskService.publish(id);
        return Result.ok();
    }

    @PostMapping("/{id}/clone")
    public Result<ReviewTask> clone(@PathVariable Long id, @Valid @RequestBody TaskCloneRequest req) {
        return Result.ok(reviewTaskService.cloneTask(id, req));
    }

    @GetMapping("/projects/import-template")
    public ResponseEntity<byte[]> projectImportTemplate() {
        return FileDownloadHelper.attachment(reviewTaskService.projectImportTemplate(),
                "project_import_template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @PostMapping("/{id}/projects/import")
    public Result<ImportResultVO> importProjects(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file) {
        return Result.ok(reviewTaskService.importProjects(id, file));
    }

    @GetMapping("/{id}/projects/export")
    public ResponseEntity<byte[]> exportProjects(@PathVariable Long id) {
        return FileDownloadHelper.attachment(reviewTaskService.exportProjects(id),
                "task_" + id + "_projects.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @PutMapping("/{taskId}/projects/{projectId}/applicant")
    public Result<Void> bindApplicant(@PathVariable Long taskId,
                                      @PathVariable Long projectId,
                                      @RequestBody BindApplicantRequest req) {
        reviewTaskService.bindApplicant(taskId, projectId, req.getApplicantUserId());
        return Result.ok();
    }

    @PostMapping("/{taskId}/projects/{projectId}/rectification/request")
    public Result<RectificationVO> requestRectification(@PathVariable Long taskId,
                                                        @PathVariable Long projectId,
                                                        @Valid @RequestBody RectificationRequestDto req) {
        return Result.ok(rectificationService.requestRectification(taskId, projectId, req.getNote()));
    }

    @PostMapping("/{taskId}/projects/{projectId}/rectification/start-rereview")
    public Result<RectificationVO> startReReview(@PathVariable Long taskId,
                                                 @PathVariable Long projectId) {
        return Result.ok(rectificationService.startReReview(taskId, projectId));
    }

    @PostMapping("/{taskId}/projects/{projectId}/rectification/cancel")
    public Result<RectificationVO> cancelRectification(@PathVariable Long taskId,
                                                       @PathVariable Long projectId) {
        return Result.ok(rectificationService.cancelRectification(taskId, projectId));
    }

    @GetMapping("/{taskId}/projects/{projectId}/rectification/materials/{materialId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadRectificationMaterial(
            @PathVariable Long taskId,
            @PathVariable Long projectId,
            @PathVariable Long materialId) {
        return rectificationService.downloadMaterial(projectId, materialId, false);
    }

    @GetMapping("/{taskId}/projects/{projectId}/rectification")
    public Result<RectificationVO> getRectification(@PathVariable Long taskId,
                                                    @PathVariable Long projectId) {
        return Result.ok(rectificationService.getRectification(taskId, projectId));
    }
}
