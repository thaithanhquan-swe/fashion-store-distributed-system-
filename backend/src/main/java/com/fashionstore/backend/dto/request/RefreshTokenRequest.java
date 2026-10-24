package com.fashionstore.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @JsonProperty("refresh_token")
        @NotBlank(message = "Refresh token không được để trống") String refreshToken) {
}
