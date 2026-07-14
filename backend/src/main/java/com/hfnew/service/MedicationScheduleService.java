package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.entity.Elderly;
import com.hfnew.entity.ElderlyLeave;
import com.hfnew.entity.MedicationPlan;
import com.hfnew.entity.MedicationPlanItem;
import com.hfnew.entity.MedicationRecord;
import com.hfnew.mapper.ElderlyLeaveMapper;
import com.hfnew.mapper.ElderlyMapper;
import com.hfnew.mapper.MedicationPlanMapper;
import com.hfnew.mapper.MedicationPlanItemMapper;
import com.hfnew.mapper.MedicationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationScheduleService {

    private final MedicationPlanMapper planMapper;
    private final MedicationPlanItemMapper planItemMapper;
    private final MedicationRecordMapper recordMapper;
    private final ElderlyMapper elderlyMapper;
    private final ElderlyLeaveMapper elderlyLeaveMapper;

    @Transactional
    @Scheduled(cron = "0 1 0 * * ?")
    public void generateDailyRecords() {
        try {
            LocalDate today = LocalDate.now();
            log.info("[MedicationSchedule] Starting daily record generation for {}", today);

            LambdaQueryWrapper<MedicationPlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MedicationPlan::getStatus, "ACTIVE")
                    .le(MedicationPlan::getStartDate, today)
                    .and(w -> w.isNull(MedicationPlan::getEndDate).or().ge(MedicationPlan::getEndDate, today))
                    .eq(MedicationPlan::getDeleted, 0);
            List<MedicationPlan> plans = planMapper.selectList(wrapper);
            if (plans.isEmpty()) {
                log.info("[MedicationSchedule] No active plans to process");
                return;
            }

            Set<Long> elderlyIds = plans.stream().map(MedicationPlan::getElderlyId).collect(Collectors.toSet());
            Map<Long, Elderly> elderlyMap = elderlyMapper.selectBatchIds(elderlyIds).stream()
                    .collect(Collectors.toMap(Elderly::getId, e -> e, (a, b) -> a));

            // Batch load all plan items
            Set<Long> planIds = plans.stream().map(MedicationPlan::getId).collect(Collectors.toSet());
            LambdaQueryWrapper<MedicationPlanItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(MedicationPlanItem::getPlanId, planIds);
            List<MedicationPlanItem> allItems = planItemMapper.selectList(itemWrapper);
            Map<Long, List<MedicationPlanItem>> itemMap = allItems.stream()
                    .collect(Collectors.groupingBy(MedicationPlanItem::getPlanId));

            int generated = 0;
            int stopped = 0;
            for (MedicationPlan plan : plans) {
                Elderly elderly = elderlyMap.get(plan.getElderlyId());
                if (elderly == null) continue;

                if ("DISCHARGED".equals(elderly.getStatus())) {
                    plan.setStatus("STOPPED");
                    planMapper.updateById(plan);
                    stopped++;
                    continue;
                }

                if ("ON_LEAVE".equals(elderly.getStatus())) {
                    log.debug("[MedicationSchedule] Skipping plan {} for ON_LEAVE elderly {}", plan.getId(), plan.getElderlyId());
                    continue;
                }

                List<MedicationPlanItem> items = itemMap.getOrDefault(plan.getId(), Collections.emptyList());
                boolean allDepleted = !items.isEmpty() && items.stream()
                        .allMatch(i -> i.getDepletionDate() != null && i.getDepletionDate().isBefore(today));
                if (allDepleted) {
                    plan.setStatus("COMPLETED");
                    planMapper.updateById(plan);
                    continue;
                }

                if (!shouldGenerateOnDate(plan, today)) continue;

                List<String> slots = plan.getTimeSlotsAsList();
                if (CollectionUtils.isEmpty(slots)) continue;

                for (String slot : slots) {
                    for (MedicationPlanItem item : items) {
                        if (item.getDepletionDate() != null && item.getDepletionDate().isBefore(today)) continue;

                        // 防止重复生成：检查当天该计划+时段+药品是否已有记录
                        LambdaQueryWrapper<MedicationRecord> dupCheck = new LambdaQueryWrapper<>();
                        dupCheck.eq(MedicationRecord::getPlanId, plan.getId())
                                .eq(MedicationRecord::getElderlyId, plan.getElderlyId())
                                .eq(MedicationRecord::getDrugId, item.getDrugId())
                                .eq(MedicationRecord::getRecordDate, today)
                                .eq(MedicationRecord::getTimeSlot, slot)
                                .eq(MedicationRecord::getDeleted, 0);
                        if (recordMapper.selectCount(dupCheck) > 0) continue;

                        MedicationRecord record = new MedicationRecord();
                        record.setPlanId(plan.getId());
                        record.setElderlyId(plan.getElderlyId());
                        record.setDrugId(item.getDrugId());
                        record.setDrugName(item.getDrugName());
                        record.setDosage(item.getDosage());
                        record.setRecordDate(today);
                        record.setTimeSlot(slot);
                        record.setStatus("PENDING");
                        record.setOrgId(plan.getOrgId());
                        recordMapper.insert(record);
                        generated++;
                    }
                }
            }
            log.info("[MedicationSchedule] Generated {} records, stopped {} plans for {}", generated, stopped, today);
        } catch (Exception e) {
            log.error("[MedicationSchedule] Failed to generate daily records: {}", e.getMessage(), e);
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 23 * * ?")
    public void checkMissedRecords() {
        try {
            LocalDate today = LocalDate.now();
            LambdaQueryWrapper<MedicationRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MedicationRecord::getRecordDate, today)
                    .eq(MedicationRecord::getStatus, "PENDING")
                    .eq(MedicationRecord::getDeleted, 0);
            MedicationRecord update = new MedicationRecord();
            update.setStatus("MISSED");
            int count = recordMapper.update(update, wrapper);
            log.info("[MedicationSchedule] Marked {} records as MISSED for {}", count, today);
        } catch (Exception e) {
            log.error("[MedicationSchedule] Failed to check missed records: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void checkDepletionWarnings() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate threshold = today.plusDays(7);
            LambdaQueryWrapper<MedicationPlanItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.le(MedicationPlanItem::getDepletionDate, threshold)
                    .ge(MedicationPlanItem::getDepletionDate, today)
                    .eq(MedicationPlanItem::getDeleted, 0);
            List<MedicationPlanItem> warningItems = planItemMapper.selectList(wrapper);
            for (MedicationPlanItem item : warningItems) {
                log.warn("[MedicationSchedule] Drug depletion warning: itemId={}, drug={}, depletionDate={}",
                        item.getId(), item.getDrugName(), item.getDepletionDate());
            }
            log.info("[MedicationSchedule] Checked depletion warnings, {} items within 7 days", warningItems.size());
        } catch (Exception e) {
            log.error("[MedicationSchedule] Failed to check depletion warnings: {}", e.getMessage(), e);
        }
    }

    @Transactional
    @Scheduled(cron = "0 5 0 * * ?")
    public void syncLeaveStatus() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LambdaQueryWrapper<ElderlyLeave> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ElderlyLeave::getStatus, "RETURNED")
                    .eq(ElderlyLeave::getReturnDate, yesterday)
                    .eq(ElderlyLeave::getDeleted, 0);
            List<ElderlyLeave> returnedLeaves = elderlyLeaveMapper.selectList(wrapper);
            if (returnedLeaves.isEmpty()) {
                log.info("[MedicationSchedule] No elderly returned from leave yesterday");
                return;
            }

            int resumed = 0;
            int extended = 0;
            for (ElderlyLeave leave : returnedLeaves) {
                if (leave.getStartDate() == null || leave.getReturnDate() == null) continue;
                long leaveDays = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getReturnDate());
                if (leaveDays <= 0) leaveDays = 1;

                LambdaQueryWrapper<MedicationPlan> planWrapper = new LambdaQueryWrapper<>();
                planWrapper.eq(MedicationPlan::getElderlyId, leave.getElderlyId())
                        .eq(MedicationPlan::getStatus, "PAUSED")
                        .eq(MedicationPlan::getDeleted, 0);
                List<MedicationPlan> plans = planMapper.selectList(planWrapper);
                for (MedicationPlan plan : plans) {
                    plan.setStatus("ACTIVE");
                    LambdaQueryWrapper<MedicationPlanItem> itemWrapper = new LambdaQueryWrapper<>();
                    itemWrapper.eq(MedicationPlanItem::getPlanId, plan.getId());
                    List<MedicationPlanItem> items = planItemMapper.selectList(itemWrapper);
                    for (MedicationPlanItem item : items) {
                        if (item.getDepletionDate() != null) {
                            item.setDepletionDate(item.getDepletionDate().plusDays(leaveDays));
                            planItemMapper.updateById(item);
                            extended++;
                        }
                    }
                    planMapper.updateById(plan);
                    resumed++;
                }
            }
            log.info("[MedicationSchedule] Resumed {} plans and extended {} depletion dates for returned elderly", resumed, extended);
        } catch (Exception e) {
            log.error("[MedicationSchedule] Failed to sync leave status: {}", e.getMessage(), e);
        }
    }

    private boolean shouldGenerateOnDate(MedicationPlan plan, LocalDate date) {
        String frequency = plan.getFrequencyType();
        if (frequency == null) frequency = "MULTI_DAILY";
        return switch (frequency) {
            case "ONCE" -> date.equals(plan.getStartDate());
            case "N_DAYS" -> {
                int interval = plan.getIntervalDays() == null || plan.getIntervalDays() <= 0 ? 1 : plan.getIntervalDays();
                long days = ChronoUnit.DAYS.between(plan.getStartDate(), date);
                yield days % interval == 0;
            }
            case "PRN", "MULTI_DAILY" -> true;
            default -> true;
        };
    }
}
