package com.fashionstore.backend.dto.response;

import java.math.BigDecimal;

public record CheckoutItemResponse(
        String productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal lineTotal) {}
