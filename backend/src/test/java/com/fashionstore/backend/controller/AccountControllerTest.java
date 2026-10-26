package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CreateAccountRequest;
import com.fashionstore.backend.dto.request.ResetPasswordRequest;
import com.fashionstore.backend.dto.request.UpdateAccountRoleRequest;
import com.fashionstore.backend.dto.request.UpdateAccountStatusRequest;
import com.fashionstore.backend.dto.response.AccountResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.dto.response.ResetPasswordResponse;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.service.AccountService;
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
class AccountControllerTest {

    @Mock
    AccountService accountService;

    @Mock
    Authentication authentication;

    @InjectMocks
    AccountController accountController;

    AccountResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new AccountResponse(
                1, "NV001", "user1", Role.STAFF, true, "Nguyen Van A", "CN01");
    }

    @Test
    void list_success() {
        when(authentication.getName()).thenReturn("admin");
        PageResponse<AccountResponse> page = PageResponse.<AccountResponse>builder()
                .content(List.of(sampleResponse))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
        when(accountService.list(eq("admin"), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> response =
                accountController.list(authentication, null, null, null, null, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy danh sách tài khoản thành công", response.getBody().getMessage());
        assertEquals(1, response.getBody().getResult().getContent().size());
    }

    @Test
    void getById_success() {
        when(authentication.getName()).thenReturn("admin");
        when(accountService.getById("admin", "1")).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<AccountResponse>> response =
                accountController.getById(authentication, "1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lấy thông tin tài khoản thành công", response.getBody().getMessage());
        assertEquals(1, response.getBody().getResult().accountId());
    }

    @Test
    void create_success() {
        when(authentication.getName()).thenReturn("admin");
        CreateAccountRequest request = new CreateAccountRequest("NV001", "user1", "secret123", Role.STAFF);
        when(accountService.create("admin", request)).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<AccountResponse>> response =
                accountController.create(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Tạo tài khoản thành công", response.getBody().getMessage());
    }

    @Test
    void updateRole_success() {
        when(authentication.getName()).thenReturn("admin");
        UpdateAccountRoleRequest request = new UpdateAccountRoleRequest(Role.MANAGER);
        AccountResponse updated = new AccountResponse(1, "NV001", "user1", Role.MANAGER, true, "Nguyen Van A", "CN01");
        when(accountService.updateRole("admin", "1", request)).thenReturn(updated);

        ResponseEntity<ApiResponse<AccountResponse>> response =
                accountController.updateRole(authentication, "1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Role.MANAGER, response.getBody().getResult().role());
    }

    @Test
    void updateStatus_success() {
        when(authentication.getName()).thenReturn("admin");
        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(false, null);
        AccountResponse locked = new AccountResponse(1, "NV001", "user1", Role.STAFF, false, "Nguyen Van A", "CN01");
        when(accountService.updateStatus("admin", "1", request)).thenReturn(locked);

        ResponseEntity<ApiResponse<AccountResponse>> response =
                accountController.updateStatus(authentication, "1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getResult().active());
        assertEquals("LOCKED", response.getBody().getResult().status());
    }

    @Test
    void resetPassword_success() {
        when(authentication.getName()).thenReturn("admin");
        ResetPasswordResponse resetRes = new ResetPasswordResponse(1, "user1", "FS123456", "Đặt lại mật khẩu thành công");
        when(accountService.resetPassword("admin", "1", null)).thenReturn(resetRes);

        ResponseEntity<ApiResponse<ResetPasswordResponse>> response =
                accountController.resetPassword(authentication, "1", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FS123456", response.getBody().getResult().tempPassword());
    }
}
