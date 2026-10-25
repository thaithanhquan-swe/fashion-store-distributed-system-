package com.fashionstore.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateEmployeeRequest(
        @Size(max = 10) @JsonAlias({"branchId", "maChiNhanh", "MaChiNhanh"}) String branchId,
        @NotBlank @Size(max = 100) @JsonAlias({"fullName", "employeeName", "hoTen", "HoTen"}) String fullName,
        @Past @JsonAlias({"birthDate", "ngaySinh", "NgaySinh"}) LocalDate birthDate,
        @Size(max = 10) @JsonAlias({"gender", "gioiTinh", "GioiTinh"}) String gender,
        @Size(max = 15) @JsonAlias({"phoneNumber", "phone", "soDienThoai", "SoDienThoai"}) String phoneNumber,
        @Email @Size(max = 100) @JsonAlias({"email", "Email"}) String email,
        @Size(max = 50) @JsonAlias({"position", "chucVu", "ChucVu"}) String position) {
}
