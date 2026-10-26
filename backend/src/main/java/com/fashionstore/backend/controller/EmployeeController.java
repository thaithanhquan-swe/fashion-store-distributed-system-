package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CreateEmployeeRequest;
import com.fashionstore.backend.dto.request.UpdateEmployeeRequest;
import com.fashionstore.backend.dto.request.UpdateEmployeeStatusRequest;
import com.fashionstore.backend.dto.response.EmployeeResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.service.EmployeeService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/employees", "/employees"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Nhân viên", description = "API quản lý nhân viên")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeController {

    EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy danh sách nhân viên")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> list(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Pageable pageable = pageable(page, pageSize);
        PageResponse<EmployeeResponse> result = employeeService.list(
                authentication.getName(), keyword, branchId, position, active, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<EmployeeResponse>>builder()
                .message("Lấy danh sách nhân viên thành công")
                .result(result)
                .build());
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Xem chi tiết nhân viên")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(
            Authentication authentication,
            @PathVariable String employeeId) {
        EmployeeResponse result = employeeService.getById(authentication.getName(), employeeId);
        return ResponseEntity.ok(ApiResponse.<EmployeeResponse>builder()
                .message("Lấy thông tin nhân viên thành công")
                .result(result)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Thêm nhân viên mới")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            Authentication authentication,
            @Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse result = employeeService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<EmployeeResponse>builder()
                        .message("Tạo nhân viên thành công")
                        .result(result)
                        .build());
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật thông tin nhân viên")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            Authentication authentication,
            @PathVariable String employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        EmployeeResponse result = employeeService.update(authentication.getName(), employeeId, request);
        return ResponseEntity.ok(ApiResponse.<EmployeeResponse>builder()
                .message("Cập nhật thông tin nhân viên thành công")
                .result(result)
                .build());
    }

    @PatchMapping("/{employeeId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Khóa hoặc mở trạng thái nhân viên")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateStatus(
            Authentication authentication,
            @PathVariable String employeeId,
            @Valid @RequestBody UpdateEmployeeStatusRequest request) {
        EmployeeResponse result = employeeService.updateStatus(authentication.getName(), employeeId, request);
        return ResponseEntity.ok(ApiResponse.<EmployeeResponse>builder()
                .message("Cập nhật trạng thái nhân viên thành công")
                .result(result)
                .build());
    }

    private Pageable pageable(int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        return PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "employeeId"));
    }
}
