package com.fashionstore.backend.dto.response.dashboard;

import java.math.BigDecimal;

public record RevenueSummaryDto(
        BigDecimal revenue,
        long orders,
        long customers,
        BigDecimal averageOrderValue,
        BigDecimal averageDailyRevenue,
        BigDecimal previousRevenue,
        long previousOrders,
        double revenueChangePercent,
        double ordersChangePercent,
        double customersChangePercent) {
}
