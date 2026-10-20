package com.fashionstore.backend.entity;

import com.fashionstore.backend.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "TaiKhoan", schema = "dbo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTaiKhoan", nullable = false)
    Integer accountId;

    @Column(name = "MaNhanVien", length = 10, nullable = false, unique = true)
    String employeeId;

    @Column(name = "TenDangNhap", length = 50, nullable = false, unique = true)
    String username;

    @Column(name = "MatKhau", length = 255, nullable = false)
    @ToString.Exclude
    String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "VaiTro", length = 20, nullable = false)
    Role role;

    @Column(name = "TrangThai", nullable = false)
    @Builder.Default
    Boolean active = true;
}
