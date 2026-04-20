package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.dto.pharmacy.ExpiryWarningItem;
import com.hfnew.service.PharmacyWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
public class PharmacyWarningController {

    private final PharmacyWarningService pharmacyWarningService;

    @GetMapping("/expiry-warnings")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<List<ExpiryWarningItem>>> expiryWarnings(@RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(ApiResponse.success(pharmacyWarningService.listExpiryWarnings(days)));
    }
}
