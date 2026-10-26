package com.fashionstore.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateAccountStatusRequest(
        @JsonAlias({"active", "trangThai"}) Boolean active,
        @JsonAlias({"status"}) String status) {
}
