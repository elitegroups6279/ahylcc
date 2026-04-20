package com.hfnew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingScheduleService {

    private final FeeBillService feeBillService;

    /**
     * Automatically generate monthly billing drafts on the 1st of each month at 2:00 AM.
     * Generates bills for the previous month.
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyBills() {
        String lastMonth = YearMonth.now().minusMonths(1).toString();
        log.info("[BillingSchedule] Auto-generating draft bills for month: {}", lastMonth);
        try {
            int count = feeBillService.generateDraft(lastMonth);
            log.info("[BillingSchedule] Generated {} draft bills for {}", count, lastMonth);
        } catch (Exception e) {
            log.error("[BillingSchedule] Failed to generate bills for {}: {}", lastMonth, e.getMessage(), e);
        }
    }
}
