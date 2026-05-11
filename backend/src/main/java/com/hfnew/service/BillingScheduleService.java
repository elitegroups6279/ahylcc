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
     * Flow:
     * 1. Auto-settle all CONFIRMED bills from any previous month
     * 2. Generate new DRAFT bills for the previous month
     * 3. Auto-confirm the newly generated DRAFT bills for the previous month
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyBills() {
        String lastMonth = YearMonth.now().minusMonths(1).toString();
        log.info("[BillingSchedule] Monthly billing cycle starting. Target month: {}", lastMonth);

        // Step 1: Auto-settle all CONFIRMED bills from previous months
        try {
            int settled = feeBillService.settleAllConfirmedBeforeMonth(YearMonth.now().toString());
            log.info("[BillingSchedule] Auto-settled {} confirmed bills from previous months", settled);
        } catch (Exception e) {
            log.error("[BillingSchedule] Failed to auto-settle previous bills: {}", e.getMessage(), e);
        }

        // Step 2: Generate DRAFT bills for last month
        try {
            int count = feeBillService.generateDraft(lastMonth);
            log.info("[BillingSchedule] Generated {} draft bills for {}", count, lastMonth);
        } catch (Exception e) {
            log.error("[BillingSchedule] Failed to generate bills for {}: {}", lastMonth, e.getMessage(), e);
        }

        // Step 3: Auto-confirm the newly generated DRAFT bills for last month
        try {
            int confirmed = feeBillService.confirmAllDraft(lastMonth);
            log.info("[BillingSchedule] Auto-confirmed {} draft bills for {}", confirmed, lastMonth);
        } catch (Exception e) {
            log.error("[BillingSchedule] Failed to auto-confirm bills for {}: {}", lastMonth, e.getMessage(), e);
        }
    }
}
