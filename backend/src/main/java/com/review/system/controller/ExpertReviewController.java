package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.dto.ExpertAssignmentVO;
import com.review.system.dto.InviteResponseRequest;
import com.review.system.dto.OpinionTemplateUpdateRequest;
import com.review.system.dto.OpinionTemplateVO;
import com.review.system.dto.ReviewSubmissionVO;
import com.review.system.dto.ReviewSubmitRequest;
import com.review.system.service.ExpertReviewService;
import com.review.system.service.OpinionTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXPERT')")
public class ExpertReviewController {

    private final ExpertReviewService expertReviewService;
    private final OpinionTemplateService opinionTemplateService;

    @GetMapping("/my")
    public Result<List<ExpertAssignmentVO>> myAssignments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String workbench) {
        return Result.ok(expertReviewService.myAssignments(status, workbench));
    }

    @GetMapping("/opinion-templates")
    public Result<OpinionTemplateVO> listOpinionTemplates() {
        return Result.ok(opinionTemplateService.listTemplates());
    }

    @PutMapping("/opinion-templates")
    public Result<OpinionTemplateVO> saveOpinionTemplates(@Valid @RequestBody OpinionTemplateUpdateRequest req) {
        return Result.ok(opinionTemplateService.saveTemplates(req.getTemplates()));
    }

    @GetMapping("/my/history")
    public Result<List<ExpertAssignmentVO>> history() {
        return Result.ok(expertReviewService.myHistory());
    }

    @GetMapping("/assignments/{id}")
    public Result<ExpertAssignmentVO> detail(@PathVariable Long id) {
        return Result.ok(expertReviewService.assignmentDetail(id));
    }

    @PostMapping("/respond")
    public Result<Void> respond(@Valid @RequestBody InviteResponseRequest req) {
        expertReviewService.respondInvite(req);
        return Result.ok();
    }

    @PostMapping("/submit")
    public Result<ReviewSubmissionVO> submit(@Valid @RequestBody ReviewSubmitRequest req) {
        return Result.ok(expertReviewService.saveReview(req));
    }

    @PostMapping("/assignments/{id}/attachment")
    public Result<ReviewSubmissionVO> uploadAttachment(@PathVariable Long id,
                                                       @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return Result.ok(expertReviewService.uploadAttachment(id, file));
    }

    @PostMapping("/assignments/{id}/return")
    public Result<Void> returnReview(@PathVariable Long id) {
        expertReviewService.returnForRevision(id);
        return Result.ok();
    }
}
