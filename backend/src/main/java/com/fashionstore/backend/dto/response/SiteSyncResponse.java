package com.fashionstore.backend.dto.response;

import lombok.Builder;

@Builder
public record SiteSyncResponse(
        int records,
        long durationMs,
        String result,
        String message) {}
