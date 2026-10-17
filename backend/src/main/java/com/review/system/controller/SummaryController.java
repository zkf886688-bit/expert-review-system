package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.service.SummaryService;
import com.review.system.util.FileDownloadHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','REVIEW_ADMIN','LEADER')")
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/tasks/{taskId}")
    public Result<List<SummaryService.ProjectSummaryVO>> summarize(
            @PathVariable Long taskId,
            @RequestParam(required = false) Boolean masked) {
        return Result.ok(summaryService.summarizeByTask(taskId, masked));
    }

    @GetMapping("/tasks/{taskId}/round-comparison")
    public Result<List<SummaryService.ProjectRoundComparisonVO>> roundComparison(@PathVariable Long taskId) {
        return Result.ok(summaryService.roundComparisonByTask(taskId));
    }

    @GetMapping("/tasks/{taskId}/dimensions")
    public Result<List<SummaryService.DimensionSummaryVO>> dimensions(@PathVariable Long taskId) {
        return Result.ok(summaryService.summarizeDimensions(taskId));
    }

    @GetMapping("/tasks/{taskId}/export")
    public ResponseEntity<byte[]> exportExcel(
            @PathVariable Long taskId,
            @RequestParam(required = false) Boolean masked) {
        return FileDownloadHelper.attachment(summaryService.exportExcel(taskId, masked),
                "summary_" + taskId + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/tasks/{taskId}/export/word")
    public ResponseEntity<byte[]> exportWord(
            @PathVariable Long taskId,
            @RequestParam(required = false) Boolean masked) {
        return FileDownloadHelper.attachment(summaryService.exportWord(taskId, masked),
                "summary_" + taskId + ".docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @GetMapping("/tasks/{taskId}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Long taskId,
            @RequestParam(required = false) Boolean masked) {
        return FileDownloadHelper.attachment(summaryService.exportPdf(taskId, masked),
                "summary_" + taskId + ".pdf",
                MediaType.APPLICATION_PDF_VALUE);
    }
}
