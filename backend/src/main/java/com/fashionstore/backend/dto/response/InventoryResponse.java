package com.fashionstore.backend.dto.response;

import java.time.LocalDateTime;

public record InventoryResponse(
        String branchId,
        String productId,
        Integer quantity,
        Integer minimumStock,
        LocalDateTime updatedAt,
        boolean lowStock) {
}
