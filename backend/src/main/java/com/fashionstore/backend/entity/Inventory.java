package com.fashionstore.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "TonKho", schema = "dbo")
@IdClass(InventoryId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Inventory {

    @Id
    @Column(name = "MaChiNhanh", length = 10, nullable = false)
    String branchId;

    @Id
    @Column(name = "MaSanPham", length = 10, nullable = false)
    String productId;

    @Column(name = "SoLuongTon", nullable = false)
    @Builder.Default
    Integer quantity = 0;

    @Column(name = "MucTonToiThieu", nullable = false)
    @Builder.Default
    Integer minimumStock = 5;

    @Column(name = "NgayCapNhat", nullable = false, columnDefinition = "datetime")
    LocalDateTime updatedAt;
}
