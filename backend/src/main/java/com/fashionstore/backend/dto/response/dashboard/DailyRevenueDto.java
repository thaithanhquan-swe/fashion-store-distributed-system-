package com.fashionstore.backend.dto.response.dashboard;

import java.math.BigDecimal;

public record DailyRevenueDto(
        String date,
        String dateLabel,
        BigDecimal revenue,
        long orders) {
}
