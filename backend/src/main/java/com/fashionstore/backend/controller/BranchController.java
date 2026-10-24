package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.response.BranchResponse;
import com.fashionstore.backend.service.BranchService;
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
@RequestMapping("/branches")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chi nhánh", description = "API quản lý chi nhánh")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BranchController {

    BranchService branchService;

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
