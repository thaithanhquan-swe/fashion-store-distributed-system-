package com.fashionstore.backend.repository;

import com.fashionstore.backend.entity.Product;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query(value = "SELECT COALESCE(MAX(TRY_CONVERT(INT, SUBSTRING(MaSanPham, 3, 8))), 0) "
            + "FROM dbo.SanPham WITH (TABLOCKX, HOLDLOCK) "
            + "WHERE MaSanPham LIKE 'SP%'", nativeQuery = true)
    Integer findHighestGeneratedProductNumber();

    @Query("""
            SELECT p FROM Product p
            WHERE (:keyword IS NULL OR
                LOWER(p.productId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(COALESCE(p.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(COALESCE(p.color, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(COALESCE(p.size, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
            AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
            AND (:color IS NULL OR LOWER(p.color) = LOWER(:color))
            AND (:size IS NULL OR LOWER(p.size) = LOWER(:size))
            AND (:active IS NULL OR p.active = :active)
            AND (:minPrice IS NULL OR p.price >= :minPrice)
            AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Product> search(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("color") String color,
            @Param("size") String size,
            @Param("active") Boolean active,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
