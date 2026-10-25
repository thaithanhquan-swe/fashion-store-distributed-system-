package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.request.CreateEmployeeRequest;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.BranchRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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
        ensureAccount("admin", Role.ADMIN, "Local Admin", "Administrator", password);
        ensureAccount("manager", Role.MANAGER, "Local Manager", "Manager", password);
        ensureAccount("staff", Role.STAFF, "Local Staff", "Staff", password);
        ensureAccount("manager.q1", Role.MANAGER, "Local Manager Q1", "Manager", password);
        ensureAccount("staff.q1", Role.STAFF, "Local Staff Q1", "Staff", password);
    }

    private void ensureAccount(String username, Role role, String fullName, String position, String password) {
        Account account = accountRepository.findByUsername(username)
                .orElseGet(() -> createAccount(username, fullName, position));

        if (account.getPassword() == null || !passwordEncoder.matches(password, account.getPassword())) {
            account.setPassword(passwordEncoder.encode(password));
        }
        account.setRole(role);
        account.setActive(true);
        accountRepository.save(account);
    }

    private Account createAccount(String username, String fullName, String position) {
        String branchId = branchRepository.findFirstByActiveTrueOrderByBranchIdAsc()
                .map(Branch::getBranchId)
                .orElseGet(() -> branchService.create(
                        new CreateBranchRequest("Local Demo Branch", null, null)).branchId());

        String employeeId = employeeService.create(
                new CreateEmployeeRequest(branchId, fullName, null, null, null, null, position))
                .employeeId();

        return Account.builder()
                .employeeId(employeeId)
                .username(username)
                .active(true)
                .build();
    }
}
