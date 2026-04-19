package com.hfnew.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.common.ApiResponse;
import com.hfnew.dto.dashboard.CalendarDayEventsDTO;
import com.hfnew.dto.dashboard.CalendarEventItemDTO;
import com.hfnew.dto.notify.FeeWarningItem;
import com.hfnew.entity.Elderly;
import com.hfnew.entity.ElderlyLeave;
import com.hfnew.entity.PaymentRecord;
import com.hfnew.entity.Reimbursement;
import com.hfnew.entity.Staff;
import com.hfnew.mapper.ElderlyLeaveMapper;
import com.hfnew.mapper.ElderlyMapper;
import com.hfnew.mapper.PaymentRecordMapper;
import com.hfnew.mapper.ReimbursementMapper;
import com.hfnew.mapper.StaffMapper;
import com.hfnew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final NotificationService notificationService;
    private final ElderlyMapper elderlyMapper;
    private final StaffMapper staffMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final ReimbursementMapper reimbursementMapper;
    private final ElderlyLeaveMapper elderlyLeaveMapper;

    @GetMapping("/fee-warnings")
    public ResponseEntity<ApiResponse<List<FeeWarningItem>>> feeWarnings() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.listFeeWarnings()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 在住老人数（status=ACTIVE或ON_LEAVE且deleted=0）
        LambdaQueryWrapper<Elderly> elderlyWrapper = new LambdaQueryWrapper<>();
        elderlyWrapper.in(Elderly::getStatus, "ACTIVE", "ON_LEAVE");
        long elderlyCount = elderlyMapper.selectCount(elderlyWrapper);
        stats.put("elderlyCount", elderlyCount);

        // 在职护工数（status=ACTIVE且deleted=0）
        LambdaQueryWrapper<Staff> staffWrapper = new LambdaQueryWrapper<>();
        staffWrapper.eq(Staff::getStatus, "ACTIVE");
        long staffCount = staffMapper.selectCount(staffWrapper);
        stats.put("staffCount", staffCount);

        // 本月收入（从payment_record表查询当月记录金额合计）
        LocalDateTime startOfMonth = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        LambdaQueryWrapper<PaymentRecord> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.ge(PaymentRecord::getCreateTime, startOfMonth);
        List<PaymentRecord> payments = paymentRecordMapper.selectList(paymentWrapper);
        BigDecimal monthlyIncome = payments.stream()
                .map(PaymentRecord::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("monthlyIncome", monthlyIncome);

        // 待处理事项（待审批报账数，从reimbursement表查status=PENDING）
        LambdaQueryWrapper<Reimbursement> reimbursementWrapper = new LambdaQueryWrapper<>();
        reimbursementWrapper.eq(Reimbursement::getStatus, "PENDING");
        long pendingTasks = reimbursementMapper.selectCount(reimbursementWrapper);
        stats.put("pendingTasks", pendingTasks);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/calendar-events")
    public ResponseEntity<ApiResponse<List<CalendarDayEventsDTO>>> calendarEvents(
            @RequestParam int year,
            @RequestParam int month) {

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // 查询当月入住的老人（绕过逻辑删除）
        List<Elderly> admissions = elderlyMapper.selectAdmissionsInMonth(monthStart, monthEnd);
        // 查询当月退住的老人（绕过逻辑删除）
        List<Elderly> discharges = elderlyMapper.selectDischargesInMonth(monthStart, monthEnd);
        // 查询当月请假记录
        List<ElderlyLeave> leaves = elderlyLeaveMapper.selectLeavesInMonth(monthStart, monthEnd);

        // 按日期分组，使用LinkedHashMap保持日期排序
        Map<LocalDate, List<CalendarEventItemDTO>> eventMap = new LinkedHashMap<>();

        for (Elderly e : admissions) {
            LocalDate date = e.getAdmissionDate();
            eventMap.computeIfAbsent(date, k -> new ArrayList<>())
                    .add(new CalendarEventItemDTO("ADMISSION", e.getCategory(), e.getName(), e.getId()));
        }

        for (Elderly e : discharges) {
            LocalDate date = e.getDischargeDate();
            // 退住事件category设为null
            eventMap.computeIfAbsent(date, k -> new ArrayList<>())
                    .add(new CalendarEventItemDTO("DISCHARGE", null, e.getName(), e.getId()));
        }

        // 处理请假事件，按天展开
        for (ElderlyLeave leave : leaves) {
            // 获取老人信息
            Elderly e = elderlyMapper.selectById(leave.getElderlyId());
            if (e == null) continue;

            LocalDate from = leave.getStartDate().isBefore(monthStart) ? monthStart : leave.getStartDate();
            LocalDate to = leave.getEndDate() != null && leave.getEndDate().isBefore(monthEnd) 
                    ? leave.getEndDate() 
                    : monthEnd;

            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                String dateStr = d.toString();
                eventMap.computeIfAbsent(d, k -> new ArrayList<>())
                        .add(new CalendarEventItemDTO("LEAVE", null, e.getName(), e.getId()));
            }
        }

        // 组装返回结果，按日期排序
        List<CalendarDayEventsDTO> result = new ArrayList<>();
        eventMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.add(
                        new CalendarDayEventsDTO(entry.getKey().toString(), entry.getValue())
                ));

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
