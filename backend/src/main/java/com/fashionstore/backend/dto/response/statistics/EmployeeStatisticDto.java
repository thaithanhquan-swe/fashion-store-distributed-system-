package com.fashionstore.backend.dto.response.statistics;

import java.math.BigDecimal;

public record EmployeeStatisticDto(
        int rank,
        String employeeId,
        String employeeName,
        String branchId,
        String branchName,
        String position,
        long orders,
        BigDecimal revenue) {
}
