package com.fashionstore.backend.dto.response;

import lombok.Builder;

@Builder
public record SiteStatusResponse(
        String siteId,
        String name,
        String kind,
        String branchId,
        String host,
        Integer baseLatency,
        Integer latencyMs,
        String link,
        String status,
        String lastSyncAt,
        Integer pending,
        String downSince) {}
