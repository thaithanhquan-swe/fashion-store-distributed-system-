package com.fashionstore.backend.dto.response;

import lombok.Builder;

@Builder
public record SitePingResponse(
        boolean ok,
        Integer latencyMs) {}
