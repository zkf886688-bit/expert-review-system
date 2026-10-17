package com.review.system.service;

import com.review.system.entity.Expert;
import com.review.system.entity.ReviewAssignment;
import com.review.system.mapper.ExpertMapper;
import com.review.system.mapper.ReviewAssignmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpertInactiveServiceTest {

    @Mock
    private ExpertMapper expertMapper;
    @Mock
    private ReviewAssignmentMapper assignmentMapper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ExpertInactiveService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "inactiveYears", 3);
    }

    @Test
    void disablesExpertWithoutRecentActivity() {
        Expert expert = activeExpert(1L, LocalDateTime.now().minusYears(5));
        ReviewAssignment assignment = assignment(1L, LocalDateTime.now().minusYears(4));
        when(expertMapper.selectList(any())).thenReturn(List.of(expert));
        when(assignmentMapper.selectList(any())).thenReturn(List.of(assignment));

        int count = service.markInactiveExperts();

        assertEquals(1, count);
        ArgumentCaptor<Expert> captor = ArgumentCaptor.forClass(Expert.class);
        verify(expertMapper).updateById(captor.capture());
        assertEquals("DISABLED", captor.getValue().getStatus());
        verify(auditService).log(eq("AUTO_DISABLE_EXPERT"), eq("EXPERT"), eq(1L), anyString());
    }

    @Test
    void keepsRecentlyActiveExpert() {
        Expert expert = activeExpert(2L, LocalDateTime.now().minusYears(5));
        ReviewAssignment assignment = assignment(2L, LocalDateTime.now().minusMonths(6));
        when(expertMapper.selectList(any())).thenReturn(List.of(expert));
        when(assignmentMapper.selectList(any())).thenReturn(List.of(assignment));

        int count = service.markInactiveExperts();

        assertEquals(0, count);
        verify(expertMapper, never()).updateById(any());
    }

    @Test
    void disablesNeverAssignedExpertCreatedLongAgo() {
        Expert expert = activeExpert(3L, LocalDateTime.now().minusYears(4));
        when(expertMapper.selectList(any())).thenReturn(List.of(expert));
        when(assignmentMapper.selectList(any())).thenReturn(List.of());

        int count = service.markInactiveExperts();

        assertEquals(1, count);
        verify(expertMapper).updateById(argThat(e -> "DISABLED".equals(e.getStatus())));
    }

    @Test
    void skipsWhenDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);

        int count = service.markInactiveExperts();

        assertEquals(0, count);
        verify(expertMapper, never()).selectList(any());
    }

    @Test
    void latestActivityUsesMostRecentTimestamp() {
        ReviewAssignment assignment = new ReviewAssignment();
        assignment.setInvitedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        assignment.setRespondedAt(LocalDateTime.of(2021, 6, 1, 0, 0));
        assignment.setSubmittedAt(LocalDateTime.of(2022, 3, 1, 0, 0));

        LocalDateTime latest = ExpertInactiveService.latestActivity(assignment);

        assertEquals(LocalDateTime.of(2022, 3, 1, 0, 0), latest);
    }

    private static Expert activeExpert(Long id, LocalDateTime createdAt) {
        Expert expert = new Expert();
        expert.setId(id);
        expert.setName("专家" + id);
        expert.setStatus("ACTIVE");
        expert.setCreatedAt(createdAt);
        return expert;
    }

    private static ReviewAssignment assignment(Long expertId, LocalDateTime invitedAt) {
        ReviewAssignment assignment = new ReviewAssignment();
        assignment.setExpertId(expertId);
        assignment.setInvitedAt(invitedAt);
        return assignment;
    }
}
