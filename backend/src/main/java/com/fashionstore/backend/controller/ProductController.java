package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.request.CreateProductRequest;
import com.fashionstore.backend.dto.request.UpdateProductRequest;
import com.fashionstore.backend.dto.request.UpdateProductStatusRequest;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.dto.response.ProductResponse;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/products")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sản phẩm", description = "API tìm kiếm và quản lý sản phẩm")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {

    ProductService productService;

    @GetMapping
    @Operation(summary = "Lấy danh sách sản phẩm, hỗ trợ tìm kiếm và lọc")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        Pageable pageable = PageRequest.of(
                page, pageSize, Sort.by(Sort.Direction.ASC, "productId"));
        PageResponse<ProductResponse> result = productService.search(
                keyword, category, brand, color, size, active, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<ProductResponse>>builder()
                .message("Lấy danh sách sản phẩm thành công")
                .result(result)
                .build());
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Xem chi tiết sản phẩm")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable String productId) {
        ProductResponse result = productService.getById(productId);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message("Lấy thông tin sản phẩm thành công")
                .result(result)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Thêm sản phẩm")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse result = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ProductResponse>builder()
                        .message("Tạo sản phẩm thành công")
                        .result(result)
                        .build());
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật thông tin sản phẩm")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse result = productService.update(productId, request);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message("Cập nhật sản phẩm thành công")
                .result(result)
                .build());
    }

    @PatchMapping("/{productId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Khóa hoặc mở hoạt động sản phẩm")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStatus(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        ProductResponse result = productService.updateStatus(productId, request);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message("Cập nhật trạng thái sản phẩm thành công")
                .result(result)
                .build());
    }
}
