package com.fashionstore.backend.dto.response.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecentInvoiceDto(
        String invoiceId,
        LocalDateTime createdAt,
        BigDecimal totalAmount,
        String customerId,
        String paymentMethod,
        String status) {
}
