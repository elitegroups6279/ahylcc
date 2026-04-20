package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.home.*;
import com.hfnew.service.HomeServiceItemService;
import com.hfnew.service.HomeServiceOrderService;
import com.hfnew.service.HomeServiceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home-service")
@RequiredArgsConstructor
public class HomeServiceController {

    private final HomeServiceItemService itemService;
    private final HomeServiceOrderService orderService;
    private final HomeServiceRecordService recordService;

    @GetMapping("/items/options")
    @PreAuthorize("hasAuthority('home-service:orders')")
    public ResponseEntity<ApiResponse<List<ServiceItemOption>>> itemOptions() {
        return ResponseEntity.ok(ApiResponse.success(itemService.options()));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('home-service:orders')")
    public ResponseEntity<ApiResponse<PageResult<HomeServiceOrderVO>>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.list(page, pageSize, status)));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('home-service:orders')")
    @OpLog(module = "上门服务", operation = "创建预约单")
    public ResponseEntity<ApiResponse<Long>> createOrder(@RequestBody HomeServiceOrderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.create(request)));
    }

    @PutMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('home-service:orders')")
    @OpLog(module = "上门服务", operation = "更新预约单")
    public ResponseEntity<ApiResponse<Object>> updateOrder(@PathVariable Long id, @RequestBody HomeServiceOrderUpdateRequest request) {
        orderService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/records")
    @PreAuthorize("hasAuthority('home-service:records')")
    public ResponseEntity<ApiResponse<PageResult<HomeServiceRecordVO>>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(recordService.list(page, pageSize, orderId)));
    }

    @PostMapping("/records")
    @PreAuthorize("hasAuthority('home-service:records')")
    @OpLog(module = "上门服务", operation = "提交服务记录")
    public ResponseEntity<ApiResponse<Long>> createRecord(@RequestBody HomeServiceRecordCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recordService.create(request)));
    }
}
