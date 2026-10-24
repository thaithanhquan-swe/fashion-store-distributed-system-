package com.fashionstore.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "ChiTietHoaDon", schema = "dbo")
@IdClass(InvoiceDetailId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDetail {

    @Id
    @Column(name = "MaHoaDon", length = 20, nullable = false)
    String invoiceId;

    @Id
    @Column(name = "MaSanPham", length = 10, nullable = false)
    String productId;

    @Column(name = "SoLuong", nullable = false)
    Integer quantity;

    @Column(name = "DonGia", precision = 18, scale = 2, nullable = false)
    BigDecimal unitPrice;

    @Column(name = "GiamGia", precision = 18, scale = 2, nullable = false)
    BigDecimal discount;

    @Column(name = "ThanhTien", precision = 18, scale = 2, nullable = false)
    BigDecimal lineTotal;
}
