package com.review.system.service;

import com.review.system.entity.Expert;
import com.review.system.mapper.ReviewAssignmentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpertAssignmentScorerTest {

    @Mock
    private ReviewAssignmentMapper assignmentMapper;

    @InjectMocks
    private ExpertAssignmentScorer scorer;

    @Test
    void picksRequestedCount() {
        when(assignmentMapper.selectList(any())).thenReturn(List.of());
        Expert a = expert(1L);
        Expert b = expert(2L);
        List<Long> picked = scorer.pickWeighted(List.of(a, b), 1);
        assertEquals(1, picked.size());
    }

    private static Expert expert(Long id) {
        Expert e = new Expert();
        e.setId(id);
        e.setOnTimeRate(BigDecimal.valueOf(90));
        e.setReviewCount(3);
        return e;
    }
}
