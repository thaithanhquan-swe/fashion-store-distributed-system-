package com.fashionstore.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record EmployeeResponse(
        String employeeId,
        String branchId,
        String fullName,
        LocalDate birthDate,
        String gender,
        String phoneNumber,
        String email,
        String position,
        Boolean active) {

    @JsonProperty("employeeName")
    public String employeeName() {
        return fullName;
    }

    @JsonProperty("phone")
    public String phone() {
        return phoneNumber;
    }

    @JsonProperty("status")
    public String status() {
        return Boolean.TRUE.equals(active) ? "WORKING" : "RESIGNED";
    }

    @JsonProperty("maNhanVien")
    public String maNhanVien() {
        return employeeId;
    }

    @JsonProperty("maChiNhanh")
    public String maChiNhanh() {
        return branchId;
    }

    @JsonProperty("hoTen")
    public String hoTen() {
        return fullName;
    }

    @JsonProperty("ngaySinh")
    public LocalDate ngaySinh() {
        return birthDate;
    }

    @JsonProperty("gioiTinh")
    public String gioiTinh() {
        return gender;
    }

    @JsonProperty("soDienThoai")
    public String soDienThoai() {
        return phoneNumber;
    }

    @JsonProperty("chucVu")
    public String chucVu() {
        return position;
    }

    @JsonProperty("trangThai")
    public Boolean trangThai() {
        return active;
    }
}
