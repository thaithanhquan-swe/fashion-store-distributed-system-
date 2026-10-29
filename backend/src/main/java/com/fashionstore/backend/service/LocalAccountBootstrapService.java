package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.request.CreateEmployeeRequest;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.BranchRepository;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LocalAccountBootstrapService {

    AccountRepository accountRepository;
    BranchRepository branchRepository;
    BranchService branchService;
    EmployeeService employeeService;
    PasswordEncoder passwordEncoder;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void ensureAccounts(String password) {
        // Tự động tạo hoặc đồng bộ tài khoản mặc định cho từng Role trong enum Role
        for (Role role : Role.values()) {
            String username = role.name().toLowerCase(Locale.ROOT);
            String title = formatRoleTitle(role);
            ensureAccount(username, role, "Default " + title, title, password);
        }

        // Tạo thêm tài khoản demo cho chi nhánh Q1
        ensureAccount("manager.q1", Role.MANAGER, "Local Manager Q1", "Manager", password);
        ensureAccount("staff.q1", Role.STAFF, "Local Staff Q1", "Staff", password);
    }

    private String formatRoleTitle(Role role) {
        String name = role.name();
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    private void ensureAccount(String username, Role role, String fullName, String position, String password) {
        Account account = accountRepository.findByUsername(username)
                .orElseGet(() -> createAccount(username, fullName, position));

        boolean updated = false;
        if (account.getPassword() == null || !passwordEncoder.matches(password, account.getPassword())) {
            account.setPassword(passwordEncoder.encode(password));
            updated = true;
        }
        if (account.getRole() != role) {
            account.setRole(role);
            updated = true;
        }
        if (!Boolean.TRUE.equals(account.getActive())) {
            account.setActive(true);
            updated = true;
        }

        if (updated || account.getAccountId() == null) {
            accountRepository.save(account);
            log.info("Bootstrapped default account '{}' (role: {}, active: true)", username, role);
        }
    }

    private Account createAccount(String username, String fullName, String position) {
        String branchId = branchRepository.findFirstByActiveTrueOrderByBranchIdAsc()
                .map(Branch::getBranchId)
                .orElseGet(() -> {
                    log.info("No active branch found in database. Auto-creating default branch 'Local Demo Branch'...");
                    return branchService.create(
                            new CreateBranchRequest("Local Demo Branch", null, null)).branchId();
                });

        String employeeId = employeeService.create(
                new CreateEmployeeRequest(branchId, fullName, null, null, null, null, position))
                .employeeId();

        log.info("Created employee '{}' (id: {}) for account '{}' at branch '{}'", fullName, employeeId, username, branchId);

        return Account.builder()
                .employeeId(employeeId)
                .username(username)
                .active(true)
                .build();
    }
}
