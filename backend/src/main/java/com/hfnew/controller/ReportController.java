package com.hfnew.controller;

import com.hfnew.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/fee-summary.xlsx")
    @PreAuthorize("hasAuthority('report:export')")
    public ResponseEntity<byte[]> exportFeeSummary(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        String filename = "fee-summary_" + (month != null ? month : LocalDate.now().format(DateTimeFormatter.ISO_DATE)) + ".xlsx";
        byte[] bytes = reportService.exportFeeSummary(month, startDate, endDate);
        return buildExcel(bytes, filename);
    }

    @GetMapping("/roster.xlsx")
    @PreAuthorize("hasAuthority('report:export')")
    public ResponseEntity<byte[]> exportRoster(@RequestParam(required = false) String status) {
        String filename = "roster" + (status != null ? "_" + status : "") + ".xlsx";
        byte[] bytes = reportService.exportRoster(status);
        return buildExcel(bytes, filename);
    }

    @GetMapping("/attendance.xlsx")
    @PreAuthorize("hasAuthority('report:export')")
    public ResponseEntity<byte[]> exportAttendance(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long staffId
    ) {
        String filename = "attendance_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".xlsx";
        byte[] bytes = reportService.exportAttendance(startDate, endDate, staffId);
        return buildExcel(bytes, filename);
    }

    @GetMapping("/material-consumption.xlsx")
    @PreAuthorize("hasAuthority('report:export')")
    public ResponseEntity<byte[]> exportMaterialConsumption(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long materialId
    ) {
        String filename = "material-consumption_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".xlsx";
        byte[] bytes = reportService.exportMaterialConsumption(startDate, endDate, materialId);
        return buildExcel(bytes, filename);
    }

    private ResponseEntity<byte[]> buildExcel(byte[] bytes, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}

