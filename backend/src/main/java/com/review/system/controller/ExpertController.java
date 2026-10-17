package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.dto.ExpertQuery;
import com.review.system.dto.ExpertSaveRequest;
import com.review.system.dto.PageResult;
import com.review.system.entity.Expert;
import com.review.system.entity.ExpertMaterial;
import com.review.system.service.ExpertAvoidanceService;
import com.review.system.service.ExpertMaterialService;
import com.review.system.service.ExpertService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/experts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','REVIEW_ADMIN')")
public class ExpertController {

    private final ExpertService expertService;
    private final ExpertMaterialService expertMaterialService;
    private final ExpertAvoidanceService expertAvoidanceService;

    @GetMapping
    public Result<PageResult<Expert>> page(ExpertQuery query) {
        return Result.ok(expertService.page(query));
    }

    @GetMapping("/pending")
    public Result<PageResult<Expert>> pending(ExpertQuery query) {
        query.setStatus("PENDING");
        return Result.ok(expertService.page(query));
    }

    @GetMapping("/{id}")
    public Result<Expert> detail(@PathVariable Long id) {
        return Result.ok(expertService.detail(id));
    }

    @PostMapping
    public Result<Expert> create(@Valid @RequestBody ExpertSaveRequest req) {
        return Result.ok(expertService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Expert> update(@PathVariable Long id, @Valid @RequestBody ExpertSaveRequest req) {
        return Result.ok(expertService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        expertService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/account")
    public Result<Void> createAccount(@PathVariable Long id, @RequestBody AccountRequest req) {
        expertService.createAccount(id, req.getUsername(), req.getPassword());
        return Result.ok();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(ExpertQuery query) {
        byte[] data = expertService.exportExcel(query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=experts.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        byte[] data = expertService.template();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expert_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/import")
    public Result<List<String>> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.ok(expertService.importExcel(file));
    }

    @GetMapping("/{id}/materials")
    public Result<List<ExpertMaterial>> materials(@PathVariable Long id) {
        return Result.ok(expertMaterialService.list(id));
    }

    @PostMapping("/{id}/materials")
    public Result<ExpertMaterial> uploadMaterial(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(required = false) String materialType) {
        return Result.ok(expertMaterialService.upload(id, file, materialType));
    }

    @DeleteMapping("/{id}/materials/{materialId}")
    public Result<Void> deleteMaterial(@PathVariable Long id, @PathVariable Long materialId) {
        expertMaterialService.delete(id, materialId);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    public Result<Expert> approve(@PathVariable Long id, @RequestBody(required = false) ApprovalRequest req) {
        String remark = req != null ? req.getRemark() : null;
        return Result.ok(expertService.approve(id, remark));
    }

    @PostMapping("/{id}/reject")
    public Result<Expert> reject(@PathVariable Long id, @RequestBody(required = false) ApprovalRequest req) {
        String remark = req != null ? req.getRemark() : null;
        return Result.ok(expertService.reject(id, remark));
    }

    @GetMapping("/{id}/avoidances")
    public Result<List<com.review.system.entity.ExpertAvoidance>> avoidances(@PathVariable Long id) {
        return Result.ok(expertAvoidanceService.listByExpert(id));
    }

    @PostMapping("/{id}/avoidances")
    public Result<com.review.system.entity.ExpertAvoidance> addAvoidance(@PathVariable Long id,
                                                                         @RequestBody AvoidanceRequest req) {
        return Result.ok(expertAvoidanceService.add(id, req.getAvoidType(), req.getAvoidValue(), req.getReason()));
    }

    @DeleteMapping("/{id}/avoidances/{avoidanceId}")
    public Result<Void> deleteAvoidance(@PathVariable Long id, @PathVariable Long avoidanceId) {
        expertAvoidanceService.delete(id, avoidanceId);
        return Result.ok();
    }

    @Data
    public static class ApprovalRequest {
        private String remark;
    }

    @Data
    public static class AvoidanceRequest {
        private String avoidType;
        private String avoidValue;
        private String reason;
    }

    @Data
    public static class AccountRequest {
        private String username;
        private String password;
    }
}
