package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.response.InventoryResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.service.InventoryService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tồn kho", description = "API xem tồn kho theo chi nhánh")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryController {

    InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Xem tồn kho của chi nhánh nhân viên đang đăng nhập")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getCurrentBranch(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ok("Lấy danh sách tồn kho thành công",
                inventoryService.getCurrentBranch(authentication.getName(), pageable(page, pageSize)));
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Xem tồn kho một sản phẩm tại chi nhánh hiện tại")
    public ResponseEntity<ApiResponse<InventoryResponse>> getProduct(
            Authentication authentication, @PathVariable String productId) {
        return ok("Lấy thông tin tồn kho thành công",
                inventoryService.getProduct(authentication.getName(), productId));
    }

    @GetMapping("/branches/{branchId}")
    @Operation(summary = "Xem tồn kho chi nhánh; ADMIN và MANAGER được xem chi nhánh khác")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getBranch(
            Authentication authentication, @PathVariable String branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ok("Lấy danh sách tồn kho thành công",
                inventoryService.getBranch(authentication.getName(), branchId, pageable(page, pageSize)));
    }

    private Pageable pageable(int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        return PageRequest.of(page, pageSize, Sort.by("productId"));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T result) {
        return ResponseEntity.ok(ApiResponse.<T>builder().message(message).result(result).build());
    }
}
