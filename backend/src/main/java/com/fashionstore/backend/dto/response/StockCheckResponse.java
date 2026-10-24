package com.fashionstore.backend.dto.response;

import java.util.List;

public record StockCheckResponse(boolean available, List<StockCheckItemResponse> items) {}
