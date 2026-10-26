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
        Role role,
        String fullName,
        String branchId) {

    public LoginResponse(
            String token,
            String refreshToken,
            String tokenType,
            long expiresIn,
            long refreshExpiresIn,
            Integer accountId,
            String employeeId,
            String username,
            Role role) {
        this(token, refreshToken, tokenType, expiresIn, refreshExpiresIn, accountId, employeeId, username, role, null, null);
    }

    @JsonProperty("name")
    public String name() {
        return fullName != null ? fullName : username;
    }
}
