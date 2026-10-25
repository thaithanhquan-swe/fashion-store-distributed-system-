package com.fashionstore.backend.dto.response.statistics;

import com.fashionstore.backend.dto.response.dashboard.DailyRevenueDto;
import com.fashionstore.backend.dto.response.dashboard.RevenueSummaryDto;
import java.util.List;

public record RevenueStatisticsResponse(
        String branchId,
        String branchName,
        RevenueSummaryDto summary,
        List<DailyRevenueDto> series) {
}
