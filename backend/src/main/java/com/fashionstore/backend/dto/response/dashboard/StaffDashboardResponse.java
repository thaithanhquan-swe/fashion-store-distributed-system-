package com.fashionstore.backend.dto.response.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record StaffDashboardResponse(
        String employeeId,
        String employeeName,
        String branchId,
        String branchName,
        BigDecimal todayRevenue,
        long todayOrders,
        BigDecimal monthRevenue,
        long monthOrders,
        List<RecentInvoiceDto> recentInvoices) {
}
