package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.response.BranchResponse;
import com.fashionstore.backend.service.BranchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchControllerTest {

    @Mock
    BranchService branchService;

    @InjectMocks
    BranchController branchController;

    BranchResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new BranchResponse(
                "CN01",
                "Chi nhánh Quận 1",
                "123 Le Loi, Q1, TP.HCM",
                "0901111111",
                true);
    }

    @Test
    void getAll_success() {
        when(branchService.getAll(true)).thenReturn(List.of(sampleResponse));

        ResponseEntity<ApiResponse<List<BranchResponse>>> response = branchController.getAll(true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy danh sách chi nhánh thành công", response.getBody().getMessage());
        assertEquals(1, response.getBody().getResult().size());
        assertEquals("CN01", response.getBody().getResult().get(0).branchId());
        verify(branchService).getAll(true);
    }

    @Test
    void getById_success() {
        when(branchService.getById("CN01")).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<BranchResponse>> response = branchController.getById("CN01");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy thông tin chi nhánh thành công", response.getBody().getMessage());
        assertEquals("CN01", response.getBody().getResult().branchId());
        verify(branchService).getById("CN01");
    }

    @Test
    void create_success() {
        CreateBranchRequest request = new CreateBranchRequest(
                "Chi nhánh Quận 1",
                "123 Le Loi, Q1, TP.HCM",
                "0901111111");

        when(branchService.create(request)).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<BranchResponse>> response = branchController.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Tạo chi nhánh thành công", response.getBody().getMessage());
        assertEquals("CN01", response.getBody().getResult().branchId());
        verify(branchService).create(request);
    }
}
