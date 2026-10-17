package com.review.system.service;

import com.review.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private SysConfigMapper configMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private NotificationTemplateService service;

    @Test
    void renderReplacesPlaceholders() {
        String result = service.render("Hello {name}, deadline {days} days",
                Map.of("name", "TaskA", "days", "3"));
        assertEquals("Hello TaskA, deadline 3 days", result);
    }

    @Test
    void renderHandlesNullValue() {
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("projectName", null);
        String result = service.render("Project {projectName}", vars);
        assertEquals("Project ", result);
    }
}
