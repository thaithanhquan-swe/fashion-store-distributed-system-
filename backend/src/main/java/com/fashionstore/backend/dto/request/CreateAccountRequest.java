package com.fashionstore.backend.dto.request;

import com.fashionstore.backend.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 10) String employeeId,
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role) {
}
