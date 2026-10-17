package com.review.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.system.entity.SysConfig;
import com.review.system.mapper.SysConfigMapper;
import com.review.system.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpinionTemplateServiceTest {

    @Mock
    private SysConfigMapper configMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OpinionTemplateService service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listReturnsDefaultsWhenNoConfig() {
        loginAs(10L);
        when(configMapper.selectById("expert_opinion_templates:10")).thenReturn(null);

        var vo = service.listTemplates();
        assertFalse(vo.getTemplates().isEmpty());
        assertTrue(vo.getTemplates().get(0).contains("建议通过"));
    }

    @Test
    void savePersistsCustomTemplates() throws Exception {
        loginAs(20L);
        when(configMapper.selectById(any())).thenReturn(null);

        var saved = service.saveTemplates(List.of("自定义评语 A", "自定义评语 B"));
        assertEquals(2, saved.getTemplates().size());

        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);
        verify(configMapper).insert(captor.capture());
        assertEquals("expert_opinion_templates:20", captor.getValue().getConfigKey());
        assertTrue(captor.getValue().getConfigValue().contains("自定义评语 A"));
    }

    private void loginAs(Long userId) {
        LoginUser user = new LoginUser(userId, "expert1", null, 1L, List.of("EXPERT"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
