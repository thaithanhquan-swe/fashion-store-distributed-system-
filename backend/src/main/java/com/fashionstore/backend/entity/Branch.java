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

@Entity
@Table(name = "ChiNhanh", schema = "dbo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Branch {

    @Id
    @Column(name = "MaChiNhanh", length = 10, nullable = false)
    String branchId;

    @Column(name = "TenChiNhanh", length = 100, nullable = false)
    String branchName;

    @Column(name = "DiaChi", length = 255)
    String address;

    @Column(name = "SoDienThoai", length = 15)
    String phoneNumber;

    @Column(name = "TrangThai", nullable = false)
    @Builder.Default
    Boolean active = true;
}
