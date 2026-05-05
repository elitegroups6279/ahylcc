package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.staff.AssignedElderlyVO;
import com.hfnew.dto.staff.StaffCreateRequest;
import com.hfnew.dto.staff.StaffOption;
import com.hfnew.dto.staff.StaffUpdateRequest;
import com.hfnew.dto.staff.StaffVO;
import com.hfnew.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<StaffVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String positionType
    ) {
        return ResponseEntity.ok(ApiResponse.success(staffService.list(page, pageSize, keyword, status, positionType)));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<StaffOption>>> options(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String positionType
    ) {
        return ResponseEntity.ok(ApiResponse.success(staffService.options(keyword, positionType)));
    }

    @GetMapping("/by-ids")
    public ResponseEntity<ApiResponse<List<StaffOption>>> byIds(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.success(staffService.listByIds(ids)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffVO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(staffService.getById(id)));
    }

    @GetMapping("/{staffId}/elderly")
    public ResponseEntity<ApiResponse<List<AssignedElderlyVO>>> getAssignedElderly(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success(staffService.getAssignedElderly(staffId)));
    }

    @PostMapping
    @OpLog(module = "护工管理", operation = "新增护工")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody StaffCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(staffService.create(request)));
    }

    @PutMapping("/{id}")
    @OpLog(module = "护工管理", operation = "更新护工")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id, @RequestBody StaffUpdateRequest request) {
        staffService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "护工管理", operation = "删除护工")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        staffService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
