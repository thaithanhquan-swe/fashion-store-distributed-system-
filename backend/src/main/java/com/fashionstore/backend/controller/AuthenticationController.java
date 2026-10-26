package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.ChangePasswordRequest;
import com.fashionstore.backend.dto.request.LoginRequest;
import com.fashionstore.backend.dto.request.RefreshTokenRequest;
import com.fashionstore.backend.dto.response.AccountResponse;
import com.fashionstore.backend.dto.response.LoginResponse;
import com.fashionstore.backend.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/auth", "/auth"})
@Tag(name = "Xác thực", description = "API đăng nhập và xác thực tài khoản")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lấy thông tin tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<AccountResponse>> getCurrentAccount(Authentication authentication) {
        AccountResponse result = authenticationService.getCurrentAccount(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<AccountResponse>builder()
                .message("Lấy thông tin tài khoản thành công")
                .result(result)
                .build());
    }

    @PutMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Đổi mật khẩu tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đổi mật khẩu thành công")
                .build());
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng tài khoản nhân viên")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse result = authenticationService.login(request);
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .message("Đăng nhập thành công")
                .result(result)
                .build());
    }

    @PostMapping("/refresh_token")
    @Operation(summary = "Làm mới access token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse result = authenticationService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .message("Làm mới token thành công")
                .result(result)
                .build());
    }
}
