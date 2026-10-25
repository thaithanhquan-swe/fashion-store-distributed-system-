package com.fashionstore.backend.dto.response.dashboard;

import java.math.BigDecimal;

public record InventorySummaryDto(
        long totalSkus,
        long totalQuantity,
        long available,
        long lowStock,
        long outOfStock,
        BigDecimal totalValue) {
}
