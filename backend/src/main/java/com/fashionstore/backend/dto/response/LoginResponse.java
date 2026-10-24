package com.fashionstore.backend.dto.response;

import com.fashionstore.backend.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        @JsonProperty("token") String token,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("refresh_expires_in") long refreshExpiresIn,
        Integer accountId,
        String employeeId,
        String username,
        Role role) {
}
