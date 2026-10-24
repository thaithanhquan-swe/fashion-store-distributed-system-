package com.fashionstore.backend.dto.response;

import java.math.BigDecimal;

public record InvoiceDetailResponse(
        String productId,
        String productName,
        String category,
        String color,
        String size,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal lineTotal) {}
