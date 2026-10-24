package com.fashionstore.backend.dto.response;

public record StockCheckItemResponse(
        String productId,
        int requestedQuantity,
        int availableQuantity,
        boolean available) {}
