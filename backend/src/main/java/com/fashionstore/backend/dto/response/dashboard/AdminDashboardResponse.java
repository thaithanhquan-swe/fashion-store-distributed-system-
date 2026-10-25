package com.fashionstore.backend.dto.response.dashboard;

import java.util.List;

public record AdminDashboardResponse(
        RevenueSummaryDto summary,
        InventorySummaryDto inventory,
        List<DailyRevenueDto> revenueSeries,
        List<BranchRevenueDto> branchRevenue,
        List<TopProductDto> topProducts,
        List<LowStockItemDto> lowStockItems,
        long lowStockTotal,
        List<EmployeeSaleDto> employeeSales) {
}
