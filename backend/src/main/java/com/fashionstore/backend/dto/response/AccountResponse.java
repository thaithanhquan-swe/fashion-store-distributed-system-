package com.fashionstore.backend.dto.response;

import com.fashionstore.backend.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountResponse(
        Integer accountId,
        String employeeId,
        String username,
        Role role,
        Boolean active,
        String employeeName,
        String branchId) {

    public AccountResponse(Integer accountId, String employeeId, String username, Role role, Boolean active) {
        this(accountId, employeeId, username, role, active, null, null);
    }

    @JsonProperty("status")
    public String status() {
        return Boolean.TRUE.equals(active) ? "ACTIVE" : "LOCKED";
    }

    @JsonProperty("fullName")
    public String fullName() {
        return employeeName;
    }

    @JsonProperty("maTaiKhoan")
    public Integer maTaiKhoan() {
        return accountId;
    }

    @JsonProperty("tenDangNhap")
    public String tenDangNhap() {
        return username;
    }

    @JsonProperty("maNhanVien")
    public String maNhanVien() {
        return employeeId;
    }

    @JsonProperty("vaiTro")
    public String vaiTro() {
        return role != null ? role.name() : null;
    }

    @JsonProperty("trangThai")
    public Boolean trangThai() {
        return active;
    }
}
