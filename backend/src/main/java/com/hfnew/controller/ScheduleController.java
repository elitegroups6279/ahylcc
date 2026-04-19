package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.staff.BatchScheduleRequest;
import com.hfnew.dto.staff.ScheduleUpsertRequest;
import com.hfnew.dto.staff.ScheduleVO;
import com.hfnew.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    @PreAuthorize("hasAuthority('staff:schedule')")
    public ResponseEntity<ApiResponse<PageResult<ScheduleVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String shiftType
    ) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.list(page, pageSize, staffId, startDate, endDate, shiftType)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('staff:schedule')")
    @OpLog(module = "排班管理", operation = "新增/更新排班")
    public ResponseEntity<ApiResponse<Long>> upsert(@RequestBody ScheduleUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.upsert(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('staff:schedule')")
    @OpLog(module = "排班管理", operation = "删除排班")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/week-view")
    @PreAuthorize("hasAuthority('staff:schedule')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> weekView(
            @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity.ok(ApiResponse.success(
            scheduleService.getWeekView(LocalDate.parse(start), LocalDate.parse(end))));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('staff:schedule')")
    @OpLog(module = "排班管理", operation = "批量排班")
    public ResponseEntity<ApiResponse<Integer>> batchCreate(@RequestBody BatchScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
            scheduleService.batchCreate(req.getStaffId(), req.getStartDate(), req.getEndDate(), req.getShiftType())));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('staff:schedule')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> monthStats(@RequestParam String month) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getMonthStats(month)));
    }
}

