package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CreateCustomerRequest;
import com.fashionstore.backend.dto.request.UpdateCustomerRequest;
import com.fashionstore.backend.dto.request.UpdateCustomerStatusRequest;
import com.fashionstore.backend.dto.response.CustomerInvoiceResponse;
import com.fashionstore.backend.dto.response.CustomerResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.service.CustomerService;
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
@RequestMapping("/api/customers")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Khách hàng", description = "API quản lý khách hàng và lịch sử mua hàng")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerController {

    CustomerService customerService;

    @GetMapping
    @Operation(summary = "Lấy danh sách khách hàng")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<CustomerResponse> result = customerService.list(active, customerPage(page, pageSize));
        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerResponse>>builder()
                .message("Lấy danh sách khách hàng thành công").result(result).build());
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm khách hàng theo số điện thoại hoặc từ khóa")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> search(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<CustomerResponse> result = customerService.search(
                keyword, phone, active, customerPage(page, pageSize));
        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerResponse>>builder()
                .message("Tìm khách hàng thành công").result(result).build());
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Xem chi tiết khách hàng")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .message("Lấy thông tin khách hàng thành công")
                .result(customerService.getById(customerId)).build());
    }

    @PostMapping
    @Operation(summary = "Thêm khách hàng mới")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CustomerResponse>builder()
                        .message("Tạo khách hàng thành công")
                        .result(customerService.create(request)).build());
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Cập nhật thông tin khách hàng")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .message("Cập nhật khách hàng thành công")
                .result(customerService.update(customerId, request)).build());
    }

    @PatchMapping("/{customerId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Khóa hoặc mở trạng thái khách hàng")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateStatus(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .message("Cập nhật trạng thái khách hàng thành công")
                .result(customerService.updateStatus(customerId, request)).build());
    }

    @GetMapping("/{customerId}/invoices")
    @Operation(summary = "Xem lịch sử mua hàng của khách hàng")
    public ResponseEntity<ApiResponse<PageResponse<CustomerInvoiceResponse>>> getInvoices(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Pageable pageable = PageRequest.of(validatePage(page, pageSize), pageSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("invoiceId")));
        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerInvoiceResponse>>builder()
                .message("Lấy lịch sử mua hàng thành công")
                .result(customerService.getInvoices(customerId, pageable)).build());
    }

    private Pageable customerPage(int page, int pageSize) {
        return PageRequest.of(validatePage(page, pageSize), pageSize,
                Sort.by(Sort.Direction.ASC, "customerId"));
    }

    private int validatePage(int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        return page;
    }
}
