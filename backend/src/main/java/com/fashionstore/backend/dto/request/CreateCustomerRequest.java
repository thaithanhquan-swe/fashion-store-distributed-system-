package com.fashionstore.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 15) String phoneNumber,
        @Email @Size(max = 100) String email,
        @Size(max = 255) String address) {
}
