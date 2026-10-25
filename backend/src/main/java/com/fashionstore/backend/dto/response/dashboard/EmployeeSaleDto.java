package com.fashionstore.backend.dto.response.dashboard;

import java.math.BigDecimal;

public record EmployeeSaleDto(
        String employeeId,
        String employeeName,
        String branchId,
        String branchName,
        long orders,
        BigDecimal revenue) {
}
