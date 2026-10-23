package com.fashionstore.backend.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
        String productId,
        String productName,
        String category,
        String brand,
        String color,
        String size,
        BigDecimal price,
        Boolean active) {
}
