package com.fashionstore.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResetPasswordResponse(
        Integer accountId,
        String username,
        String tempPassword,
        String message) {

    @JsonProperty("password")
    public String password() {
        return tempPassword;
    }

    @JsonProperty("maTaiKhoan")
    public Integer maTaiKhoan() {
        return accountId;
    }

    @JsonProperty("tenDangNhap")
    public String tenDangNhap() {
        return username;
    }
}
