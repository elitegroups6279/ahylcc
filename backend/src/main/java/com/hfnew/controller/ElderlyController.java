package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.elderly.*;
import com.hfnew.entity.ElderlyChangeLog;
import com.hfnew.entity.ElderlyLeave;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.ElderlyLeaveService;
import com.hfnew.service.ElderlyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elderly")
@RequiredArgsConstructor
public class ElderlyController {

    private final ElderlyService elderlyService;
    private final ElderlyLeaveService elderlyLeaveService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<ElderlyVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(ApiResponse.success(elderlyService.list(page, pageSize, keyword, status, category)));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> options(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(elderlyService.listActiveOptions(keyword)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ElderlyVO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(elderlyService.getById(id)));
    }

    @PostMapping
    @OpLog(module = "入住管理", operation = "新增入住")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody ElderlyCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(elderlyService.create(request)));
    }

    @PutMapping("/{id}")
    @OpLog(module = "入住管理", operation = "更新老人档案")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id, @RequestBody ElderlyUpdateRequest request) {
        elderlyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/{id}/discharge")
    @OpLog(module = "入住管理", operation = "退住")
    public ResponseEntity<ApiResponse<Object>> discharge(@PathVariable Long id, @RequestBody ElderlyDischargeRequest request) {
        elderlyService.discharge(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/{id}/undo-discharge")
    @OpLog(module = "入住管理", operation = "撤销退住")
    public ResponseEntity<ApiResponse<Object>> undoDischarge(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        elderlyService.undoDischarge(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/{id}/transfer")
    @OpLog(module = "入住管理", operation = "转床")
    public ResponseEntity<ApiResponse<Object>> transfer(@PathVariable Long id, @RequestBody ElderlyTransferRequest request) {
        elderlyService.transfer(id, request, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 下载老人导入模板
     */
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate() throws IOException {
        byte[] data = elderlyService.generateImportTemplate();
        String filename = URLEncoder.encode("老人导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=elderly_import_template.xlsx; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /**
     * 批量导入老人数据
     */
    @PostMapping("/import")
    @OpLog(module = "入住管理", operation = "批量导入老人")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importElderly(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> result = elderlyService.importFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/changes")
    public ResponseEntity<ApiResponse<List<ElderlyChangeLog>>> getChanges(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(elderlyService.getChangeLogs(id)));
    }

    // ==================== 请假管理 ====================

    @PostMapping("/{id}/leave")
    @OpLog(module = "入住管理", operation = "老人请假")
    public ResponseEntity<ApiResponse<ElderlyLeave>> applyLeave(@PathVariable Long id, @Valid @RequestBody ElderlyLeaveRequest request) {
        ElderlyLeave leave = elderlyLeaveService.applyLeave(id, request);
        return ResponseEntity.ok(ApiResponse.success(leave));
    }

    @PutMapping("/{id}/leave/return")
    @OpLog(module = "入住管理", operation = "老人销假")
    public ResponseEntity<ApiResponse<ElderlyLeave>> returnFromLeave(@PathVariable Long id, @RequestBody(required = false) ElderlyReturnRequest request) {
        ElderlyLeave leave = elderlyLeaveService.returnFromLeave(id, request);
        return ResponseEntity.ok(ApiResponse.success(leave));
    }

    @GetMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<List<ElderlyLeave>>> getLeaveHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(elderlyLeaveService.getLeaveHistory(id)));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth == null ? null : auth.getPrincipal();
        if (principal instanceof AuthUserPrincipal p) {
            return p.getUserId();
        }
        return null;
    }
}
