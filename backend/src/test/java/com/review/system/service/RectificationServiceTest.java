package com.review.system.service;

import com.review.system.common.BusinessException;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.ProjectRectificationMaterialMapper;
import com.review.system.mapper.ReviewAssignmentMapper;
import com.review.system.mapper.ReviewProjectMapper;
import com.review.system.mapper.ReviewSubmissionMapper;
import com.review.system.mapper.ReviewTaskMapper;
import com.review.system.security.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RectificationServiceTest {

    @Mock
    private ReviewProjectMapper projectMapper;
    @Mock
    private ReviewTaskMapper taskMapper;
    @Mock
    private ReviewAssignmentMapper assignmentMapper;
    @Mock
    private ReviewSubmissionMapper submissionMapper;
    @Mock
    private ProjectRectificationMaterialMapper rectMaterialMapper;
    @Mock
    private FileService fileService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private RectificationService service;

    private ReviewProject project;
    private ReviewTask task;

    @BeforeEach
    void setUp() {
        project = new ReviewProject();
        project.setId(1L);
        project.setTaskId(10L);
        project.setProjectName("测试项目");
        project.setApplicantUserId(5L);
        project.setRectificationStatus(RectificationService.STATUS_NONE);
        project.setReviewRound(1);

        task = new ReviewTask();
        task.setId(10L);
        task.setStatus("IN_REVIEW");
        task.setCreatedBy(2L);
    }

    @Test
    void requestRectificationSetsRequestedStatus() {
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(taskMapper.selectById(10L)).thenReturn(task);

        var vo = service.requestRectification(10L, 1L, "请补充预算说明");

        assertEquals(RectificationService.STATUS_REQUESTED, vo.getStatus());
        verify(projectMapper).updateById(any(ReviewProject.class));
        verify(notificationService).notifyUser(eq(5L), anyString(), anyString(), eq("RECTIFICATION"), eq(1L));
    }

    @Test
    void requestRectificationAllowedAfterReReviewing() {
        project.setRectificationStatus(RectificationService.STATUS_RE_REVIEWING);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(taskMapper.selectById(10L)).thenReturn(task);

        var vo = service.requestRectification(10L, 1L, "第二轮整改");

        assertEquals(RectificationService.STATUS_REQUESTED, vo.getStatus());
    }

    @Test
    void requestRectificationRejectsWhenAlreadyRequested() {
        project.setRectificationStatus(RectificationService.STATUS_REQUESTED);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(taskMapper.selectById(10L)).thenReturn(task);

        assertThrows(BusinessException.class,
                () -> service.requestRectification(10L, 1L, "again"));
    }

    @Test
    void startReReviewRequiresSubmittedStatus() {
        project.setRectificationStatus(RectificationService.STATUS_REQUESTED);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(taskMapper.selectById(10L)).thenReturn(task);

        assertThrows(BusinessException.class,
                () -> service.startReReview(10L, 1L));
    }

    @Test
    void cancelOnlyWhenRequested() {
        project.setRectificationStatus(RectificationService.STATUS_SUBMITTED);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(taskMapper.selectById(10L)).thenReturn(task);

        assertThrows(BusinessException.class,
                () -> service.cancelRectification(10L, 1L));
    }
}
