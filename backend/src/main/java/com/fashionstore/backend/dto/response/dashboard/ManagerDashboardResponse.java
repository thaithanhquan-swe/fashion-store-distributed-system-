package com.fashionstore.backend.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ManagerDashboardResponse(
        String branchId,
        String branchName,
        RevenueSummaryDto summary,
        InventorySummaryDto inventory,
        List<DailyRevenueDto> series,
        List<TopProductDto> top,
        List<LowStockItemDto> low,
        List<EmployeeSaleDto> employees) {

    @JsonProperty("revenueSeries")
    public List<DailyRevenueDto> revenueSeries() {
        return series;
    }

    @JsonProperty("topProducts")
    public List<TopProductDto> topProducts() {
        return top;
    }

    @JsonProperty("lowStockItems")
    public List<LowStockItemDto> lowStockItems() {
        return low;
    }

    @JsonProperty("employeeSales")
    public List<EmployeeSaleDto> employeeSales() {
        return employees;
    }
}
