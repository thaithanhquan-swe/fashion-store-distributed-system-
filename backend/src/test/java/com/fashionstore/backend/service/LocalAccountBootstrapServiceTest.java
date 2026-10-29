package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.request.CreateEmployeeRequest;
import com.fashionstore.backend.dto.response.BranchResponse;
import com.fashionstore.backend.dto.response.EmployeeResponse;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.BranchRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalAccountBootstrapServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    BranchRepository branchRepository;

    @Mock
    BranchService branchService;

    @Mock
    EmployeeService employeeService;

    @Mock
    PasswordEncoder passwordEncoder;

    LocalAccountBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        bootstrapService = new LocalAccountBootstrapService(
                accountRepository,
                branchRepository,
                branchService,
                employeeService,
                passwordEncoder
        );
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
    }

    @Test
    @DisplayName("Should create accounts for each role in enum when accounts do not exist")
    void shouldCreateAccountsWhenNotExist() {
        Branch branch = Branch.builder().branchId("CN01").branchName("Store Q1").active(true).build();
        when(branchRepository.findFirstByActiveTrueOrderByBranchIdAsc()).thenReturn(Optional.of(branch));
        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(employeeService.create(any(CreateEmployeeRequest.class)))
                .thenReturn(new EmployeeResponse("NV001", "CN01", "Name", null, null, null, null, "Staff", true));

        bootstrapService.ensureAccounts("123456");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, atLeast(Role.values().length)).save(accountCaptor.capture());

        for (Role role : Role.values()) {
            String expectedUsername = role.name().toLowerCase();
            boolean found = accountCaptor.getAllValues().stream()
                    .anyMatch(acc -> acc.getUsername().equals(expectedUsername)
                            && acc.getRole() == role
                            && Boolean.TRUE.equals(acc.getActive())
                            && "encoded_123456".equals(acc.getPassword()));
            assertTrue(found, "Account for role " + role + " should be created with role and encoded password");
        }
    }

    @Test
    @DisplayName("Should create default branch if no active branch exists in database")
    void shouldCreateDefaultBranchIfNoneExists() {
        when(branchRepository.findFirstByActiveTrueOrderByBranchIdAsc()).thenReturn(Optional.empty());
        when(branchService.create(any(CreateBranchRequest.class)))
                .thenReturn(new BranchResponse("CN001", "Local Demo Branch", null, null, true));
        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(employeeService.create(any(CreateEmployeeRequest.class)))
                .thenReturn(new EmployeeResponse("NV001", "CN001", "Name", null, null, null, null, "Staff", true));

        bootstrapService.ensureAccounts("123456");

        verify(branchService, atLeastOnce()).create(any(CreateBranchRequest.class));
        verify(accountRepository, atLeast(Role.values().length)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should update password and role if account already exists")
    void shouldUpdateExistingAccounts() {
        Account existingAdmin = Account.builder()
                .accountId(1)
                .username("admin")
                .password("old_encoded_pwd")
                .role(Role.STAFF) // will be updated to ADMIN
                .active(false)
                .build();

        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));
        when(accountRepository.findByUsername(argThat(u -> !"admin".equals(u)))).thenReturn(Optional.empty());

        Branch branch = Branch.builder().branchId("CN01").branchName("Store Q1").active(true).build();
        when(branchRepository.findFirstByActiveTrueOrderByBranchIdAsc()).thenReturn(Optional.of(branch));
        when(employeeService.create(any(CreateEmployeeRequest.class)))
                .thenReturn(new EmployeeResponse("NV001", "CN01", "Name", null, null, null, null, "Role", true));

        bootstrapService.ensureAccounts("123456");

        assertEquals("encoded_123456", existingAdmin.getPassword());
        assertEquals(Role.ADMIN, existingAdmin.getRole());
        assertTrue(existingAdmin.getActive());
        verify(accountRepository).save(existingAdmin);
    }
}
