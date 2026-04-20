package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.pharmacy.DrugCreateRequest;
import com.hfnew.dto.pharmacy.DrugUpdateRequest;
import com.hfnew.dto.pharmacy.DrugVO;
import com.hfnew.service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pharmacy/drugs")
@RequiredArgsConstructor
public class PharmacyDrugController {

    private final DrugService drugService;

    @GetMapping
    @PreAuthorize("hasAuthority('pharmacy:drugs')")
    public ResponseEntity<ApiResponse<PageResult<DrugVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(drugService.list(page, pageSize, keyword)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pharmacy:drugs')")
    @OpLog(module = "药物管理", operation = "新增药品")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody DrugCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(drugService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('pharmacy:drugs')")
    @OpLog(module = "药物管理", operation = "更新药品")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id, @RequestBody DrugUpdateRequest request) {
        drugService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pharmacy:drugs')")
    @OpLog(module = "药物管理", operation = "删除药品")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        drugService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
