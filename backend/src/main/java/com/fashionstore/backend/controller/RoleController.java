package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Vai trò", description = "Danh sách vai trò cố định của hệ thống")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Lấy danh sách vai trò")
    public ApiResponse<List<Role>> getAll() {
        return ApiResponse.<List<Role>>builder()
                .message("Lấy danh sách vai trò thành công")
                .result(List.of(Role.values()))
                .build();
    }
}
