package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.response.dashboard.AdminDashboardResponse;
import com.fashionstore.backend.dto.response.dashboard.InventorySummaryDto;
import com.fashionstore.backend.dto.response.dashboard.ManagerDashboardResponse;
import com.fashionstore.backend.dto.response.dashboard.RevenueSummaryDto;
import com.fashionstore.backend.dto.response.dashboard.StaffDashboardResponse;
import com.fashionstore.backend.service.DashboardService;
import java.math.BigDecimal;
import java.util.Collections;
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
class DashboardControllerTest {

    @Mock
    DashboardService dashboardService;

    @Mock
    Authentication authentication;

    @InjectMocks
    DashboardController dashboardController;

    AdminDashboardResponse sampleAdmin;
    ManagerDashboardResponse sampleManager;
    StaffDashboardResponse sampleStaff;

    @BeforeEach
    void setUp() {
        RevenueSummaryDto rev = new RevenueSummaryDto(
                BigDecimal.valueOf(1000000), 10, 8,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(33333),
                BigDecimal.valueOf(800000), 8, 25.0, 25.0, 14.3);
        InventorySummaryDto inv = new InventorySummaryDto(
                20, 100, 15, 3, 2, BigDecimal.valueOf(5000000));

        sampleAdmin = new AdminDashboardResponse(
                rev, inv,
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), 0, Collections.emptyList());

        sampleManager = new ManagerDashboardResponse(
                "CN01", "Chi nhánh Quận 1",
                rev, inv,
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());

        sampleStaff = new StaffDashboardResponse(
                "NV001", "Nguyen Van A", "CN01", "Chi nhánh Quận 1",
                BigDecimal.valueOf(500000), 5, BigDecimal.valueOf(2000000), 20, Collections.emptyList());
    }

    @Test
    void getAdminDashboard_success() {
        when(dashboardService.getAdminDashboard(30)).thenReturn(sampleAdmin);

        ResponseEntity<ApiResponse<AdminDashboardResponse>> response =
                dashboardController.getAdminDashboard(30);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy dữ liệu tổng quan toàn hệ thống thành công", response.getBody().getMessage());
        assertEquals(BigDecimal.valueOf(1000000), response.getBody().getResult().summary().revenue());
        verify(dashboardService).getAdminDashboard(30);
    }

    @Test
    void getManagerDashboard_success() {
        when(authentication.getName()).thenReturn("manager.q1");
        when(dashboardService.getManagerDashboard("manager.q1", "CN01", 30)).thenReturn(sampleManager);

        ResponseEntity<ApiResponse<ManagerDashboardResponse>> response =
                dashboardController.getManagerDashboard(authentication, "CN01", 30);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy dữ liệu tổng quan chi nhánh thành công", response.getBody().getMessage());
        assertEquals("CN01", response.getBody().getResult().branchId());
        verify(dashboardService).getManagerDashboard("manager.q1", "CN01", 30);
    }

    @Test
    void getStaffDashboard_success() {
        when(authentication.getName()).thenReturn("staff.q1");
        when(dashboardService.getStaffDashboard("staff.q1")).thenReturn(sampleStaff);

        ResponseEntity<ApiResponse<StaffDashboardResponse>> response =
                dashboardController.getStaffDashboard(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy dữ liệu làm việc nhân viên thành công", response.getBody().getMessage());
        assertEquals("NV001", response.getBody().getResult().employeeId());
        verify(dashboardService).getStaffDashboard("staff.q1");
    }
}
