package com.fashionstore.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "SanPham", schema = "dbo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    @Column(name = "MaSanPham", length = 10, nullable = false)
    String productId;

    @Column(name = "TenSanPham", length = 150, nullable = false)
    String productName;

    @Column(name = "LoaiSanPham", length = 50)
    String category;

    @Column(name = "ThuongHieu", length = 100)
    String brand;

    @Column(name = "MauSac", length = 30)
    String color;

    @Column(name = "KichThuoc", length = 10)
    String size;

    @Column(name = "GiaBan", precision = 18, scale = 2, nullable = false)
    BigDecimal price;

    @Column(name = "TrangThai", nullable = false)
    @Builder.Default
    Boolean active = true;
}
