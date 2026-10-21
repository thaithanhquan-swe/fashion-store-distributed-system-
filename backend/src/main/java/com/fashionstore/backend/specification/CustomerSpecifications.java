package com.fashionstore.backend.specification;

import com.fashionstore.backend.entity.Customer;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> filter(String keyword, String phone, Boolean active) {
        return (root, query, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            if (active != null) {
                filters.add(builder.equal(root.get("active"), active));
            }
            if (phone != null) {
                filters.add(builder.like(root.get("phoneNumber"), "%" + phone + "%"));
            }
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                filters.add(builder.or(
                        builder.like(builder.lower(root.get("customerId")), pattern),
                        builder.like(builder.lower(root.get("fullName")), pattern),
                        builder.like(builder.lower(root.get("phoneNumber")), pattern),
                        builder.like(builder.lower(root.get("email")), pattern),
                        builder.like(builder.lower(root.get("address")), pattern)));
            }
            return builder.and(filters.toArray(Predicate[]::new));
        };
    }
}
