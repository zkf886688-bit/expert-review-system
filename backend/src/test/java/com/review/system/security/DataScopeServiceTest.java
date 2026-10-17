package com.review.system.security;

import com.review.system.entity.SysUser;
import com.review.system.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataScopeServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private DataScopeService dataScopeService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void globalAdminSeesAllDomains() {
        setUser(1L, List.of("ADMIN", "REVIEW_ADMIN"));
        assertTrue(dataScopeService.isGlobalAdmin());
        assertFalse(dataScopeService.needsDomainFilter());
    }

    @Test
    void reviewAdminFilteredByDomain() {
        setUser(2L, List.of("REVIEW_ADMIN"));
        SysUser user = new SysUser();
        user.setDataDomain("DOMAIN_A");
        when(sysUserMapper.selectById(2L)).thenReturn(user);

        assertTrue(dataScopeService.needsDomainFilter());
        assertEquals("DOMAIN_A", dataScopeService.currentDataDomain());
        assertEquals("DOMAIN_A", dataScopeService.resolveTaskDomain("OTHER"));
    }

    @Test
    void adminCanSetTaskDomain() {
        setUser(1L, List.of("ADMIN"));
        assertEquals("CUSTOM", dataScopeService.resolveTaskDomain("CUSTOM"));
    }

    private void setUser(Long userId, List<String> roles) {
        LoginUser loginUser = new LoginUser(userId, "test", "pwd", null, roles);
        var auth = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
