package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.staff.AttendanceUpsertRequest;
import com.hfnew.dto.staff.AttendanceVO;
import com.hfnew.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/staff/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    @PreAuthorize("hasAuthority('staff:attendance')")
    public ResponseEntity<ApiResponse<PageResult<AttendanceVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.list(page, pageSize, staffId, startDate, endDate)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('staff:attendance')")
    @OpLog(module = "护工管理", operation = "补录打卡")
    public ResponseEntity<ApiResponse<Long>> upsert(@RequestBody AttendanceUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.upsert(request)));
    }
}
