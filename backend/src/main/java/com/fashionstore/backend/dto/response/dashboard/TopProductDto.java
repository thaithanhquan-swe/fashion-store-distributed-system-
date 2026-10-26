package com.fashionstore.backend.dto.response.dashboard;

import java.math.BigDecimal;

public record TopProductDto(
        String productId,
        String productName,
        long quantity,
        BigDecimal revenue,
        int rank) {
}
