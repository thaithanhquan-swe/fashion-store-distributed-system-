package com.fashionstore.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ImportInventoryRequest(
        @NotBlank @Size(max = 10) String productId,
        @NotNull @Positive Integer quantity) {
}
