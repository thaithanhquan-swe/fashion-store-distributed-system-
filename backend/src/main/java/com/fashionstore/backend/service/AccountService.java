package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateAccountRequest;
import com.fashionstore.backend.dto.request.ResetPasswordRequest;
import com.fashionstore.backend.dto.request.UpdateAccountRoleRequest;
import com.fashionstore.backend.dto.request.UpdateAccountStatusRequest;
import com.fashionstore.backend.dto.response.AccountResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.dto.response.ResetPasswordResponse;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Employee;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import com.fashionstore.backend.specification.AccountSpecifications;
import com.fashionstore.backend.specification.EmployeeSpecifications;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService {

    AccountRepository accountRepository;
    EmployeeRepository employeeRepository;
    PasswordEncoder passwordEncoder;
    SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> list(
            String callerUsername,
            String keyword,
            String branchId,
            Role role,
            Boolean active,
            Pageable pageable) {
        Account caller = findAccount(callerUsername);
        String effectiveBranchId;
        if (caller.getRole() == Role.ADMIN) {
            effectiveBranchId = normalize(branchId);
        } else if (caller.getRole() == Role.MANAGER) {
            String managerBranchId = currentBranchId(caller);
            if (branchId != null && !branchId.isBlank() && !branchId.trim().equals(managerBranchId)) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            effectiveBranchId = managerBranchId;
        } else {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        List<String> branchEmployeeIds = null;
        if (effectiveBranchId != null) {
            List<Employee> branchEmployees = employeeRepository.findAllByBranchId(effectiveBranchId);
            branchEmployeeIds = branchEmployees.stream().map(Employee::getEmployeeId).toList();
        }

        List<String> matchingEmployeeIds = null;
        if (keyword != null && !keyword.isBlank()) {
            List<Employee> matchingEmployees = employeeRepository.findAll(
                    EmployeeSpecifications.filter(keyword, effectiveBranchId, null, null));
            matchingEmployeeIds = matchingEmployees.stream().map(Employee::getEmployeeId).toList();
        }

        Specification<Account> spec = AccountSpecifications.filter(
                normalize(keyword), matchingEmployeeIds, branchEmployeeIds, role, active);
        Page<Account> page = accountRepository.findAll(spec, pageable);

        List<String> empIds = page.getContent().stream().map(Account::getEmployeeId).toList();
        Map<String, Employee> empMap = employeeRepository.findAllById(empIds).stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e, (a, b) -> a));

        Page<AccountResponse> mappedPage = page.map(acc -> {
            Employee emp = empMap.get(acc.getEmployeeId());
            return new AccountResponse(
                    acc.getAccountId(),
                    acc.getEmployeeId(),
                    acc.getUsername(),
                    acc.getRole(),
                    acc.getActive(),
                    emp != null ? emp.getFullName() : null,
                    emp != null ? emp.getBranchId() : null);
        });

        return PageResponse.of(mappedPage);
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(String callerUsername, String identifier) {
        Account caller = findAccount(callerUsername);
        Account target = findAccountByIdOrUsername(identifier);
        Employee employee = employeeRepository.findById(target.getEmployeeId()).orElse(null);

        if (caller.getRole() == Role.ADMIN) {
            return toResponse(target, employee);
        }
        if (caller.getRole() == Role.MANAGER) {
            String managerBranchId = currentBranchId(caller);
            if (employee == null || !managerBranchId.equals(employee.getBranchId())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            return toResponse(target, employee);
        }
        if (caller.getAccountId().equals(target.getAccountId())) {
            return toResponse(target, employee);
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        return create(null, request);
    }

    @Transactional
    public AccountResponse create(String callerUsername, CreateAccountRequest request) {
        if (callerUsername != null) {
            Account caller = findAccount(callerUsername);
            if (caller.getRole() == Role.MANAGER) {
                if (request.role() != Role.STAFF) {
                    throw new AppException(ErrorCode.ACCESS_DENIED);
                }
                Employee employee = employeeRepository.findById(request.employeeId().trim())
                        .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));
                if (!currentBranchId(caller).equals(employee.getBranchId())) {
                    throw new AppException(ErrorCode.ACCESS_DENIED);
                }
            } else if (caller.getRole() != Role.ADMIN) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        }

        if (accountRepository.existsByUsername(request.username().trim())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (accountRepository.existsByEmployeeId(request.employeeId().trim())) {
            throw new AppException(ErrorCode.EMPLOYEE_ACCOUNT_ALREADY_EXISTS);
        }

        Employee employee = employeeRepository.findById(request.employeeId().trim())
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));
        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new AppException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }

        Account account = Account.builder()
                .employeeId(employee.getEmployeeId())
                .username(request.username().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build();

        Account savedAccount = accountRepository.save(account);
        return toResponse(savedAccount, employee);
    }

    @Transactional
    public AccountResponse updateRole(String callerUsername, String identifier, UpdateAccountRoleRequest request) {
        Account caller = findAccount(callerUsername);
        if (caller.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Account target = findAccountByIdOrUsername(identifier);
        if (caller.getAccountId().equals(target.getAccountId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        target.setRole(request.role());
        Account saved = accountRepository.save(target);
        Employee employee = employeeRepository.findById(saved.getEmployeeId()).orElse(null);
        return toResponse(saved, employee);
    }

    @Transactional
    public AccountResponse updateStatus(String callerUsername, String identifier, UpdateAccountStatusRequest request) {
        Account caller = findAccount(callerUsername);
        Account target = findAccountByIdOrUsername(identifier);

        final boolean targetActive = resolveActive(request);

        if (caller.getAccountId().equals(target.getAccountId()) && !targetActive) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (target.getRole() == Role.ADMIN && caller.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (caller.getRole() == Role.MANAGER) {
            if (target.getRole() != Role.STAFF) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            Employee employee = employeeRepository.findById(target.getEmployeeId())
                    .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));
            if (!currentBranchId(caller).equals(employee.getBranchId())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        } else if (caller.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        target.setActive(targetActive);
        Account saved = accountRepository.save(target);
        Employee employee = employeeRepository.findById(saved.getEmployeeId()).orElse(null);
        return toResponse(saved, employee);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(
            String callerUsername, String identifier, ResetPasswordRequest request) {
        Account caller = findAccount(callerUsername);
        Account target = findAccountByIdOrUsername(identifier);

        if (target.getRole() == Role.ADMIN && caller.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (caller.getRole() == Role.MANAGER) {
            if (target.getRole() != Role.STAFF) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            Employee employee = employeeRepository.findById(target.getEmployeeId())
                    .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));
            if (!currentBranchId(caller).equals(employee.getBranchId())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        } else if (caller.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        String plainPassword;
        if (request != null && request.newPassword() != null && !request.newPassword().isBlank()) {
            plainPassword = request.newPassword().trim();
        } else {
            int code = 100000 + random.nextInt(900000);
            plainPassword = "FS" + code;
        }

        target.setPassword(passwordEncoder.encode(plainPassword));
        accountRepository.save(target);

        return new ResetPasswordResponse(
                target.getAccountId(),
                target.getUsername(),
                plainPassword,
                "Đặt lại mật khẩu thành công");
    }

    private boolean resolveActive(UpdateAccountStatusRequest request) {
        if (request.active() != null) {
            return request.active();
        }
        if (request.status() != null && !request.status().isBlank()) {
            String s = request.status().trim().toUpperCase(Locale.ROOT);
            if ("ACTIVE".equals(s) || "WORKING".equals(s) || "TRUE".equals(s)) {
                return true;
            }
            if ("LOCKED".equals(s) || "INACTIVE".equals(s) || "FALSE".equals(s)) {
                return false;
            }
        }
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    private Account findAccountByIdOrUsername(String identifier) {
        try {
            int id = Integer.parseInt(identifier);
            return accountRepository.findById(id)
                    .orElseGet(() -> accountRepository.findByUsername(identifier)
                            .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
        } catch (NumberFormatException e) {
            return accountRepository.findByUsername(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        }
    }

    private Account findAccount(String username) {
        return accountRepository.findByUsername(username)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    private String currentBranchId(Account caller) {
        Employee employee = employeeRepository.findById(caller.getEmployeeId())
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
        return employee.getBranchId();
    }

    private AccountResponse toResponse(Account account, Employee employee) {
        return new AccountResponse(
                account.getAccountId(),
                account.getEmployeeId(),
                account.getUsername(),
                account.getRole(),
                account.getActive(),
                employee != null ? employee.getFullName() : null,
                employee != null ? employee.getBranchId() : null);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
