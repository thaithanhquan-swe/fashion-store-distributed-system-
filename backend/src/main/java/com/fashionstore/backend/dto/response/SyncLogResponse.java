package com.fashionstore.backend.dto.response;

import lombok.Builder;

@Builder
public record SyncLogResponse(
        String id,
        String at,
        String siteId,
        String mode,
        int records,
        long durationMs,
        String result,
        String message) {}
