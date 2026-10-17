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
class PublicHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicHealthReturnsAppMetadata() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.app").exists())
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.phase").value("19"))
                .andExpect(jsonPath("$.components").exists())
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void welcomePageListsHealthEndpoints() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value("/api/public/health"))
                .andExpect(jsonPath("$.actuatorHealth").value("/actuator/health"));
    }
}
