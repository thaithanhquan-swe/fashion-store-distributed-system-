package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.response.dashboard.BranchRevenueDto;
import com.fashionstore.backend.dto.response.statistics.EmployeeStatisticDto;
import com.fashionstore.backend.dto.response.statistics.RevenueStatisticsResponse;
import com.fashionstore.backend.dto.response.statistics.TopProductStatisticDto;
import com.fashionstore.backend.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/statistics", "/statistics"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Thống kê", description = "API báo cáo thống kê doanh thu, sản phẩm và nhân viên")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticsController {

    StatisticsService statisticsService;

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Thống kê doanh thu theo khoảng thời gian")
    public ResponseEntity<ApiResponse<RevenueStatisticsResponse>> getRevenue(
            Authentication authentication,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        RevenueStatisticsResponse result = statisticsService.getRevenueStatistics(
                authentication.getName(), branchId, days, from, to);
        return ResponseEntity.ok(ApiResponse.<RevenueStatisticsResponse>builder()
                .message("Lấy thống kê doanh thu thành công")
                .result(result)
                .build());
    }

    @GetMapping("/revenue/branches")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "So sánh doanh thu giữa các chi nhánh")
    public ResponseEntity<ApiResponse<List<BranchRevenueDto>>> getRevenueByBranches(
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<BranchRevenueDto> result = statisticsService.getBranchRevenueStatistics(days, from, to);
        return ResponseEntity.ok(ApiResponse.<List<BranchRevenueDto>>builder()
                .message("So sánh doanh thu giữa các chi nhánh thành công")
                .result(result)
                .build());
    }

    @GetMapping("/revenue/branches/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Xem doanh thu của một chi nhánh")
    public ResponseEntity<ApiResponse<RevenueStatisticsResponse>> getBranchRevenue(
            Authentication authentication,
            @PathVariable String branchId,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        RevenueStatisticsResponse result = statisticsService.getBranchRevenueDetail(
                authentication.getName(), branchId, days, from, to);
        return ResponseEntity.ok(ApiResponse.<RevenueStatisticsResponse>builder()
                .message("Lấy doanh thu chi nhánh thành công")
                .result(result)
                .build());
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Thống kê sản phẩm bán chạy")
    public ResponseEntity<ApiResponse<List<TopProductStatisticDto>>> getTopProducts(
            Authentication authentication,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "revenue") String sortBy) {
        List<TopProductStatisticDto> result = statisticsService.getTopProducts(
                authentication.getName(), branchId, days, limit, sortBy);
        return ResponseEntity.ok(ApiResponse.<List<TopProductStatisticDto>>builder()
                .message("Lấy danh sách sản phẩm bán chạy thành công")
                .result(result)
                .build());
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Thống kê doanh số theo nhân viên")
    public ResponseEntity<ApiResponse<List<EmployeeStatisticDto>>> getEmployees(
            Authentication authentication,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        List<EmployeeStatisticDto> result = statisticsService.getEmployeeSales(
                authentication.getName(), branchId, days, limit);
        return ResponseEntity.ok(ApiResponse.<List<EmployeeStatisticDto>>builder()
                .message("Lấy thống kê doanh số nhân viên thành công")
                .result(result)
                .build());
    }
}
