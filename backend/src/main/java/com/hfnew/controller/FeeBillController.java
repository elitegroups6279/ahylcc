package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.finance.FeeBillVO;
import com.hfnew.service.FeeBillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/finance/bills")
@RequiredArgsConstructor
public class FeeBillController {

    private final FeeBillService feeBillService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<FeeBillVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String billMonth,
            @RequestParam(required = false) Long elderlyId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(feeBillService.list(page, pageSize, billMonth, elderlyId, status)));
    }

    @PostMapping("/generate")
    @OpLog(module = "收支管理", operation = "生成月账单草稿")
    public ResponseEntity<ApiResponse<Integer>> generate(@RequestParam(required = false) String billMonth) {
        return ResponseEntity.ok(ApiResponse.success(feeBillService.generateDraft(billMonth)));
    }

    @PutMapping("/{id}/confirm")
    @OpLog(module = "收支管理", operation = "确认月结算")
    public ResponseEntity<ApiResponse<Object>> confirm(@PathVariable Long id) {
        feeBillService.confirm(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/subsidy-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> subsidySummary(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(ApiResponse.success(feeBillService.getSubsidySummary(month)));
    }
}
