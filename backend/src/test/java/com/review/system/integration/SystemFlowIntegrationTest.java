package com.review.system.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanReadSystemInfoAndTestSms() throws Exception {
        String token = IntegrationTestSupport.loginAndGetToken(mockMvc, "admin", "admin123");

        mockMvc.perform(get("/api/system/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mailEnabled").value(false))
                .andExpect(jsonPath("$.data.smsEnabled").value(false))
                .andExpect(jsonPath("$.data.smsProvider").value("mock"));

        mockMvc.perform(post("/api/system/test-sms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"content\":\"integration test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.provider").value("mock"));
    }

    @Test
    void nonAdminCannotAccessSystemInfo() throws Exception {
        String token = IntegrationTestSupport.loginAndGetToken(mockMvc, "leader", "leader123");

        mockMvc.perform(get("/api/system/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
