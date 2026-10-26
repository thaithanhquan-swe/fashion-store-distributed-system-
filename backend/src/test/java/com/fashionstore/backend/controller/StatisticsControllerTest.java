package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.response.dashboard.BranchRevenueDto;
import com.fashionstore.backend.dto.response.dashboard.RevenueSummaryDto;
import com.fashionstore.backend.dto.response.statistics.EmployeeStatisticDto;
import com.fashionstore.backend.dto.response.statistics.RevenueStatisticsResponse;
import com.fashionstore.backend.dto.response.statistics.TopProductStatisticDto;
import com.fashionstore.backend.service.StatisticsService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    @Mock
    StatisticsService statisticsService;

    @Mock
    Authentication authentication;

    @InjectMocks
    StatisticsController statisticsController;

    RevenueStatisticsResponse sampleRevenue;
    BranchRevenueDto sampleBranch;
    TopProductStatisticDto sampleProduct;
    EmployeeStatisticDto sampleEmployee;

    @BeforeEach
    void setUp() {
        RevenueSummaryDto summary = new RevenueSummaryDto(
                BigDecimal.valueOf(1000000), 10, 8,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(33333),
                BigDecimal.valueOf(800000), 8, 25.0, 25.0, 14.3);

        sampleRevenue = new RevenueStatisticsResponse(
                "CN01", "Chi nhánh Quận 1", summary, Collections.emptyList());

        sampleBranch = new BranchRevenueDto(
                "CN01", "Chi nhánh Quận 1", BigDecimal.valueOf(1000000), 10, 100.0, BigDecimal.valueOf(100000));

        sampleProduct = new TopProductStatisticDto(
                1, "SP001", "Ao Thun", "Thoi trang", "BrandX", 20, BigDecimal.valueOf(2000000));

        sampleEmployee = new EmployeeStatisticDto(
                1, "NV001", "Nguyen Van A", "CN01", "Chi nhánh Quận 1", "Sales", 10, BigDecimal.valueOf(1000000));
    }

    @Test
    void getRevenue_success() {
        when(authentication.getName()).thenReturn("admin");
        when(statisticsService.getRevenueStatistics("admin", "CN01", 30, null, null))
                .thenReturn(sampleRevenue);

        ResponseEntity<ApiResponse<RevenueStatisticsResponse>> response =
                statisticsController.getRevenue(authentication, "CN01", 30, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy thống kê doanh thu thành công", response.getBody().getMessage());
        assertEquals("CN01", response.getBody().getResult().branchId());
        verify(statisticsService).getRevenueStatistics("admin", "CN01", 30, null, null);
    }

    @Test
    void getRevenueByBranches_success() {
        when(statisticsService.getBranchRevenueStatistics(30, null, null))
                .thenReturn(List.of(sampleBranch));

        ResponseEntity<ApiResponse<List<BranchRevenueDto>>> response =
                statisticsController.getRevenueByBranches(30, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("So sánh doanh thu giữa các chi nhánh thành công", response.getBody().getMessage());
        assertEquals(1, response.getBody().getResult().size());
        assertEquals("CN01", response.getBody().getResult().get(0).branchId());
        verify(statisticsService).getBranchRevenueStatistics(30, null, null);
    }

    @Test
    void getBranchRevenue_success() {
        when(authentication.getName()).thenReturn("admin");
        when(statisticsService.getBranchRevenueDetail("admin", "CN01", 30, null, null))
                .thenReturn(sampleRevenue);

        ResponseEntity<ApiResponse<RevenueStatisticsResponse>> response =
                statisticsController.getBranchRevenue(authentication, "CN01", 30, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy doanh thu chi nhánh thành công", response.getBody().getMessage());
        assertEquals("CN01", response.getBody().getResult().branchId());
        verify(statisticsService).getBranchRevenueDetail("admin", "CN01", 30, null, null);
    }

    @Test
    void getTopProducts_success() {
        when(authentication.getName()).thenReturn("admin");
        when(statisticsService.getTopProducts("admin", "CN01", 30, 10, "revenue"))
                .thenReturn(List.of(sampleProduct));

        ResponseEntity<ApiResponse<List<TopProductStatisticDto>>> response =
                statisticsController.getTopProducts(authentication, "CN01", 30, 10, "revenue");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy danh sách sản phẩm bán chạy thành công", response.getBody().getMessage());
        assertEquals(1, response.getBody().getResult().size());
        assertEquals("SP001", response.getBody().getResult().get(0).productId());
        verify(statisticsService).getTopProducts("admin", "CN01", 30, 10, "revenue");
    }

    @Test
    void getEmployees_success() {
        when(authentication.getName()).thenReturn("admin");
        when(statisticsService.getEmployeeSales("admin", "CN01", 30, 50))
                .thenReturn(List.of(sampleEmployee));

        ResponseEntity<ApiResponse<List<EmployeeStatisticDto>>> response =
                statisticsController.getEmployees(authentication, "CN01", 30, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy thống kê doanh số nhân viên thành công", response.getBody().getMessage());
        assertEquals(1, response.getBody().getResult().size());
        assertEquals("NV001", response.getBody().getResult().get(0).employeeId());
        verify(statisticsService).getEmployeeSales("admin", "CN01", 30, 50);
    }
}
