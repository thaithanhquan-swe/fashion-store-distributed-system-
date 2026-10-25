package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.response.dashboard.AdminDashboardResponse;
import com.fashionstore.backend.dto.response.dashboard.ManagerDashboardResponse;
import com.fashionstore.backend.dto.response.dashboard.StaffDashboardResponse;
import com.fashionstore.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/dashboard", "/dashboard"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "API tổng quan và số liệu thống kê Dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {

    DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy dữ liệu tổng quan toàn hệ thống cho Admin")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard(
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        AdminDashboardResponse result = dashboardService.getAdminDashboard(days);
        return ResponseEntity.ok(ApiResponse.<AdminDashboardResponse>builder()
                .message("Lấy dữ liệu tổng quan toàn hệ thống thành công")
                .result(result)
                .build());
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy dữ liệu tổng quan chi nhánh cho Manager")
    public ResponseEntity<ApiResponse<ManagerDashboardResponse>> getManagerDashboard(
            Authentication authentication,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        ManagerDashboardResponse result = dashboardService.getManagerDashboard(
                authentication.getName(), branchId, days);
        return ResponseEntity.ok(ApiResponse.<ManagerDashboardResponse>builder()
                .message("Lấy dữ liệu tổng quan chi nhánh thành công")
                .result(result)
                .build());
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lấy dữ liệu làm việc cá nhân cho Staff")
    public ResponseEntity<ApiResponse<StaffDashboardResponse>> getStaffDashboard(
            Authentication authentication) {
        StaffDashboardResponse result = dashboardService.getStaffDashboard(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<StaffDashboardResponse>builder()
                .message("Lấy dữ liệu làm việc nhân viên thành công")
                .result(result)
                .build());
    }
}
