package com.fashionstore.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateEmployeeStatusRequest(
        @JsonAlias({"active", "trangThai", "TrangThai"}) Boolean active,
        @JsonAlias({"status", "trangThaiStr"}) String status) {
}
