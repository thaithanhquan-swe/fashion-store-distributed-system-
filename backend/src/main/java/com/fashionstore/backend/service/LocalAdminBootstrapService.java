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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LocalAdminBootstrapService {

    AccountRepository accountRepository;
    BranchRepository branchRepository;
    BranchService branchService;
    EmployeeService employeeService;
    PasswordEncoder passwordEncoder;

    @Transactional
    public void ensureAdmin(String username, String password) {
        if (accountRepository.existsByUsername(username)) {
            return;
        }

        String branchId = branchRepository.findFirstByActiveTrueOrderByBranchIdAsc()
                .map(Branch::getBranchId)
                .orElseGet(() -> branchService.create(
                        new CreateBranchRequest("Local Admin Branch", null, null)).branchId());

        String employeeId = employeeService.create(
                new CreateEmployeeRequest(branchId, "Local Admin", null, null, null, null, "Administrator"))
                .employeeId();

        accountRepository.save(Account.builder()
                .employeeId(employeeId)
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build());
    }
}
