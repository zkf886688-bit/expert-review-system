package com.review.system.service;

import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewSubmission;
import com.review.system.entity.ReviewSubmissionArchive;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.*;
import com.review.system.security.DataScopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryRoundComparisonTest {

    @Mock private ReviewTaskMapper taskMapper;
    @Mock private ReviewProjectMapper projectMapper;
    @Mock private ReviewSubmissionMapper submissionMapper;
    @Mock private ReviewSubmissionArchiveMapper archiveMapper;
    @Mock private ReviewAssignmentMapper assignmentMapper;
    @Mock private ExpertMapper expertMapper;
    @Mock private AuditService auditService;
    @Mock private DataScopeService dataScopeService;

    @InjectMocks
    private SummaryService summaryService;

    @Test
    void roundComparisonMergesArchiveAndCurrentRound() {
        ReviewTask task = new ReviewTask();
        task.setId(1L);
        task.setMinReviewers(2);
        when(taskMapper.selectById(1L)).thenReturn(task);

        ReviewProject project = new ReviewProject();
        project.setId(100L);
        project.setTaskId(1L);
        project.setProjectName("示例项目");
        project.setReviewRound(2);
        when(projectMapper.selectList(any())).thenReturn(List.of(project));

        ReviewSubmissionArchive round1 = new ReviewSubmissionArchive();
        round1.setProjectId(100L);
        round1.setReviewRound(1);
        round1.setExpertName("张专家");
        round1.setTotalScore(new BigDecimal("85"));
        round1.setConclusion("推荐立项");
        round1.setOpinion("第一轮意见");
        when(archiveMapper.selectList(any())).thenReturn(List.of(round1));

        ReviewSubmission round2 = new ReviewSubmission();
        round2.setId(9L);
        round2.setProjectId(100L);
        round2.setExpertId(5L);
        round2.setStatus("SUBMITTED");
        round2.setTotalScore(new BigDecimal("90"));
        round2.setConclusion("推荐立项");
        round2.setOpinion("第二轮意见");
        when(submissionMapper.selectList(any())).thenReturn(List.of(round2));

        var list = summaryService.roundComparisonByTask(1L);
        assertEquals(1, list.size());
        assertEquals(2, list.get(0).getCurrentRound());
        assertEquals(2, list.get(0).getRounds().size());
        assertEquals(1, list.get(0).getRounds().get(0).getRound());
        assertEquals(new BigDecimal("85.00"), list.get(0).getRounds().get(0).getAvgScore());
        assertEquals(2, list.get(0).getRounds().get(1).getRound());
        assertEquals(new BigDecimal("90.00"), list.get(0).getRounds().get(1).getAvgScore());
    }
}
