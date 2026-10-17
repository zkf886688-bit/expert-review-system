package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.dto.CreditRateRequest;
import com.review.system.entity.ExpertCreditRecord;
import com.review.system.service.ExpertCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','REVIEW_ADMIN')")
public class ExpertCreditController {

    private final ExpertCreditService expertCreditService;

    @GetMapping("/experts/{expertId}")
    public Result<List<ExpertCreditRecord>> list(@PathVariable Long expertId) {
        return Result.ok(expertCreditService.listByExpert(expertId));
    }

    @PostMapping("/rate")
    public Result<Void> rate(@Valid @RequestBody CreditRateRequest req) {
        expertCreditService.rateQuality(req);
        return Result.ok();
    }
}
