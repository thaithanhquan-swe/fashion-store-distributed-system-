package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CreateAccountRequest;
import com.fashionstore.backend.dto.response.AccountResponse;
import com.fashionstore.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tài khoản", description = "API quản lý tài khoản nhân viên")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountController {

    AccountService accountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Tạo tài khoản nhân viên")
    public ResponseEntity<ApiResponse<AccountResponse>> create(
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse result = accountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AccountResponse>builder()
                        .message("Tạo tài khoản thành công")
                        .result(result)
                        .build());
    }
}
