package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.dto.elderly.BedOption;
import com.hfnew.service.BedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beds")
@RequiredArgsConstructor
public class BedController {

    private final BedService bedService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<BedOption>>> available() {
        return ResponseEntity.ok(ApiResponse.success(bedService.listAvailable()));
    }
}
