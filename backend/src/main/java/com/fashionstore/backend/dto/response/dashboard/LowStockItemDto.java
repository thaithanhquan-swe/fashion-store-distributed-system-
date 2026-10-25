package com.fashionstore.backend.dto.response.dashboard;

import java.time.LocalDateTime;

public record LowStockItemDto(
        String branchId,
        String branchName,
        String productId,
        String productName,
        int quantity,
        int minQuantity,
        String stockStatus,
        LocalDateTime updatedAt) {
}
