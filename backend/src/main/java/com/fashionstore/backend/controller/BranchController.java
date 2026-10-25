package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.response.BranchResponse;
import com.fashionstore.backend.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/branches", "/branches"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chi nhánh", description = "API thông tin chi nhánh")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BranchController {

    BranchService branchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lấy danh sách tất cả chi nhánh")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAll(
            @RequestParam(required = false) Boolean active) {
        List<BranchResponse> result = branchService.getAll(active);
        return ResponseEntity.ok(ApiResponse.<List<BranchResponse>>builder()
                .message("Lấy danh sách chi nhánh thành công")
                .result(result)
                .build());
    }

    @GetMapping("/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Xem thông tin chi tiết một chi nhánh")
    public ResponseEntity<ApiResponse<BranchResponse>> getById(
            @PathVariable String branchId) {
        BranchResponse result = branchService.getById(branchId);
        return ResponseEntity.ok(ApiResponse.<BranchResponse>builder()
                .message("Lấy thông tin chi nhánh thành công")
                .result(result)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Tạo chi nhánh")
    public ResponseEntity<ApiResponse<BranchResponse>> create(
            @Valid @RequestBody CreateBranchRequest request) {
        BranchResponse result = branchService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BranchResponse>builder()
                        .message("Tạo chi nhánh thành công")
                        .result(result)
                        .build());
    }
}
