package com.fashionstore.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CheckStockRequest(@NotEmpty List<@Valid SaleItemRequest> items) {}
