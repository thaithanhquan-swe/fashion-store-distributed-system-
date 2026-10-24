package com.fashionstore.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "HoaDon", schema = "dbo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Invoice {

    @Id
    @Column(name = "MaHoaDon", length = 20, nullable = false)
    String invoiceId;

    @Column(name = "MaChiNhanh", length = 10, nullable = false)
    String branchId;

    @Column(name = "MaKhachHang", length = 10)
    String customerId;

    @Column(name = "MaNhanVien", length = 10, nullable = false)
    String employeeId;

    @Column(name = "NgayLap", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "TongTien", precision = 18, scale = 2, nullable = false)
    BigDecimal totalAmount;

    @Column(name = "PhuongThucTT", length = 30)
    String paymentMethod;

    @Column(name = "TrangThai", length = 20, nullable = false)
    String status;
}
