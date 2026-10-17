package com.review.system.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void opinionTemplatesRequireAuth() throws Exception {
        mockMvc.perform(get("/api/reviews/opinion-templates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roundComparisonRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/summary/tasks/1/round-comparison"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expertWorkbenchRequiresExpertRole() throws Exception {
        String token = IntegrationTestSupport.loginAndGetToken(mockMvc, "admin", "admin123");
        mockMvc.perform(get("/api/reviews/my?workbench=PENDING_ACCEPT")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
