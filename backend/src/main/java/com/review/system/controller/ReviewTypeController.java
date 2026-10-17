package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.config.ReviewTypeRegistry;
import com.review.system.service.ReviewAnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review-types")
@RequiredArgsConstructor
public class ReviewTypeController {

    private final ReviewTypeRegistry reviewTypeRegistry;
    private final ReviewAnomalyService reviewAnomalyService;

    @GetMapping
    public Result<List<String>> list() {
        return Result.ok(reviewTypeRegistry.allTypes());
    }

    @GetMapping("/{type}/config")
    public Result<Map<String, Object>> config(@PathVariable String type) {
        return Result.ok(reviewTypeRegistry.configForType(type));
    }

    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyRole('ADMIN','REVIEW_ADMIN')")
    public Result<List<ReviewAnomalyService.AnomalyVO>> globalAnomalies() {
        return Result.ok(reviewAnomalyService.listGlobal());
    }

    @GetMapping("/tasks/{taskId}/anomalies")
    @PreAuthorize("hasAnyRole('ADMIN','REVIEW_ADMIN')")
    public Result<List<ReviewAnomalyService.AnomalyVO>> taskAnomalies(@PathVariable Long taskId) {
        return Result.ok(reviewAnomalyService.listForTask(taskId));
    }
}
