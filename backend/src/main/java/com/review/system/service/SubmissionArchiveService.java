package com.review.system.service;

import com.review.system.entity.Expert;
import com.review.system.entity.ReviewSubmission;
import com.review.system.entity.ReviewSubmissionArchive;
import com.review.system.mapper.ExpertMapper;
import com.review.system.mapper.ReviewSubmissionArchiveMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubmissionArchiveService {

    private final ReviewSubmissionArchiveMapper archiveMapper;
    private final ExpertMapper expertMapper;

    public void archiveIfSubmitted(ReviewSubmission submission, int reviewRound) {
        if (submission == null || !"SUBMITTED".equals(submission.getStatus())) {
            return;
        }
        Expert expert = expertMapper.selectById(submission.getExpertId());
        ReviewSubmissionArchive archive = new ReviewSubmissionArchive();
        archive.setProjectId(submission.getProjectId());
        archive.setReviewRound(reviewRound);
        archive.setAssignmentId(submission.getAssignmentId());
        archive.setExpertId(submission.getExpertId());
        archive.setExpertName(expert != null ? expert.getName() : "专家");
        archive.setScores(submission.getScores());
        archive.setTotalScore(submission.getTotalScore());
        archive.setOpinion(submission.getOpinion());
        archive.setConclusion(submission.getConclusion());
        archive.setSubmittedAt(submission.getSubmittedAt());
        archive.setArchivedAt(LocalDateTime.now());
        archiveMapper.insert(archive);
    }
}
