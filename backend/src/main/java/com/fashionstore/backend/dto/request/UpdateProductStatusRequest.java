package com.fashionstore.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(@NotNull Boolean active) {
}
