package com.hfnew.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = {"finance:reimbursement"})
    void summary_shouldReturnCounts() throws Exception {
        mockMvc.perform(get("/api/notifications/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pendingReimbursementCount").exists())
                .andExpect(jsonPath("$.data.stockWarningCount").exists())
                .andExpect(jsonPath("$.data.drugExpiryWarningCount").exists())
                .andExpect(jsonPath("$.data.contractExpiringCount").exists())
                .andExpect(jsonPath("$.data.feeWarningCount").exists());
    }
}

