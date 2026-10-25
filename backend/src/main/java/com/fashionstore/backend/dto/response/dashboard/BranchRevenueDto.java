package com.fashionstore.backend.dto.response.dashboard;

import java.math.BigDecimal;

public record BranchRevenueDto(
        String branchId,
        String branchName,
        BigDecimal revenue,
        long orders,
        double share,
        BigDecimal averageOrderValue) {
}
