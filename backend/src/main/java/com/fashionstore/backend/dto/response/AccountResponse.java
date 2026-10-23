package com.fashionstore.backend.dto.response;

import com.fashionstore.backend.enums.Role;

public record AccountResponse(
        Integer accountId,
        String employeeId,
        String username,
        Role role,
        Boolean active) {
}
