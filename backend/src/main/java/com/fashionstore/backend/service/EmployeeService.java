package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateEmployeeRequest;
import com.fashionstore.backend.dto.request.UpdateEmployeeRequest;
import com.fashionstore.backend.dto.request.UpdateEmployeeStatusRequest;
import com.fashionstore.backend.dto.response.EmployeeResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.entity.Employee;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.EmployeeMapper;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.BranchRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import com.fashionstore.backend.specification.EmployeeSpecifications;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeService {

    EmployeeRepository employeeRepository;
    BranchRepository branchRepository;
    AccountRepository accountRepository;
    EmployeeMapper employeeMapper;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(
            String username,
            String keyword,
            String branchId,
            String position,
            Boolean active,
            Pageable pageable) {
        Account account = findAccount(username);
        String effectiveBranchId;
        if (account.getRole() == Role.ADMIN) {
            effectiveBranchId = normalize(branchId);
        } else if (account.getRole() == Role.MANAGER) {
            String managerBranchId = currentBranchId(account);
            if (branchId != null && !branchId.isBlank() && !branchId.trim().equals(managerBranchId)) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            effectiveBranchId = managerBranchId;
        } else {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Specification<Employee> spec = EmployeeSpecifications.filter(
                normalize(keyword), effectiveBranchId, normalize(position), active);
        Page<Employee> page = employeeRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(employeeMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(String username, String employeeId) {
        Account account = findAccount(username);
        Employee employee = findEmployee(employeeId);
        ensureCanAccess(account, employee);
        return employeeMapper.toResponse(employee);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EmployeeResponse create(CreateEmployeeRequest request) {
        return create(null, request);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EmployeeResponse create(String username, CreateEmployeeRequest request) {
        if (username != null) {
            Account account = findAccount(username);
            if (account.getRole() == Role.MANAGER) {
                String managerBranchId = currentBranchId(account);
                if (!managerBranchId.equals(request.branchId().trim())) {
                    throw new AppException(ErrorCode.ACCESS_DENIED);
                }
            } else if (account.getRole() != Role.ADMIN) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        }

        String branchId = request.branchId().trim();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
        if (Boolean.FALSE.equals(branch.getActive())) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }

        String employeeId = GeneratedId.next("NV", employeeRepository.findAllByEmployeeIdStartingWith("NV")
                .stream().map(Employee::getEmployeeId).toList(), ErrorCode.EMPLOYEE_ID_CAPACITY_EXCEEDED);

        Employee employee = employeeMapper.toEntity(request, employeeId);
        employee.setBranchId(branchId);
        employee.setFullName(request.fullName().trim());
        employee.setPhoneNumber(normalize(request.phoneNumber()));
        employee.setEmail(normalize(request.email()));
        employee.setPosition(normalize(request.position()));
        employee.setGender(normalize(request.gender()));
        employee.setActive(true);

        Employee savedEmployee = employeeRepository.save(employee);
        return employeeMapper.toResponse(savedEmployee);
    }

    @Transactional
    public EmployeeResponse update(String username, String employeeId, UpdateEmployeeRequest request) {
        Account account = findAccount(username);
        Employee employee = findEmployee(employeeId);
        ensureCanAccess(account, employee);

        String newBranchId = normalize(request.branchId());
        if (account.getRole() == Role.MANAGER) {
            String managerBranchId = currentBranchId(account);
            if (newBranchId != null && !newBranchId.equals(managerBranchId)) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        } else if (account.getRole() == Role.ADMIN) {
            if (newBranchId != null && !newBranchId.equals(employee.getBranchId())) {
                Branch branch = branchRepository.findById(newBranchId)
                        .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
                if (Boolean.FALSE.equals(branch.getActive())) {
                    throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
                }
                employee.setBranchId(newBranchId);
            }
        }

        employeeMapper.updateEntity(request, employee);
        employee.setFullName(request.fullName().trim());
        if (newBranchId != null) {
            employee.setBranchId(newBranchId);
        }
        employee.setBirthDate(request.birthDate());
        employee.setGender(normalize(request.gender()));
        employee.setPhoneNumber(normalize(request.phoneNumber()));
        employee.setEmail(normalize(request.email()));
        employee.setPosition(normalize(request.position()));

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Transactional
    public EmployeeResponse updateStatus(String username, String employeeId, UpdateEmployeeStatusRequest request) {
        Account account = findAccount(username);
        Employee employee = findEmployee(employeeId);
        ensureCanAccess(account, employee);

        final boolean targetActive = resolveActive(request);

        // Không cho phép tự khóa tài khoản của chính mình
        if (account.getEmployeeId() != null && account.getEmployeeId().equals(employeeId) && !targetActive) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        employee.setActive(targetActive);
        Employee saved = employeeRepository.save(employee);

        // Đồng bộ trạng thái tài khoản liên kết (nếu có)
        accountRepository.findByEmployeeId(employeeId).ifPresent(linkedAccount -> {
            linkedAccount.setActive(targetActive);
            accountRepository.save(linkedAccount);
        });

        return employeeMapper.toResponse(saved);
    }

    private boolean resolveActive(UpdateEmployeeStatusRequest request) {
        if (request.active() != null) {
            return request.active();
        }
        if (request.status() != null && !request.status().isBlank()) {
            String s = request.status().trim().toUpperCase(Locale.ROOT);
            if ("WORKING".equals(s) || "ACTIVE".equals(s) || "TRUE".equals(s)) {
                return true;
            }
            if ("RESIGNED".equals(s) || "INACTIVE".equals(s) || "LOCKED".equals(s) || "FALSE".equals(s)) {
                return false;
            }
        }
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    private Account findAccount(String username) {
        return accountRepository.findByUsername(username)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    private Employee findEmployee(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private String currentBranchId(Account account) {
        Employee employee = employeeRepository.findById(account.getEmployeeId())
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
        return employee.getBranchId();
    }

    private void ensureCanAccess(Account account, Employee employee) {
        if (account.getRole() == Role.ADMIN) {
            return;
        }
        if (account.getRole() == Role.MANAGER) {
            String managerBranchId = currentBranchId(account);
            if (!managerBranchId.equals(employee.getBranchId())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            return;
        }
        if (account.getEmployeeId() != null && account.getEmployeeId().equals(employee.getEmployeeId())) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
