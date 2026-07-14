package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfnew.exception.BizException;
import com.hfnew.common.PageResult;
import com.hfnew.dto.medication.MedicationPlanCreateRequest;
import com.hfnew.dto.medication.MedicationPlanItemDTO;
import com.hfnew.dto.medication.MedicationPlanVO;
import com.hfnew.dto.medication.MedicationRecordVO;
import com.hfnew.dto.medication.TodayBoardVO;
import com.hfnew.entity.Bed;
import com.hfnew.entity.Drug;
import com.hfnew.entity.Elderly;
import com.hfnew.entity.MedicationPlan;
import com.hfnew.entity.MedicationPlanItem;
import com.hfnew.entity.MedicationRecord;
import com.hfnew.mapper.BedMapper;
import com.hfnew.mapper.DrugMapper;
import com.hfnew.mapper.ElderlyMapper;
import com.hfnew.mapper.MedicationPlanMapper;
import com.hfnew.mapper.MedicationPlanItemMapper;
import com.hfnew.mapper.MedicationRecordMapper;
import com.hfnew.util.BatchNameLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationPlanMapper planMapper;
    private final MedicationPlanItemMapper planItemMapper;
    private final MedicationRecordMapper recordMapper;
    private final ElderlyMapper elderlyMapper;
    private final DrugMapper drugMapper;
    private final BedMapper bedMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*");
    private static final List<String> SLOT_ORDER = List.of("MORNING", "AFTERNOON", "EVENING", "BEDTIME");
    private static final Map<String, String> SLOT_LABELS = Map.of(
            "MORNING", "早晨",
            "AFTERNOON", "中午",
            "EVENING", "晚上",
            "BEDTIME", "睡前"
    );

    @Transactional
    public Long createPlan(Long operatorId, MedicationPlanCreateRequest request) {
        validateElderly(request.getElderlyId());
        validateItems(request.getItems());
        validateNoDuplicatePlan(request.getElderlyId(), request.getStartDate(),
                request.getTimeSlots(), request.getItems(), null);

        MedicationPlan plan = new MedicationPlan();
        plan.setElderlyId(request.getElderlyId());
        plan.setFrequencyType(request.getFrequencyType() == null ? "MULTI_DAILY" : request.getFrequencyType());
        plan.setIntervalDays(request.getIntervalDays() == null ? 1 : request.getIntervalDays());
        plan.setTimeSlots(toJson(request.getTimeSlots()));
        plan.setStartDate(request.getStartDate());
        plan.setTimesPerDay(calculateTimesPerDay(request));
        plan.setInstructions(request.getInstructions());
        plan.setPrescriberName(request.getPrescriberName());
        plan.setRemark(request.getRemark());
        plan.setOperatorId(operatorId);
        plan.setStatus("ACTIVE");
        planMapper.insert(plan);

        for (MedicationPlanItemDTO itemDTO : request.getItems()) {
            MedicationPlanItem item = new MedicationPlanItem();
            item.setPlanId(plan.getId());
            item.setDrugId(itemDTO.getDrugId());
            item.setDrugName(itemDTO.getDrugName());
            item.setDosage(itemDTO.getDosage());
            item.setDosageUnit(itemDTO.getDosageUnit());
            item.setTotalQuantity(itemDTO.getTotalQuantity());
            item.setDosagePerTime(itemDTO.getDosagePerTime());
            item.setDepletionDate(calculateItemDepletionDate(plan, itemDTO));
            planItemMapper.insert(item);
        }

        backfillRecords(plan);
        return plan.getId();
    }

    @Transactional
    public void updatePlan(Long id, MedicationPlanCreateRequest request) {
        MedicationPlan plan = planMapper.selectById(id);
        if (plan == null || plan.getDeleted() != null && plan.getDeleted() == 1) {
            throw new BizException(404, 404, "用药计划不存在");
        }
        validateElderly(request.getElderlyId());
        validateItems(request.getItems());
        validateNoDuplicatePlan(request.getElderlyId(), request.getStartDate(),
                request.getTimeSlots(), request.getItems(), id);

        plan.setElderlyId(request.getElderlyId());
        plan.setFrequencyType(request.getFrequencyType() == null ? "MULTI_DAILY" : request.getFrequencyType());
        plan.setIntervalDays(request.getIntervalDays() == null ? 1 : request.getIntervalDays());
        plan.setTimeSlots(toJson(request.getTimeSlots()));
        plan.setStartDate(request.getStartDate());
        plan.setTimesPerDay(calculateTimesPerDay(request));
        plan.setInstructions(request.getInstructions());
        plan.setPrescriberName(request.getPrescriberName());
        plan.setRemark(request.getRemark());
        planMapper.updateById(plan);

        // Diff-update items
        LambdaQueryWrapper<MedicationPlanItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MedicationPlanItem::getPlanId, id);
        List<MedicationPlanItem> existingItems = planItemMapper.selectList(itemWrapper);
        Map<Long, MedicationPlanItem> existingMap = existingItems.stream()
                .collect(Collectors.toMap(MedicationPlanItem::getId, i -> i));

        Set<Long> requestItemIds = request.getItems().stream()
                .map(MedicationPlanItemDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (MedicationPlanItem existing : existingItems) {
            if (!requestItemIds.contains(existing.getId())) {
                planItemMapper.deleteById(existing.getId());
            }
        }

        for (MedicationPlanItemDTO itemDTO : request.getItems()) {
            if (itemDTO.getId() != null && existingMap.containsKey(itemDTO.getId())) {
                MedicationPlanItem item = existingMap.get(itemDTO.getId());
                item.setDrugId(itemDTO.getDrugId());
                item.setDrugName(itemDTO.getDrugName());
                item.setDosage(itemDTO.getDosage());
                item.setDosageUnit(itemDTO.getDosageUnit());
                item.setTotalQuantity(itemDTO.getTotalQuantity());
                item.setDosagePerTime(itemDTO.getDosagePerTime());
                item.setDepletionDate(calculateItemDepletionDate(plan, itemDTO));
                planItemMapper.updateById(item);
            } else {
                MedicationPlanItem item = new MedicationPlanItem();
                item.setPlanId(id);
                item.setDrugId(itemDTO.getDrugId());
                item.setDrugName(itemDTO.getDrugName());
                item.setDosage(itemDTO.getDosage());
                item.setDosageUnit(itemDTO.getDosageUnit());
                item.setTotalQuantity(itemDTO.getTotalQuantity());
                item.setDosagePerTime(itemDTO.getDosagePerTime());
                item.setDepletionDate(calculateItemDepletionDate(plan, itemDTO));
                planItemMapper.insert(item);
            }
        }

        // 清理旧配置产生的 PENDING 记录（计划变更后旧配置的待执行记录失效）
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<MedicationRecord> pendingCleanup = new LambdaQueryWrapper<>();
        pendingCleanup.eq(MedicationRecord::getPlanId, id)
                .eq(MedicationRecord::getStatus, "PENDING")
                .ge(MedicationRecord::getRecordDate, today);
        recordMapper.delete(pendingCleanup);

        // 重新生成从 startDate 到今天的 PENDING 记录（含今天的记录）
        backfillRecords(plan);
    }

    @Transactional
    public void pausePlan(Long id) {
        updateStatus(id, "PAUSED");
    }

    @Transactional
    public void resumePlan(Long id) {
        updateStatus(id, "ACTIVE");
    }

    @Transactional
    public void stopPlan(Long id) {
        updateStatus(id, "STOPPED");
    }

    public Map<String, Long> getPlanDeleteStats(Long id) {
        LambdaQueryWrapper<MedicationRecord> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(MedicationRecord::getPlanId, id).eq(MedicationRecord::getDeleted, 0);
        Long total = recordMapper.selectCount(totalWrapper);

        LambdaQueryWrapper<MedicationRecord> doneWrapper = new LambdaQueryWrapper<>();
        doneWrapper.eq(MedicationRecord::getPlanId, id).eq(MedicationRecord::getStatus, "DONE").eq(MedicationRecord::getDeleted, 0);
        Long done = recordMapper.selectCount(doneWrapper);

        LambdaQueryWrapper<MedicationPlanItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MedicationPlanItem::getPlanId, id).eq(MedicationPlanItem::getDeleted, 0);
        Long items = planItemMapper.selectCount(itemWrapper);

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("totalRecords", total != null ? total : 0);
        stats.put("doneRecords", done != null ? done : 0);
        stats.put("itemCount", items != null ? items : 0);
        return stats;
    }

    @Transactional
    public void deletePlan(Long id) {
        MedicationPlan plan = planMapper.selectById(id);
        if (plan == null) throw new BizException(404, 404, "用药计划不存在");

        // Cascade delete items
        LambdaQueryWrapper<MedicationPlanItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MedicationPlanItem::getPlanId, id);
        planItemMapper.delete(itemWrapper);

        // 统计各状态记录数（审计用）
        LambdaQueryWrapper<MedicationRecord> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(MedicationRecord::getPlanId, id);
        Long totalRecords = recordMapper.selectCount(countWrapper);
        LambdaQueryWrapper<MedicationRecord> doneWrapper = new LambdaQueryWrapper<>();
        doneWrapper.eq(MedicationRecord::getPlanId, id).eq(MedicationRecord::getStatus, "DONE");
        Long doneRecords = recordMapper.selectCount(doneWrapper);

        // Cascade delete records
        LambdaQueryWrapper<MedicationRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(MedicationRecord::getPlanId, id);
        recordMapper.delete(recordWrapper);

        // Delete the plan itself
        planMapper.deleteById(id);

        if (totalRecords != null && totalRecords > 0) {
            log.warn("删除用药计划 id={}，级联删除 {} 条记录（其中已确认 {} 条）", id, totalRecords, doneRecords != null ? doneRecords : 0);
        }
    }

    public PageResult<MedicationPlanVO> listPlans(int page, int pageSize, Long elderlyId, String elderlyName, Long drugId, String status) {
        Page<MedicationPlan> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<MedicationPlan> wrapper = new LambdaQueryWrapper<>();
        if (elderlyId != null) wrapper.eq(MedicationPlan::getElderlyId, elderlyId);

        // 根据老人姓名模糊搜索
        if (StringUtils.hasText(elderlyName)) {
            LambdaQueryWrapper<Elderly> nameQuery = new LambdaQueryWrapper<>();
            nameQuery.like(Elderly::getName, elderlyName);
            nameQuery.eq(Elderly::getDeleted, 0);
            List<Elderly> matchedElderly = elderlyMapper.selectList(nameQuery);
            Set<Long> matchedIds = matchedElderly.stream().map(Elderly::getId).collect(Collectors.toSet());
            if (matchedIds.isEmpty()) {
                return new PageResult<>(pageReq.getCurrent(), pageReq.getSize(), 0, Collections.emptyList());
            }
            wrapper.in(MedicationPlan::getElderlyId, matchedIds);
        }
        if (drugId != null) {
            // Query item table for matching plan IDs
            LambdaQueryWrapper<MedicationPlanItem> itemQuery = new LambdaQueryWrapper<>();
            itemQuery.eq(MedicationPlanItem::getDrugId, drugId);
            List<MedicationPlanItem> matchingItems = planItemMapper.selectList(itemQuery);
            Set<Long> matchingPlanIds = matchingItems.stream().map(MedicationPlanItem::getPlanId).collect(Collectors.toSet());
            if (matchingPlanIds.isEmpty()) {
                return new PageResult<>(pageReq.getCurrent(), pageReq.getSize(), 0, Collections.emptyList());
            }
            wrapper.in(MedicationPlan::getId, matchingPlanIds);
        }
        if (StringUtils.hasText(status)) wrapper.eq(MedicationPlan::getStatus, status);
        wrapper.orderByDesc(MedicationPlan::getCreateTime).orderByDesc(MedicationPlan::getId);
        IPage<MedicationPlan> result = planMapper.selectPage(pageReq, wrapper);

        Set<Long> elderlyIds = result.getRecords().stream()
                .map(MedicationPlan::getElderlyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> elderlyNameMap = BatchNameLoader.loadNames(jdbcTemplate, "t_elderly", "id", "name", elderlyIds);

        // Batch load items
        Set<Long> planIds = result.getRecords().stream().map(MedicationPlan::getId).collect(Collectors.toSet());
        final Map<Long, List<MedicationPlanItem>> itemMap;
        if (!planIds.isEmpty()) {
            LambdaQueryWrapper<MedicationPlanItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(MedicationPlanItem::getPlanId, planIds);
            List<MedicationPlanItem> allItems = planItemMapper.selectList(itemWrapper);
            itemMap = allItems.stream().collect(Collectors.groupingBy(MedicationPlanItem::getPlanId));
        } else {
            itemMap = Collections.emptyMap();
        }

        LocalDate today = LocalDate.now();
        List<MedicationPlanVO> list = result.getRecords().stream()
                .map(p -> toPlanVO(p, elderlyNameMap.get(p.getElderlyId()), today,
                        itemMap.getOrDefault(p.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public List<TodayBoardVO> getTodayBoard(LocalDate date) {
        if (date == null) date = LocalDate.now();
        LambdaQueryWrapper<MedicationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationRecord::getRecordDate, date);
        wrapper.eq(MedicationRecord::getDeleted, 0);
        wrapper.orderByAsc(MedicationRecord::getElderlyId)
                .orderByAsc(MedicationRecord::getTimeSlot)
                .orderByAsc(MedicationRecord::getId);
        List<MedicationRecord> records = recordMapper.selectList(wrapper);
        if (records.isEmpty()) return Collections.emptyList();

        Set<Long> elderlyIds = records.stream().map(MedicationRecord::getElderlyId).collect(Collectors.toSet());
        Map<Long, Elderly> elderlyMap = loadElderlyMap(elderlyIds);
        Set<Long> bedIds = elderlyMap.values().stream().map(Elderly::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Bed> bedMap = loadBedMap(bedIds);
        Map<Long, String> elderlyNameMap = elderlyMap.values().stream()
                .collect(Collectors.toMap(Elderly::getId, Elderly::getName, (a, b) -> a));

        Map<Long, Map<String, List<MedicationRecordVO>>> grouped = new LinkedHashMap<>();
        for (MedicationRecord r : records) {
            MedicationRecordVO vo = toRecordVO(r);
            grouped.computeIfAbsent(r.getElderlyId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(r.getTimeSlot(), k -> new ArrayList<>())
                    .add(vo);
        }

        List<TodayBoardVO> board = new ArrayList<>();
        for (Long elderlyId : grouped.keySet()) {
            Elderly elderly = elderlyMap.get(elderlyId);
            TodayBoardVO vo = new TodayBoardVO();
            vo.setElderlyId(elderlyId);
            vo.setElderlyName(elderlyNameMap.get(elderlyId));
            if (elderly != null && elderly.getBedId() != null) {
                Bed bed = bedMap.get(elderly.getBedId());
                if (bed != null) vo.setBedNumber(bed.getBedNumber());
                vo.setSupplyCategory(elderly.getCategory());
            }

            Map<String, List<MedicationRecordVO>> slotMap = grouped.get(elderlyId);
            List<TodayBoardVO.TimeSlotGroup> timeSlots = new ArrayList<>();
            for (String slot : SLOT_ORDER) {
                List<MedicationRecordVO> slotRecords = slotMap.getOrDefault(slot, Collections.emptyList());
                if (slotRecords.isEmpty()) continue;
                TodayBoardVO.TimeSlotGroup group = new TodayBoardVO.TimeSlotGroup();
                group.setSlot(slot);
                group.setSlotLabel(SLOT_LABELS.getOrDefault(slot, slot));
                group.setOverallStatus(calculateOverallStatus(slotRecords));
                group.setRecords(slotRecords);
                timeSlots.add(group);
            }
            vo.setTimeSlots(timeSlots);
            board.add(vo);
        }
        return board;
    }

    public PageResult<MedicationRecordVO> listRecords(Long elderlyId, LocalDate startDate, LocalDate endDate, String timeSlot, String status, String keyword, int page, int size) {
        Page<MedicationRecord> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<MedicationRecord> wrapper = new LambdaQueryWrapper<>();
        if (elderlyId != null) wrapper.eq(MedicationRecord::getElderlyId, elderlyId);
        if (startDate != null) wrapper.ge(MedicationRecord::getRecordDate, startDate);
        if (endDate != null) wrapper.le(MedicationRecord::getRecordDate, endDate);
        if (StringUtils.hasText(timeSlot)) wrapper.eq(MedicationRecord::getTimeSlot, timeSlot);
        if (StringUtils.hasText(status)) wrapper.eq(MedicationRecord::getStatus, status);
        if (StringUtils.hasText(keyword)) wrapper.like(MedicationRecord::getDrugName, keyword);
        wrapper.eq(MedicationRecord::getDeleted, 0);
        wrapper.orderByDesc(MedicationRecord::getRecordDate)
                .orderByAsc(MedicationRecord::getTimeSlot)
                .orderByDesc(MedicationRecord::getId);
        IPage<MedicationRecord> result = recordMapper.selectPage(pageReq, wrapper);

        // Batch load elderly names
        Set<Long> elderlyIds = result.getRecords().stream()
                .map(MedicationRecord::getElderlyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> elderlyNameMap = BatchNameLoader.loadNames(jdbcTemplate, "t_elderly", "id", "name", elderlyIds);

        List<MedicationRecordVO> list = result.getRecords().stream()
                .map(r -> {
                    MedicationRecordVO vo = toRecordVO(r);
                    vo.setElderlyName(elderlyNameMap.get(r.getElderlyId()));
                    return vo;
                })
                .collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public void confirmRecord(Long id, Long executorId, String executorName) {
        MedicationRecord record = recordMapper.selectById(id);
        if (record == null || record.getDeleted() != null && record.getDeleted() == 1) {
            throw new BizException(404, 404, "用药记录不存在");
        }
        record.setStatus("DONE");
        record.setExecutorId(executorId);
        record.setExecutorName(executorName);
        record.setExecutedAt(LocalDateTime.now());
        recordMapper.updateById(record);
    }

    @Transactional
    public void skipRecord(Long id, String reason) {
        MedicationRecord record = recordMapper.selectById(id);
        if (record == null || record.getDeleted() != null && record.getDeleted() == 1) {
            throw new BizException(404, 404, "用药记录不存在");
        }
        record.setStatus("SKIPPED");
        record.setSkipReason(reason);
        recordMapper.updateById(record);
    }

    @Transactional
    public void refuseRecord(Long id, String reason) {
        MedicationRecord record = recordMapper.selectById(id);
        if (record == null || record.getDeleted() != null && record.getDeleted() == 1) {
            throw new BizException(404, 404, "用药记录不存在");
        }
        record.setStatus("REFUSED");
        record.setSkipReason(reason);
        recordMapper.updateById(record);
    }

    @Transactional
    public void confirmBatch(List<Long> recordIds, Long executorId, String executorName) {
        if (CollectionUtils.isEmpty(recordIds)) return;
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<MedicationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MedicationRecord::getId, recordIds)
                .eq(MedicationRecord::getDeleted, 0);
        MedicationRecord update = new MedicationRecord();
        update.setStatus("DONE");
        update.setExecutorId(executorId);
        update.setExecutorName(executorName);
        update.setExecutedAt(now);
        recordMapper.update(update, wrapper);
    }

    @Transactional
    public void confirmBatchByDate(LocalDate date, Long executorId, String executorName) {
        if (date == null) date = LocalDate.now();
        String execName = executorName != null ? executorName : "批量确认";
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<MedicationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationRecord::getRecordDate, date)
                .eq(MedicationRecord::getStatus, "PENDING")
                .eq(MedicationRecord::getDeleted, 0);
        MedicationRecord update = new MedicationRecord();
        update.setStatus("DONE");
        update.setExecutorId(executorId);
        update.setExecutorName(execName);
        update.setExecutedAt(now);
        recordMapper.update(update, wrapper);
    }

    @Transactional
    public void supplementRecord(Long id, String supplementType, Long executorId, String executorName, String reason) {
        MedicationRecord record = recordMapper.selectById(id);
        if (record == null || record.getDeleted() != null && record.getDeleted() == 1) {
            throw new BizException(404, 404, "用药记录不存在");
        }
        if (!"PENDING".equals(record.getStatus()) && !"MISSED".equals(record.getStatus())) {
            throw new BizException(400, 400, "仅待执行或漏服状态的记录可补录");
        }
        switch (supplementType) {
            case "CONFIRM":
                record.setStatus("DONE");
                record.setExecutorId(executorId);
                record.setExecutorName(executorName != null ? executorName : "补录");
                record.setExecutedAt(LocalDateTime.now());
                break;
            case "SKIP":
                record.setStatus("SKIPPED");
                record.setSkipReason(reason);
                break;
            case "REFUSE":
                record.setStatus("REFUSED");
                record.setSkipReason(reason);
                break;
            default:
                throw new BizException(400, 400, "无效的补录类型: " + supplementType);
        }
        recordMapper.updateById(record);
    }

    public List<MedicationRecordVO> getElderlyRecords(Long elderlyId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        LambdaQueryWrapper<MedicationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationRecord::getElderlyId, elderlyId)
                .ge(MedicationRecord::getRecordDate, startDate)
                .le(MedicationRecord::getRecordDate, endDate)
                .eq(MedicationRecord::getDeleted, 0)
                .orderByDesc(MedicationRecord::getRecordDate)
                .orderByAsc(MedicationRecord::getTimeSlot)
                .orderByDesc(MedicationRecord::getId);
        return recordMapper.selectList(wrapper).stream().map(this::toRecordVO).collect(Collectors.toList());
    }

    public List<MedicationRecordVO> exportRecords(Long elderlyId, LocalDate startDate, LocalDate endDate,
                                                  String timeSlot, String status, String keyword) {
        LambdaQueryWrapper<MedicationRecord> wrapper = new LambdaQueryWrapper<>();
        if (elderlyId != null) wrapper.eq(MedicationRecord::getElderlyId, elderlyId);
        if (startDate != null) wrapper.ge(MedicationRecord::getRecordDate, startDate);
        if (endDate != null) wrapper.le(MedicationRecord::getRecordDate, endDate);
        if (StringUtils.hasText(timeSlot)) wrapper.eq(MedicationRecord::getTimeSlot, timeSlot);
        if (StringUtils.hasText(status)) wrapper.eq(MedicationRecord::getStatus, status);
        if (StringUtils.hasText(keyword)) wrapper.like(MedicationRecord::getDrugName, keyword);
        wrapper.eq(MedicationRecord::getDeleted, 0);
        wrapper.orderByDesc(MedicationRecord::getRecordDate)
                .orderByAsc(MedicationRecord::getTimeSlot)
                .orderByDesc(MedicationRecord::getId);
        // Page-based limit for export (max 5000 rows)
        Page<MedicationRecord> exportPage = new Page<>(1, 5000);
        IPage<MedicationRecord> exportResult = recordMapper.selectPage(exportPage, wrapper);

        List<MedicationRecord> records = exportResult.getRecords();

        // Batch load elderly names
        Set<Long> elderlyIds = records.stream()
                .map(MedicationRecord::getElderlyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> elderlyNameMap = BatchNameLoader.loadNames(jdbcTemplate, "t_elderly", "id", "name", elderlyIds);

        return records.stream()
                .map(r -> {
                    MedicationRecordVO vo = toRecordVO(r);
                    vo.setElderlyName(elderlyNameMap.get(r.getElderlyId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void backfillRecords(MedicationPlan plan) {
        if (plan.getStartDate() == null || plan.getStartDate().isAfter(LocalDate.now())) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<String> slots = plan.getTimeSlotsAsList();
        if (CollectionUtils.isEmpty(slots)) return;

        LambdaQueryWrapper<MedicationPlanItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MedicationPlanItem::getPlanId, plan.getId());
        List<MedicationPlanItem> items = planItemMapper.selectList(itemWrapper);
        if (CollectionUtils.isEmpty(items)) return;

        // 批量查询已有记录，避免 N+1 查询
        LambdaQueryWrapper<MedicationRecord> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(MedicationRecord::getPlanId, plan.getId())
                .eq(MedicationRecord::getDeleted, 0);
        List<MedicationRecord> existingRecords = recordMapper.selectList(existingWrapper);
        Set<String> existingKeys = existingRecords.stream()
                .map(r -> r.getRecordDate() + "|" + r.getTimeSlot() + "|" + r.getDrugId())
                .collect(Collectors.toSet());

        List<MedicationRecord> records = new ArrayList<>();
        for (LocalDate date = plan.getStartDate(); !date.isAfter(today); date = date.plusDays(1)) {
            if (!shouldGenerateOnDate(plan, date)) continue;
            for (String slot : slots) {
                for (MedicationPlanItem item : items) {
                    String key = date + "|" + slot + "|" + item.getDrugId();
                    if (existingKeys.contains(key)) continue;

                    MedicationRecord record = new MedicationRecord();
                    record.setPlanId(plan.getId());
                    record.setElderlyId(plan.getElderlyId());
                    record.setDrugId(item.getDrugId());
                    record.setDrugName(item.getDrugName());
                    record.setDosage(item.getDosage());
                    record.setRecordDate(date);
                    record.setTimeSlot(slot);
                    record.setStatus(date.isBefore(today) ? "DONE" : "PENDING");
                    record.setOrgId(plan.getOrgId());
                    records.add(record);
                }
            }
        }
        if (!records.isEmpty()) {
            for (MedicationRecord record : records) {
                recordMapper.insert(record);
            }
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

    private void validateElderly(Long elderlyId) {
        if (elderlyId == null) throw new BizException(400, 400, "请选择老人");
        Elderly elderly = elderlyMapper.selectById(elderlyId);
        if (elderly == null || elderly.getDeleted() != null && elderly.getDeleted() == 1) {
            throw new BizException(404, 404, "老人不存在");
        }
        if ("DISCHARGED".equals(elderly.getStatus())) {
            throw new BizException(400, 400, "老人已退住，无法创建用药计划");
        }
    }

    private void validateDrug(Long drugId) {
        if (drugId == null) throw new BizException(400, 400, "请选择药品");
        Drug drug = drugMapper.selectById(drugId);
        if (drug == null || drug.getDeleted() != null && drug.getDeleted() == 1) {
            throw new BizException(404, 404, "药品不存在");
        }
    }

    private void validateItems(List<MedicationPlanItemDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(400, 400, "请至少添加一种药品");
        }
        for (MedicationPlanItemDTO item : items) {
            validateDrug(item.getDrugId());
        }
    }

    private void validateNoDuplicatePlan(Long elderlyId, LocalDate startDate, List<String> timeSlots,
                                         List<MedicationPlanItemDTO> items, Long excludePlanId) {
        if (elderlyId == null || startDate == null || CollectionUtils.isEmpty(items)) return;

        Set<Long> requestDrugIds = items.stream()
                .map(MedicationPlanItemDTO::getDrugId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (requestDrugIds.isEmpty()) return;

        Set<String> requestSlots = new HashSet<>(timeSlots == null ? Collections.emptyList() : timeSlots);

        LambdaQueryWrapper<MedicationPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationPlan::getElderlyId, elderlyId);
        wrapper.eq(MedicationPlan::getStartDate, startDate);
        wrapper.ne(MedicationPlan::getStatus, "STOPPED");
        wrapper.ne(MedicationPlan::getStatus, "COMPLETED");
        if (excludePlanId != null) wrapper.ne(MedicationPlan::getId, excludePlanId);
        wrapper.eq(MedicationPlan::getDeleted, 0);
        List<MedicationPlan> existingPlans = planMapper.selectList(wrapper);

        for (MedicationPlan existing : existingPlans) {
            Set<String> existingSlots = new HashSet<>(existing.getTimeSlotsAsList());
            if (!existingSlots.equals(requestSlots)) continue;

            LambdaQueryWrapper<MedicationPlanItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(MedicationPlanItem::getPlanId, existing.getId());
            List<MedicationPlanItem> existingItems = planItemMapper.selectList(itemWrapper);
            Set<Long> existingDrugIds = existingItems.stream()
                    .map(MedicationPlanItem::getDrugId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (existingDrugIds.equals(requestDrugIds)) {
                throw new BizException(400, 400, "该老人在" + startDate + "已存在相同的用药计划，请勿重复添加");
            }
        }
    }

    private LocalDate calculateItemDepletionDate(MedicationPlan plan, MedicationPlanItemDTO item) {
        if (plan.getStartDate() == null || item.getTotalQuantity() == null || item.getDosagePerTime() == null) {
            return null;
        }
        BigDecimal dosagePerTime = parseNumeric(item.getDosagePerTime());
        if (dosagePerTime == null || dosagePerTime.compareTo(BigDecimal.ZERO) <= 0) return null;
        int timesPerDay = plan.getTimesPerDay() == null || plan.getTimesPerDay() <= 0 ? 1 : plan.getTimesPerDay();
        BigDecimal daily = dosagePerTime.multiply(BigDecimal.valueOf(timesPerDay));
        BigDecimal days = item.getTotalQuantity().divide(daily, 0, RoundingMode.FLOOR);
        if (days.compareTo(BigDecimal.ZERO) <= 0) return plan.getStartDate();
        return plan.getStartDate().plusDays(days.longValue() - 1);
    }

    private void updateStatus(Long id, String status) {
        MedicationPlan plan = planMapper.selectById(id);
        if (plan == null || plan.getDeleted() != null && plan.getDeleted() == 1) {
            throw new BizException(404, 404, "用药计划不存在");
        }
        plan.setStatus(status);
        planMapper.updateById(plan);
    }

    private BigDecimal parseNumeric(String value) {
        if (!StringUtils.hasText(value)) return null;
        Matcher matcher = NUMERIC_PATTERN.matcher(value);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private int calculateTimesPerDay(MedicationPlanCreateRequest request) {
        if ("MULTI_DAILY".equals(request.getFrequencyType())) {
            return CollectionUtils.isEmpty(request.getTimeSlots()) ? 1 : request.getTimeSlots().size();
        }
        return 1;
    }

    private String toJson(List<String> list) {
        if (CollectionUtils.isEmpty(list)) return "[]";
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<Long, Elderly> loadElderlyMap(Set<Long> elderlyIds) {
        if (elderlyIds.isEmpty()) return Map.of();
        return elderlyMapper.selectBatchIds(elderlyIds).stream()
                .collect(Collectors.toMap(Elderly::getId, e -> e, (a, b) -> a));
    }

    private Map<Long, Bed> loadBedMap(Set<Long> bedIds) {
        if (bedIds.isEmpty()) return Map.of();
        return bedMapper.selectBatchIds(bedIds).stream()
                .collect(Collectors.toMap(Bed::getId, b -> b, (a, b) -> a));
    }

    private MedicationPlanVO toPlanVO(MedicationPlan plan, String elderlyName, LocalDate today,
                                       List<MedicationPlanItem> items) {
        MedicationPlanVO vo = new MedicationPlanVO();
        vo.setId(plan.getId());
        vo.setElderlyId(plan.getElderlyId());
        vo.setElderlyName(elderlyName);
        vo.setFrequencyType(plan.getFrequencyType());
        vo.setIntervalDays(plan.getIntervalDays());
        vo.setTimeSlots(plan.getTimeSlots());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setTimesPerDay(plan.getTimesPerDay());
        vo.setInstructions(plan.getInstructions());
        vo.setPrescriberName(plan.getPrescriberName());
        vo.setStatus(plan.getStatus());
        vo.setOperatorId(plan.getOperatorId());
        vo.setRemark(plan.getRemark());
        vo.setCreateTime(plan.getCreateTime());
        vo.setUpdateTime(plan.getUpdateTime());

        List<MedicationPlanItemDTO> itemDTOs = items.stream()
                .map(i -> {
                    MedicationPlanItemDTO dto = new MedicationPlanItemDTO();
                    dto.setId(i.getId());
                    dto.setDrugId(i.getDrugId());
                    dto.setDrugName(i.getDrugName());
                    dto.setDosage(i.getDosage());
                    dto.setDosageUnit(i.getDosageUnit());
                    dto.setTotalQuantity(i.getTotalQuantity());
                    dto.setDosagePerTime(i.getDosagePerTime());
                    dto.setDepletionDate(i.getDepletionDate());
                    return dto;
                })
                .collect(Collectors.toList());
        vo.setItems(itemDTOs);

        LocalDate earliestDepletion = items.stream()
                .map(MedicationPlanItem::getDepletionDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (earliestDepletion != null) {
            long remaining = ChronoUnit.DAYS.between(today, earliestDepletion);
            vo.setRemainingDays(Math.max(remaining, 0));
        }
        return vo;
    }

    private MedicationRecordVO toRecordVO(MedicationRecord record) {
        MedicationRecordVO vo = new MedicationRecordVO();
        vo.setId(record.getId());
        vo.setPlanId(record.getPlanId());
        vo.setElderlyId(record.getElderlyId());
        vo.setDrugId(record.getDrugId());
        vo.setDrugName(record.getDrugName());
        vo.setDosage(record.getDosage());
        vo.setRecordDate(record.getRecordDate());
        vo.setTimeSlot(record.getTimeSlot());
        vo.setStatus(record.getStatus());
        vo.setExecutorId(record.getExecutorId());
        vo.setExecutorName(record.getExecutorName());
        vo.setExecutedAt(record.getExecutedAt());
        vo.setSkipReason(record.getSkipReason());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        return vo;
    }

    private String calculateOverallStatus(List<MedicationRecordVO> records) {
        if (records.isEmpty()) return "NONE";
        long done = records.stream().filter(r -> "DONE".equals(r.getStatus())).count();
        long pending = records.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        if (pending == 0) return "ALL_DONE";
        if (done == 0) return "PENDING";
        return "PARTIAL";
    }
}
