package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.response.CustomerInvoiceResponse;
import com.fashionstore.backend.dto.response.InvoiceResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hóa đơn", description = "Tra cứu hóa đơn và xử lý hủy hóa đơn tại chi nhánh")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceController {

    InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lấy danh sách hóa đơn; Staff và Manager chỉ xem chi nhánh hiện tại")
    public ResponseEntity<ApiResponse<PageResponse<CustomerInvoiceResponse>>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Pageable pageable = pageable(page, pageSize);
        return ok("Lấy danh sách hóa đơn thành công",
                invoiceService.list(authentication.getName(), pageable));
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Xem chi tiết hóa đơn và sản phẩm đã mua")
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(
            Authentication authentication, @PathVariable String invoiceId) {
        return ok("Lấy chi tiết hóa đơn thành công",
                invoiceService.get(authentication.getName(), invoiceId));
    }

    @PatchMapping("/{invoiceId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Hủy hóa đơn đã thanh toán, hoàn kho và thu hồi điểm tích lũy")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancel(
            Authentication authentication, @PathVariable String invoiceId) {
        return ok("Hủy hóa đơn và hoàn tồn kho thành công",
                invoiceService.cancel(authentication.getName(), invoiceId));
    }

    private Pageable pageable(int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        return PageRequest.of(page, pageSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("invoiceId")));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T result) {
        return ResponseEntity.ok(ApiResponse.<T>builder().message(message).result(result).build());
    }
}
