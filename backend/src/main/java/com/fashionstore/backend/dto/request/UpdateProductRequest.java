package com.fashionstore.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank @Size(max = 150) String productName,
        @Size(max = 50) String category,
        @Size(max = 100) String brand,
        @Size(max = 30) String color,
        @Size(max = 10) String size,
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal price) {
}
