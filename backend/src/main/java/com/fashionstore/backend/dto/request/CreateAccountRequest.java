package com.fashionstore.backend.dto.request;

import com.fashionstore.backend.enums.Role;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 10) @JsonAlias({"employeeId", "maNhanVien"}) String employeeId,
        @NotBlank @Size(max = 50) @JsonAlias({"username", "tenDangNhap"}) String username,
        @NotBlank @Size(min = 6, max = 72) @JsonAlias({"password", "matKhau"}) String password,
        @NotNull @JsonAlias({"role", "vaiTro"}) Role role) {
}
