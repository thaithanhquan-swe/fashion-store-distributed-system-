package com.fashionstore.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CheckoutRequest(
        @Size(max = 10) String customerId,
        @NotBlank @Size(max = 30) String paymentMethod,
        @NotEmpty List<@Valid SaleItemRequest> items) {}
