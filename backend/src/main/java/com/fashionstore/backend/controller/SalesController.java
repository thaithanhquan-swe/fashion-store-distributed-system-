package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CheckStockRequest;
import com.fashionstore.backend.dto.request.CheckoutRequest;
import com.fashionstore.backend.dto.response.CheckoutResponse;
import com.fashionstore.backend.dto.response.StockCheckResponse;
import com.fashionstore.backend.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bán hàng", description = "Kiểm tra tồn kho và thanh toán tại chi nhánh hiện tại")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SalesController {

    SalesService salesService;

    @PostMapping("/check-stock")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Kiểm tra tồn kho các sản phẩm tại chi nhánh hiện tại")
    public ResponseEntity<ApiResponse<StockCheckResponse>> checkStock(
            Authentication authentication, @Valid @RequestBody CheckStockRequest request) {
        return ok("Kiểm tra tồn kho thành công",
                salesService.checkStock(authentication.getName(), request));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @Operation(summary = "Thanh toán, tạo hóa đơn, trừ tồn kho và cộng điểm khách hàng")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            Authentication authentication, @Valid @RequestBody CheckoutRequest request) {
        return ok("Thanh toán thành công", salesService.checkout(authentication.getName(), request));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T result) {
        return ResponseEntity.ok(ApiResponse.<T>builder().message(message).result(result).build());
    }
}
