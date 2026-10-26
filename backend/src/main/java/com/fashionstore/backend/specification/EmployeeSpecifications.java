package com.fashionstore.backend.specification;

import com.fashionstore.backend.entity.Employee;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> filter(
            String keyword, String branchId, String position, Boolean active) {
        return (root, query, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            if (active != null) {
                filters.add(builder.equal(root.get("active"), active));
            }
            if (branchId != null && !branchId.isBlank()) {
                filters.add(builder.equal(root.get("branchId"), branchId.trim()));
            }
            if (position != null && !position.isBlank()) {
                filters.add(builder.like(
                        builder.lower(root.get("position")),
                        "%" + position.toLowerCase(Locale.ROOT).trim() + "%"));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT).trim() + "%";
                filters.add(builder.or(
                        builder.like(builder.lower(root.get("employeeId")), pattern),
                        builder.like(builder.lower(root.get("fullName")), pattern),
                        builder.like(builder.lower(root.get("phoneNumber")), pattern),
                        builder.like(builder.lower(root.get("email")), pattern),
                        builder.like(builder.lower(root.get("position")), pattern)));
            }
            return builder.and(filters.toArray(Predicate[]::new));
        };
    }
}
