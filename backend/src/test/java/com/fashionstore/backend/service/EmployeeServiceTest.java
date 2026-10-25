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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    BranchRepository branchRepository;

    @Mock
    AccountRepository accountRepository;

    EmployeeMapper employeeMapper = Mappers.getMapper(EmployeeMapper.class);

    EmployeeService employeeService;

    Account adminAccount;
    Account managerAccount;
    Employee managerEmployee;
    Employee staffEmployee;
    Branch branch1;
    Branch branch2;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(
                employeeRepository,
                branchRepository,
                accountRepository,
                employeeMapper);

        adminAccount = Account.builder()
                .accountId(1)
                .username("admin")
                .employeeId("NV001")
                .role(Role.ADMIN)
                .active(true)
                .build();

        managerAccount = Account.builder()
                .accountId(2)
                .username("manager")
                .employeeId("NV002")
                .role(Role.MANAGER)
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

        branch1 = Branch.builder()
                .branchId("CN01")
                .branchName("Chi Nhanh 1")
                .active(true)
                .build();

        branch2 = Branch.builder()
                .branchId("CN02")
                .branchName("Chi Nhanh 2")
                .active(true)
                .build();
    }

    @Test
    void list_asAdmin_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        Page<Employee> page = new PageImpl<>(List.of(managerEmployee, staffEmployee));
        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<EmployeeResponse> result = employeeService.list(
                "admin", null, null, null, null, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void list_asManager_sameBranch_success() {
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findById("NV002")).thenReturn(Optional.of(managerEmployee));
        Page<Employee> page = new PageImpl<>(List.of(managerEmployee, staffEmployee));
        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<EmployeeResponse> result = employeeService.list(
                "manager", null, "CN01", null, null, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void list_asManager_differentBranch_throwsAccessDenied() {
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findById("NV002")).thenReturn(Optional.of(managerEmployee));

        AppException ex = assertThrows(AppException.class, () ->
                employeeService.list("manager", null, "CN02", null, null, PageRequest.of(0, 20)));

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void getById_asAdmin_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(employeeRepository.findById("NV003")).thenReturn(Optional.of(staffEmployee));

        EmployeeResponse response = employeeService.getById("admin", "NV003");
        assertNotNull(response);
        assertEquals("NV003", response.employeeId());
        assertEquals("Staff User", response.fullName());
        assertEquals("Staff User", response.employeeName());
        assertEquals("WORKING", response.status());
    }

    @Test
    void getById_asManager_differentBranch_throwsAccessDenied() {
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(managerAccount));
        Employee otherBranchEmployee = Employee.builder()
                .employeeId("NV004")
                .branchId("CN02")
                .fullName("Other Branch Staff")
                .active(true)
                .build();
        when(employeeRepository.findById("NV004")).thenReturn(Optional.of(otherBranchEmployee));
        when(employeeRepository.findById("NV002")).thenReturn(Optional.of(managerEmployee));

        AppException ex = assertThrows(AppException.class, () ->
                employeeService.getById("manager", "NV004"));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void getById_notFound_throwsException() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(employeeRepository.findById("NV999")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                employeeService.getById("admin", "NV999"));
        assertEquals(ErrorCode.EMPLOYEE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_asAdmin_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(branchRepository.findById("CN01")).thenReturn(Optional.of(branch1));
        when(employeeRepository.findAllByEmployeeIdStartingWith("NV")).thenReturn(Collections.emptyList());
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "CN01", "New Staff", LocalDate.of(1995, 5, 10), "Nam", "0912345678", "new@store.com", "Sales");

        EmployeeResponse response = employeeService.create("admin", request);
        assertNotNull(response);
        assertEquals("NV001", response.employeeId());
        assertEquals("New Staff", response.fullName());
        assertTrue(response.active());
    }

    @Test
    void create_asManager_otherBranch_throwsAccessDenied() {
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findById("NV002")).thenReturn(Optional.of(managerEmployee));

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "CN02", "New Staff", null, null, null, null, null);

        AppException ex = assertThrows(AppException.class, () ->
                employeeService.create("manager", request));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void update_asAdmin_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(employeeRepository.findById("NV003")).thenReturn(Optional.of(staffEmployee));
        when(branchRepository.findById("CN02")).thenReturn(Optional.of(branch2));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "CN02", "Updated Staff", LocalDate.of(1992, 1, 1), "Nu", "0987654321", "updated@store.com", "Manager Assistant");

        EmployeeResponse response = employeeService.update("admin", "NV003", request);
        assertNotNull(response);
        assertEquals("CN02", response.branchId());
        assertEquals("Updated Staff", response.fullName());
        assertEquals("0987654321", response.phoneNumber());
    }

    @Test
    void update_asManager_changeBranch_throwsAccessDenied() {
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findById("NV003")).thenReturn(Optional.of(staffEmployee));
        when(employeeRepository.findById("NV002")).thenReturn(Optional.of(managerEmployee));

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "CN02", "Updated Staff", null, null, null, null, null);

        AppException ex = assertThrows(AppException.class, () ->
                employeeService.update("manager", "NV003", request));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void updateStatus_lockAndUnlock_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(employeeRepository.findById("NV003")).thenReturn(Optional.of(staffEmployee));
        Account staffAccount = Account.builder()
                .accountId(3)
                .username("staff.user")
                .employeeId("NV003")
                .active(true)
                .build();
        when(accountRepository.findByEmployeeId("NV003")).thenReturn(Optional.of(staffAccount));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Lock employee with active = false
        UpdateEmployeeStatusRequest lockRequest = new UpdateEmployeeStatusRequest(false, null);
        EmployeeResponse locked = employeeService.updateStatus("admin", "NV003", lockRequest);
        assertFalse(locked.active());
        assertEquals("RESIGNED", locked.status());
        assertFalse(staffAccount.getActive());

        // Unlock employee with status string "WORKING"
        UpdateEmployeeStatusRequest unlockRequest = new UpdateEmployeeStatusRequest(null, "WORKING");
        EmployeeResponse unlocked = employeeService.updateStatus("admin", "NV003", unlockRequest);
        assertTrue(unlocked.active());
        assertEquals("WORKING", unlocked.status());
        assertTrue(staffAccount.getActive());
    }

    @Test
    void updateStatus_selfDeactivation_throwsAccessDenied() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(employeeRepository.findById("NV001")).thenReturn(Optional.of(Employee.builder()
                .employeeId("NV001")
                .fullName("Admin User")
                .active(true)
                .build()));

        UpdateEmployeeStatusRequest lockRequest = new UpdateEmployeeStatusRequest(false, null);
        AppException ex = assertThrows(AppException.class, () ->
                employeeService.updateStatus("admin", "NV001", lockRequest));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }
}
