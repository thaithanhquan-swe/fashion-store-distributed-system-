package com.fashionstore.backend.dto.response;

import java.time.LocalDate;

public record CustomerResponse(
        String customerId,
        String fullName,
        String phoneNumber,
        String email,
        String address,
        Integer loyaltyPoints,
        LocalDate registeredDate,
        Boolean active) {
}
