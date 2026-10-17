package com.review.system.service;

import com.review.system.common.BusinessException;
import com.review.system.dto.BatchRandomAssignRequest;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.*;
import com.review.system.security.DataScopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewTaskServiceBatchTest {

    @Mock private ReviewTaskMapper taskMapper;
    @Mock private ReviewProjectMapper projectMapper;
    @Mock private ReviewMaterialMapper materialMapper;
    @Mock private ReviewAssignmentMapper assignmentMapper;
    @Mock private ExpertMapper expertMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private FileService fileService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private NotificationTemplateService notificationTemplateService;
    @Mock private DataScopeService dataScopeService;

    @InjectMocks
    private ReviewTaskService reviewTaskService;

    @Test
    void batchRandomAssignRejectsArchivedTask() {
        ReviewTask task = new ReviewTask();
        task.setId(1L);
        task.setStatus("ARCHIVED");
        when(taskMapper.selectById(1L)).thenReturn(task);

        BatchRandomAssignRequest req = new BatchRandomAssignRequest();
        assertThrows(BusinessException.class, () -> reviewTaskService.batchRandomAssign(1L, req));
        verify(projectMapper, never()).selectList(any());
    }

    @Test
    void batchRandomAssignRejectsWhenAllProjectsSkipped() {
        ReviewTask task = new ReviewTask();
        task.setId(1L);
        task.setStatus("UNASSIGNED");
        when(taskMapper.selectById(1L)).thenReturn(task);

        ReviewProject p = new ReviewProject();
        p.setId(10L);
        p.setProjectName("项目A");
        when(projectMapper.selectList(any())).thenReturn(List.of(p));
        when(assignmentMapper.selectCount(any())).thenReturn(3L);

        BatchRandomAssignRequest req = new BatchRandomAssignRequest();
        req.setCount(3);
        req.setSkipAssigned(true);

        assertThrows(BusinessException.class, () -> reviewTaskService.batchRandomAssign(1L, req));
    }

    @Test
    void batchRandomAssignReturnsEmptyProjectsError() {
        ReviewTask task = new ReviewTask();
        task.setId(1L);
        task.setStatus("UNASSIGNED");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(projectMapper.selectList(any())).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> reviewTaskService.batchRandomAssign(1L, new BatchRandomAssignRequest()));
    }
}
