package com.review.system.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileDownloadSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pathTraversalRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/files/review-materials/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyPathDownloadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/files/download").param("path", "../application.yml"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pathTraversalRejectedForAuthenticatedUser() throws Exception {
        String token = IntegrationTestSupport.loginAndGetToken(mockMvc, "admin", "admin123");

        mockMvc.perform(get("/api/files/download")
                        .param("path", "../application.yml")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("非法路径"));
    }

    @Test
    void missingFileReturnsBusinessError() throws Exception {
        String token = IntegrationTestSupport.loginAndGetToken(mockMvc, "admin", "admin123");

        mockMvc.perform(get("/api/files/download")
                        .param("path", "missing/file.txt")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文件不存在"));
    }
}
