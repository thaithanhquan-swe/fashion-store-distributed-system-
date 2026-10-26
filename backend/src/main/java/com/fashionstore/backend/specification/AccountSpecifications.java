package com.fashionstore.backend.specification;

import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.enums.Role;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class AccountSpecifications {

    private AccountSpecifications() {
    }

    public static Specification<Account> filter(
            String keyword,
            Collection<String> matchingEmployeeIds,
            Collection<String> branchEmployeeIds,
            Role role,
            Boolean active) {
        return (root, query, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            if (active != null) {
                filters.add(builder.equal(root.get("active"), active));
            }
            if (role != null) {
                filters.add(builder.equal(root.get("role"), role));
            }
            if (branchEmployeeIds != null) {
                if (branchEmployeeIds.isEmpty()) {
                    return builder.disjunction();
                }
                filters.add(root.get("employeeId").in(branchEmployeeIds));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT).trim() + "%";
                Predicate matchUsername = builder.like(builder.lower(root.get("username")), pattern);
                Predicate matchEmployeeId = builder.like(builder.lower(root.get("employeeId")), pattern);
                if (matchingEmployeeIds != null && !matchingEmployeeIds.isEmpty()) {
                    Predicate matchEmpName = root.get("employeeId").in(matchingEmployeeIds);
                    filters.add(builder.or(matchUsername, matchEmployeeId, matchEmpName));
                } else {
                    filters.add(builder.or(matchUsername, matchEmployeeId));
                }
            }
            return builder.and(filters.toArray(Predicate[]::new));
        };
    }
}
