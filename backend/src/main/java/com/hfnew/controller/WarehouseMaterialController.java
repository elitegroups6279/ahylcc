package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.warehouse.MaterialCreateRequest;
import com.hfnew.dto.warehouse.MaterialUpdateRequest;
import com.hfnew.dto.warehouse.MaterialVO;
import com.hfnew.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouse/materials")
@RequiredArgsConstructor
public class WarehouseMaterialController {

    private final MaterialService materialService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:materials')")
    public ResponseEntity<ApiResponse<PageResult<MaterialVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(ApiResponse.success(materialService.list(page, pageSize, keyword, category)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:materials')")
    @OpLog(module = "仓库管理", operation = "新增物资")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody MaterialCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(materialService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:materials')")
    @OpLog(module = "仓库管理", operation = "更新物资")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id, @RequestBody MaterialUpdateRequest request) {
        materialService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:materials')")
    @OpLog(module = "仓库管理", operation = "删除物资")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
