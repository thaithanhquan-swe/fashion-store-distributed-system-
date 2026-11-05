package com.fashionstore.backend.dto.response;

import lombok.Builder;

@Builder
public record SiteSummaryResponse(
        String status,
        int offline,
        int degraded,
        int pending) {}
