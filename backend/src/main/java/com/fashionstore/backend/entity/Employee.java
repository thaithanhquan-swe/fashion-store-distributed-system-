package com.fashionstore.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Entity
@Table(name = "NhanVien", schema = "dbo")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Employee {

    @Id
    @Column(name = "MaNhanVien", length = 10, nullable = false)
    String employeeId;

    @Column(name = "MaChiNhanh", length = 10, nullable = false)
    String branchId;

    @Column(name = "HoTen", length = 100, nullable = false)
    String fullName;

    @Column(name = "NgaySinh")
    LocalDate birthDate;

    @Column(name = "GioiTinh", length = 10)
    String gender;

    @Column(name = "SoDienThoai", length = 15)
    String phoneNumber;

    @Column(name = "Email", length = 100)
    String email;

    @Column(name = "ChucVu", length = 50)
    String position;

    @Column(name = "TrangThai", nullable = false)
    @Builder.Default
    Boolean active = true;
}
