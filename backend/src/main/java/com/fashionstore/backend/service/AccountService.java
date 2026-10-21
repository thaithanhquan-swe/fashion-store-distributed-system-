package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateAccountRequest;
import com.fashionstore.backend.dto.request.RegisterRequest;
import com.fashionstore.backend.dto.response.AccountResponse;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService {

    AccountRepository accountRepository;
    EmployeeRepository employeeRepository;
    PasswordEncoder passwordEncoder;

    public AccountResponse register(RegisterRequest request) {
        return create(new CreateAccountRequest(
                request.employeeId(),
                request.username(),
                request.password(),
                Role.STAFF));
    }

    public AccountResponse create(CreateAccountRequest request) {
        if (accountRepository.existsByUsername(request.username())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (accountRepository.existsByEmployeeId(request.employeeId())) {
            throw new AppException(ErrorCode.EMPLOYEE_ACCOUNT_ALREADY_EXISTS);
        }

        if (!employeeRepository.existsById(request.employeeId())) {
            throw new AppException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }

        Account account = Account.builder()
                .employeeId(request.employeeId())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        Account savedAccount = accountRepository.save(account);
        return new AccountResponse(
                savedAccount.getAccountId(),
                savedAccount.getEmployeeId(),
                savedAccount.getUsername(),
                savedAccount.getRole(),
                savedAccount.getActive());
    }
}
