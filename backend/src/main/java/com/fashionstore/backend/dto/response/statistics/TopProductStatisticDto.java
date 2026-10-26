package com.fashionstore.backend.dto.response.statistics;

import java.math.BigDecimal;

public record TopProductStatisticDto(
        int rank,
        String productId,
        String productName,
        String category,
        String brand,
        long quantity,
        BigDecimal revenue) {
}
