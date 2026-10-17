package com.review.system.controller;

import com.review.system.service.FileAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileAccessService fileAccessService;

    @GetMapping("/review-materials/{id}")
    public ResponseEntity<Resource> downloadReviewMaterial(@PathVariable Long id) {
        return fileAccessService.downloadReviewMaterial(id);
    }

    @GetMapping("/expert-materials/{id}")
    public ResponseEntity<Resource> downloadExpertMaterial(@PathVariable Long id) {
        return fileAccessService.downloadExpertMaterial(id);
    }

    @GetMapping("/submission-attachments/{id}")
    public ResponseEntity<Resource> downloadSubmissionAttachment(@PathVariable Long id) {
        return fileAccessService.downloadSubmissionAttachment(id);
    }

    /** 兼容旧前端 path 参数，仅管理员 */
    @GetMapping("/download")
    @SuppressWarnings("deprecation")
    public ResponseEntity<Resource> downloadLegacy(@RequestParam String path) {
        return fileAccessService.downloadByPath(path);
    }
}
