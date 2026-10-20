package com.fashionstore.backend.dto.response;

import java.time.LocalDate;

public record EmployeeResponse(
        String employeeId,
        String branchId,
        String fullName,
        LocalDate birthDate,
        String gender,
        String phoneNumber,
        String email,
        String position,
        Boolean active) {
}
