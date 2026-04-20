package com.hfnew.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = {"report:export"})
    void exportFeeSummary_shouldReturnExcel() throws Exception {
        mockMvc.perform(get("/api/reports/fee-summary.xlsx").param("month", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0));
    }

    @Test
    @WithMockUser(authorities = {"report:export"})
    void exportRoster_shouldReturnExcel() throws Exception {
        mockMvc.perform(get("/api/reports/roster.xlsx").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0));
    }

    @Test
    @WithMockUser(authorities = {"report:export"})
    void exportAttendance_shouldReturnExcel() throws Exception {
        mockMvc.perform(get("/api/reports/attendance.xlsx").param("startDate", "2026-04-01").param("endDate", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0));
    }

    @Test
    @WithMockUser(authorities = {"report:export"})
    void exportMaterialConsumption_shouldReturnExcel() throws Exception {
        mockMvc.perform(get("/api/reports/material-consumption.xlsx").param("startDate", "2026-04-01").param("endDate", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0));
    }
}

