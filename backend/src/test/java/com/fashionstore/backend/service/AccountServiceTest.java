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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    AccountService accountService;

    Account adminAccount;
    Account managerAccount;
    Account staffAccount;
    Employee adminEmployee;
    Employee managerEmployee;
    Employee staffEmployee;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, employeeRepository, passwordEncoder);

        adminAccount = Account.builder()
                .accountId(1)
                .username("admin")
                .employeeId("NV001")
                .password("encoded_pass")
                .role(Role.ADMIN)
                .active(true)
                .build();

        managerAccount = Account.builder()
                .accountId(2)
                .username("manager")
                .employeeId("NV002")
                .password("encoded_pass")
                .role(Role.MANAGER)
                .active(true)
                .build();

        staffAccount = Account.builder()
                .accountId(3)
                .username("staff.q1")
                .employeeId("NV003")
                .password("encoded_pass")
                .role(Role.STAFF)
                .active(true)
                .build();

        adminEmployee = Employee.builder()
                .employeeId("NV001")
                .branchId("CN01")
                .fullName("Admin User")
                .active(true)
                .build();

        managerEmployee = Employee.builder()
                .employeeId("NV002")
                .branchId("CN01")
                .fullName("Manager User")
                .active(true)
                .build();

        staffEmployee = Employee.builder()
                .employeeId("NV003")
                .branchId("CN01")
                .fullName("Staff User")
                .active(true)
                .build();
    }

    @Test
    void list_asAdmin_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        Page<Account> page = new PageImpl<>(List.of(adminAccount, managerAccount, staffAccount));
        when(accountRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(employeeRepository.findAllById(anyCollection())).thenReturn(List.of(adminEmployee, managerEmployee, staffEmployee));

        PageResponse<AccountResponse> result = accountService.list(
                "admin", null, null, null, null, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Admin User", result.getContent().get(0).employeeName());
    }

    @Test
    void list_asManager_differentBranch_throwsAccessDenied() {
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findById("NV002")).thenReturn(Optional.of(managerEmployee));

        AppException ex = assertThrows(AppException.class, () ->
                accountService.list("manager", null, "CN02", null, null, PageRequest.of(0, 20)));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void getById_numericIdAndUsername_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(accountRepository.findById(3)).thenReturn(Optional.of(staffAccount));
        when(employeeRepository.findById("NV003")).thenReturn(Optional.of(staffEmployee));

        AccountResponse byId = accountService.getById("admin", "3");
        assertNotNull(byId);
        assertEquals("staff.q1", byId.username());
        assertEquals("Staff User", byId.employeeName());
        assertEquals("ACTIVE", byId.status());

        when(accountRepository.findByUsername("staff.q1")).thenReturn(Optional.of(staffAccount));
        AccountResponse byUsername = accountService.getById("admin", "staff.q1");
        assertNotNull(byUsername);
        assertEquals("staff.q1", byUsername.username());
    }

    @Test
    void getById_notFound_throwsException() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(accountRepository.findById(99)).thenReturn(Optional.empty());
        when(accountRepository.findByUsername("99")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                accountService.getById("admin", "99"));
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_asAdmin_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(accountRepository.existsByUsername("new.user")).thenReturn(false);
        when(accountRepository.existsByEmployeeId("NV004")).thenReturn(false);
        Employee newEmp = Employee.builder()
                .employeeId("NV004")
                .branchId("CN02")
                .fullName("New Employee")
                .active(true)
                .build();
        when(employeeRepository.findById("NV004")).thenReturn(Optional.of(newEmp));
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAccountRequest request = new CreateAccountRequest("NV004", "new.user", "secret123", Role.STAFF);
        AccountResponse response = accountService.create("admin", request);

        assertNotNull(response);
        assertEquals("new.user", response.username());
        assertEquals("NV004", response.employeeId());
        assertEquals("New Employee", response.employeeName());
    }

    @Test
    void create_asManager_tryingToCreateManagerRole_throwsAccessDenied() {
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(managerAccount));

        CreateAccountRequest request = new CreateAccountRequest("NV004", "new.manager", "secret123", Role.MANAGER);
        AppException ex = assertThrows(AppException.class, () ->
                accountService.create("manager", request));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void updateRole_asAdmin_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(accountRepository.findById(3)).thenReturn(Optional.of(staffAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.findById("NV003")).thenReturn(Optional.of(staffEmployee));

        UpdateAccountRoleRequest request = new UpdateAccountRoleRequest(Role.MANAGER);
        AccountResponse response = accountService.updateRole("admin", "3", request);

        assertNotNull(response);
        assertEquals(Role.MANAGER, response.role());
    }

    @Test
    void updateRole_changeOwnRole_throwsAccessDenied() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(accountRepository.findById(1)).thenReturn(Optional.of(adminAccount));

        UpdateAccountRoleRequest request = new UpdateAccountRoleRequest(Role.STAFF);
        AppException ex = assertThrows(AppException.class, () ->
                accountService.updateRole("admin", "1", request));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void updateStatus_lockAndUnlock_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(accountRepository.findById(3)).thenReturn(Optional.of(staffAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.findById("NV003")).thenReturn(Optional.of(staffEmployee));

        UpdateAccountStatusRequest lockReq = new UpdateAccountStatusRequest(false, null);
        AccountResponse locked = accountService.updateStatus("admin", "3", lockReq);
        assertFalse(locked.active());
        assertEquals("LOCKED", locked.status());

        UpdateAccountStatusRequest unlockReq = new UpdateAccountStatusRequest(null, "ACTIVE");
        AccountResponse unlocked = accountService.updateStatus("admin", "3", unlockReq);
        assertTrue(unlocked.active());
        assertEquals("ACTIVE", unlocked.status());
    }

    @Test
    void updateStatus_managerLockAdmin_throwsAccessDenied() {
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(managerAccount));
        when(accountRepository.findById(1)).thenReturn(Optional.of(adminAccount));

        UpdateAccountStatusRequest lockReq = new UpdateAccountStatusRequest(false, null);
        AppException ex = assertThrows(AppException.class, () ->
                accountService.updateStatus("manager", "1", lockReq));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void resetPassword_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(accountRepository.findById(3)).thenReturn(Optional.of(staffAccount));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_reset");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResetPasswordResponse response = accountService.resetPassword("admin", "3", null);
        assertNotNull(response);
        assertNotNull(response.tempPassword());
        assertTrue(response.tempPassword().startsWith("FS"));
        assertEquals("staff.q1", response.username());
    }
}
