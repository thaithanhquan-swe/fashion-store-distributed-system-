package com.fashionstore.backend.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateInventoryRequest(
        @PositiveOrZero Integer quantity,
        @PositiveOrZero Integer minimumStock) {
}
