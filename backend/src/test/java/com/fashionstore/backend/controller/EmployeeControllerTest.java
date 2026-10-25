package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CreateEmployeeRequest;
import com.fashionstore.backend.dto.request.UpdateEmployeeRequest;
import com.fashionstore.backend.dto.request.UpdateEmployeeStatusRequest;
import com.fashionstore.backend.dto.response.EmployeeResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.service.EmployeeService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    EmployeeService employeeService;

    @Mock
    Authentication authentication;

    @InjectMocks
    EmployeeController employeeController;

    EmployeeResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new EmployeeResponse(
                "NV001", "CN01", "Nguyen Van A",
                LocalDate.of(1990, 1, 1), "Nam", "0901234567",
                "a@fashionstore.com", "Sales", true);
    }

    @Test
    void list_success() {
        when(authentication.getName()).thenReturn("admin");
        PageResponse<EmployeeResponse> page = PageResponse.<EmployeeResponse>builder()
                .content(List.of(sampleResponse))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
        when(employeeService.list(eq("admin"), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> response =
                employeeController.list(authentication, null, null, null, null, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy danh sách nhân viên thành công", response.getBody().getMessage());
        assertEquals(1, response.getBody().getResult().getContent().size());
    }

    @Test
    void getById_success() {
        when(authentication.getName()).thenReturn("admin");
        when(employeeService.getById("admin", "NV001")).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<EmployeeResponse>> response =
                employeeController.getById(authentication, "NV001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy thông tin nhân viên thành công", response.getBody().getMessage());
        assertEquals("NV001", response.getBody().getResult().employeeId());
    }

    @Test
    void create_success() {
        when(authentication.getName()).thenReturn("admin");
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "CN01", "Nguyen Van A", LocalDate.of(1990, 1, 1), "Nam", "0901234567", "a@fashionstore.com", "Sales");
        when(employeeService.create("admin", request)).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<EmployeeResponse>> response =
                employeeController.create(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Tạo nhân viên thành công", response.getBody().getMessage());
        assertEquals("NV001", response.getBody().getResult().employeeId());
    }

    @Test
    void update_success() {
        when(authentication.getName()).thenReturn("admin");
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "CN01", "Nguyen Van A Updated", LocalDate.of(1990, 1, 1), "Nam", "0901234567", "a@fashionstore.com", "Sales");
        when(employeeService.update("admin", "NV001", request)).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<EmployeeResponse>> response =
                employeeController.update(authentication, "NV001", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Cập nhật thông tin nhân viên thành công", response.getBody().getMessage());
    }

    @Test
    void updateStatus_success() {
        when(authentication.getName()).thenReturn("admin");
        UpdateEmployeeStatusRequest request = new UpdateEmployeeStatusRequest(false, null);
        EmployeeResponse locked = new EmployeeResponse(
                "NV001", "CN01", "Nguyen Van A",
                LocalDate.of(1990, 1, 1), "Nam", "0901234567",
                "a@fashionstore.com", "Sales", false);
        when(employeeService.updateStatus("admin", "NV001", request)).thenReturn(locked);

        ResponseEntity<ApiResponse<EmployeeResponse>> response =
                employeeController.updateStatus(authentication, "NV001", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Cập nhật trạng thái nhân viên thành công", response.getBody().getMessage());
        assertFalse(response.getBody().getResult().active());
        assertEquals("RESIGNED", response.getBody().getResult().status());
    }
}
