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
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/accounts", "/accounts"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tài khoản", description = "API quản lý tài khoản nhân viên")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountController {

    AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy danh sách tài khoản nhân viên")
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> list(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Pageable pageable = pageable(page, pageSize);
        PageResponse<AccountResponse> result = accountService.list(
                authentication.getName(), keyword, branchId, role, active, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<AccountResponse>>builder()
                .message("Lấy danh sách tài khoản thành công")
                .result(result)
                .build());
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Xem chi tiết tài khoản")
    public ResponseEntity<ApiResponse<AccountResponse>> getById(
            Authentication authentication,
            @PathVariable String accountId) {
        AccountResponse result = accountService.getById(authentication.getName(), accountId);
        return ResponseEntity.ok(ApiResponse.<AccountResponse>builder()
                .message("Lấy thông tin tài khoản thành công")
                .result(result)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo tài khoản cho nhân viên")
    public ResponseEntity<ApiResponse<AccountResponse>> create(
            Authentication authentication,
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse result = accountService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AccountResponse>builder()
                        .message("Tạo tài khoản thành công")
                        .result(result)
                        .build());
    }

    @PatchMapping("/{accountId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thay đổi quyền của tài khoản")
    public ResponseEntity<ApiResponse<AccountResponse>> updateRole(
            Authentication authentication,
            @PathVariable String accountId,
            @Valid @RequestBody UpdateAccountRoleRequest request) {
        AccountResponse result = accountService.updateRole(authentication.getName(), accountId, request);
        return ResponseEntity.ok(ApiResponse.<AccountResponse>builder()
                .message("Thay đổi quyền tài khoản thành công")
                .result(result)
                .build());
    }

    @PatchMapping("/{accountId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Khóa hoặc mở tài khoản")
    public ResponseEntity<ApiResponse<AccountResponse>> updateStatus(
            Authentication authentication,
            @PathVariable String accountId,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        AccountResponse result = accountService.updateStatus(authentication.getName(), accountId, request);
        return ResponseEntity.ok(ApiResponse.<AccountResponse>builder()
                .message("Cập nhật trạng thái tài khoản thành công")
                .result(result)
                .build());
    }

    @PatchMapping("/{accountId}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Reset mật khẩu tài khoản")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(
            Authentication authentication,
            @PathVariable String accountId,
            @Valid @RequestBody(required = false) ResetPasswordRequest request) {
        ResetPasswordResponse result = accountService.resetPassword(authentication.getName(), accountId, request);
        return ResponseEntity.ok(ApiResponse.<ResetPasswordResponse>builder()
                .message("Reset mật khẩu tài khoản thành công")
                .result(result)
                .build());
    }

    private Pageable pageable(int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        return PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "accountId"));
    }
}
