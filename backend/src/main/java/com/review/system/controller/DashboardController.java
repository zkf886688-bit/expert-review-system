package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','REVIEW_ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<DashboardService.DashboardVO> overview() {
        return Result.ok(dashboardService.overview());
    }

    @GetMapping("/tasks")
    public Result<List<DashboardService.TaskProgressVO>> taskProgress() {
        return Result.ok(dashboardService.taskProgress());
    }
}
