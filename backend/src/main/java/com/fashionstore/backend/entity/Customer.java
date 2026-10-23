package com.fashionstore.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "KhachHang", schema = "dbo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Customer {

    @Id
    @Column(name = "MaKhachHang", length = 10, nullable = false)
    String customerId;

    @Column(name = "HoTen", length = 100, nullable = false)
    String fullName;

    @Column(name = "SoDienThoai", length = 15)
    String phoneNumber;

    @Column(name = "Email", length = 100)
    String email;

    @Column(name = "DiaChi", length = 255)
    String address;

    @Column(name = "DiemTichLuy", nullable = false)
    @Builder.Default
    Integer loyaltyPoints = 0;

    @Column(name = "NgayDangKy", nullable = false)
    LocalDate registeredDate;

    @Column(name = "TrangThai", nullable = false)
    @Builder.Default
    Boolean active = true;
}
