package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.medication.MedicationPlanCreateRequest;
import com.hfnew.dto.medication.MedicationPlanVO;
import com.hfnew.dto.medication.MedicationRecordActionRequest;
import com.hfnew.dto.medication.MedicationRecordBatchConfirmRequest;
import com.hfnew.dto.medication.MedicationRecordSupplementRequest;
import com.hfnew.dto.medication.MedicationRecordVO;
import com.hfnew.dto.medication.TodayBoardVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/medication")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;

    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    public ResponseEntity<ApiResponse<PageResult<MedicationPlanVO>>> listPlans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long elderlyId,
            @RequestParam(required = false) String elderlyName,
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(medicationService.listPlans(page, pageSize, elderlyId, elderlyName, drugId, status)));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "新增用药计划")
    public ResponseEntity<ApiResponse<Long>> createPlan(@RequestBody MedicationPlanCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(medicationService.createPlan(getCurrentUserId(), request)));
    }

    @PutMapping("/plans/{id}")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "编辑用药计划")
    public ResponseEntity<ApiResponse<Void>> updatePlan(@PathVariable Long id, @RequestBody MedicationPlanCreateRequest request) {
        medicationService.updatePlan(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/plans/{id}/pause")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "暂停用药计划")
    public ResponseEntity<ApiResponse<Void>> pausePlan(@PathVariable Long id) {
        medicationService.pausePlan(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/plans/{id}/resume")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "恢复用药计划")
    public ResponseEntity<ApiResponse<Void>> resumePlan(@PathVariable Long id) {
        medicationService.resumePlan(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/plans/{id}/stop")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "停止用药计划")
    public ResponseEntity<ApiResponse<Void>> stopPlan(@PathVariable Long id) {
        medicationService.stopPlan(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/plans/{id}/delete-info")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getPlanDeleteInfo(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(medicationService.getPlanDeleteStats(id)));
    }

    @DeleteMapping("/plans/{id}")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "删除用药计划")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        medicationService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/records")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    public ResponseEntity<ApiResponse<PageResult<MedicationRecordVO>>> listRecords(
            @RequestParam(required = false) Long elderlyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String timeSlot,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(ApiResponse.success(medicationService.listRecords(elderlyId, startDate, endDate, timeSlot, status, keyword, page, pageSize)));
    }

    @GetMapping("/records/export")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    public ResponseEntity<byte[]> exportRecords(
            @RequestParam(required = false) Long elderlyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String timeSlot,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        List<MedicationRecordVO> records = medicationService.exportRecords(elderlyId, startDate, endDate, timeSlot, status, keyword);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("用药记录");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Header row
            String[] headers = {"序号", "用药人", "药品名称", "剂量", "服药日期", "时段", "状态", "执行护工", "执行时间", "跳过/拒服原因", "备注"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, i == 0 ? 2000 : (i == 10 ? 5000 : 3500));
            }

            // Data rows
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            int rowIdx = 1;
            for (MedicationRecordVO r : records) {
                Row row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(rowIdx);
                row.createCell(1).setCellValue(r.getElderlyName() != null ? r.getElderlyName() : "");
                row.createCell(2).setCellValue(r.getDrugName() != null ? r.getDrugName() : "");
                row.createCell(3).setCellValue(r.getDosage() != null ? r.getDosage() : "");
                row.createCell(4).setCellValue(r.getRecordDate() != null ? r.getRecordDate().toString() : "");
                row.createCell(5).setCellValue(slotLabel(r.getTimeSlot()));
                row.createCell(6).setCellValue(statusLabel(r.getStatus()));
                row.createCell(7).setCellValue(r.getExecutorName() != null ? r.getExecutorName() : "");
                row.createCell(8).setCellValue(r.getExecutedAt() != null ? r.getExecutedAt().format(dtf) : "");
                row.createCell(9).setCellValue(r.getSkipReason() != null ? r.getSkipReason() : "");
                row.createCell(10).setCellValue(r.getRemark() != null ? r.getRemark() : "");
                rowIdx++;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "medication_records_" + LocalDate.now() + ".xlsx";
            httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + java.net.URLEncoder.encode("用药记录_" + LocalDate.now() + ".xlsx", "UTF-8").replace("+", "%20"));

            return ResponseEntity.ok().headers(httpHeaders).body(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败: " + e.getMessage(), e);
        }
    }

    private String slotLabel(String slot) {
        if (slot == null) return "";
        return switch (slot) {
            case "MORNING" -> "早晨";
            case "AFTERNOON" -> "中午";
            case "EVENING" -> "晚上";
            case "BEDTIME" -> "睡前";
            default -> slot;
        };
    }

    private String statusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "待执行";
            case "DONE" -> "已确认";
            case "SKIPPED" -> "已跳过";
            case "REFUSED" -> "拒服";
            case "MISSED" -> "漏服";
            default -> status;
        };
    }

    @GetMapping("/records/today")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    public ResponseEntity<ApiResponse<List<TodayBoardVO>>> getTodayBoard(
            @RequestParam(required = false) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.success(medicationService.getTodayBoard(date)));
    }

    @PutMapping("/records/{id}/confirm")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "确认用药")
    public ResponseEntity<ApiResponse<Void>> confirmRecord(
            @PathVariable Long id,
            @RequestBody MedicationRecordActionRequest request
    ) {
        medicationService.confirmRecord(id, request.getExecutorId(), request.getExecutorName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/records/{id}/skip")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "跳过用药")
    public ResponseEntity<ApiResponse<Void>> skipRecord(
            @PathVariable Long id,
            @RequestBody MedicationRecordActionRequest request
    ) {
        medicationService.skipRecord(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/records/{id}/refuse")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "拒服用药")
    public ResponseEntity<ApiResponse<Void>> refuseRecord(
            @PathVariable Long id,
            @RequestBody MedicationRecordActionRequest request
    ) {
        medicationService.refuseRecord(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/records/confirm-batch")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "批量确认用药")
    public ResponseEntity<ApiResponse<Void>> confirmBatch(
            @RequestBody MedicationRecordBatchConfirmRequest request
    ) {
        medicationService.confirmBatch(request.getRecordIds(), request.getExecutorId(), request.getExecutorName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/records/batch-confirm")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "批量确认用药(日期)")
    public ResponseEntity<ApiResponse<Void>> confirmBatchByDate(
            @RequestBody MedicationRecordBatchConfirmRequest request
    ) {
        medicationService.confirmBatchByDate(request.getDate(), request.getExecutorId(), request.getExecutorName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/records/{id}/supplement")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    @OpLog(module = "用药管理", operation = "补录用药")
    public ResponseEntity<ApiResponse<Void>> supplementRecord(
            @PathVariable Long id,
            @RequestBody MedicationRecordSupplementRequest request
    ) {
        medicationService.supplementRecord(id, request.getSupplementType(), request.getExecutorId(), request.getExecutorName(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/records/elderly/{elderlyId}")
    @PreAuthorize("hasAuthority('pharmacy:medication')")
    public ResponseEntity<ApiResponse<List<MedicationRecordVO>>> getElderlyRecords(
            @PathVariable Long elderlyId,
            @RequestParam(defaultValue = "7") int days
    ) {
        return ResponseEntity.ok(ApiResponse.success(medicationService.getElderlyRecords(elderlyId, days)));
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
