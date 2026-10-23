package com.fashionstore.backend.specification;

import com.fashionstore.backend.entity.Product;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> filter(
            String keyword, String category, String brand, String color, String size,
            Boolean active, BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                filters.add(builder.or(
                        builder.like(builder.lower(root.get("productId")), pattern),
                        builder.like(builder.lower(root.get("productName")), pattern),
                        builder.like(builder.lower(root.get("category")), pattern),
                        builder.like(builder.lower(root.get("brand")), pattern),
                        builder.like(builder.lower(root.get("color")), pattern),
                        builder.like(builder.lower(root.get("size")), pattern)));
            }
            addTextFilter(filters, root.get("category"), category, builder);
            addTextFilter(filters, root.get("brand"), brand, builder);
            addTextFilter(filters, root.get("color"), color, builder);
            addTextFilter(filters, root.get("size"), size, builder);
            if (active != null) {
                filters.add(builder.equal(root.get("active"), active));
            }
            if (minPrice != null) {
                filters.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                filters.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return builder.and(filters.toArray(Predicate[]::new));
        };
    }

    private static void addTextFilter(
            List<Predicate> filters, jakarta.persistence.criteria.Path<String> path,
            String value, jakarta.persistence.criteria.CriteriaBuilder builder) {
        if (value != null) {
            filters.add(builder.equal(builder.lower(path), value.toLowerCase(Locale.ROOT)));
        }
    }
}
