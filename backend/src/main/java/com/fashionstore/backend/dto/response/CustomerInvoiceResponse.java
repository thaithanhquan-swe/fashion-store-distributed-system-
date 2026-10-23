package com.fashionstore.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerInvoiceResponse(
        String invoiceId,
        String branchId,
        String customerId,
        String employeeId,
        LocalDateTime createdAt,
        BigDecimal totalAmount,
        String paymentMethod,
        String status) {
}
