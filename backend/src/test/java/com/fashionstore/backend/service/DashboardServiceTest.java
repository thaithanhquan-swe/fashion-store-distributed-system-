package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.response.dashboard.AdminDashboardResponse;
import com.fashionstore.backend.dto.response.dashboard.ManagerDashboardResponse;
import com.fashionstore.backend.dto.response.dashboard.StaffDashboardResponse;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.entity.Employee;
import com.fashionstore.backend.entity.Inventory;
import com.fashionstore.backend.entity.Invoice;
import com.fashionstore.backend.entity.InvoiceDetail;
import com.fashionstore.backend.entity.Product;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.BranchRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import com.fashionstore.backend.repository.InventoryRepository;
import com.fashionstore.backend.repository.InvoiceDetailRepository;
import com.fashionstore.backend.repository.InvoiceRepository;
import com.fashionstore.backend.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    BranchRepository branchRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    InvoiceRepository invoiceRepository;

    @Mock
    InvoiceDetailRepository invoiceDetailRepository;

    @Mock
    InventoryRepository inventoryRepository;

    @InjectMocks
    DashboardService dashboardService;

    Branch branchQ1;
    Branch branchQ7;
    Employee employee1;
    Employee employee2;
    Product product1;
    Product product2;
    Account adminAccount;
    Account managerAccount;
    Account staffAccount;

    @BeforeEach
    void setUp() {
        branchQ1 = Branch.builder().branchId("CN01").branchName("Chi nhánh Quận 1").active(true).build();
        branchQ7 = Branch.builder().branchId("CN02").branchName("Chi nhánh Quận 7").active(true).build();

        employee1 = Employee.builder()
                .employeeId("NV001").fullName("Nguyen Van A").branchId("CN01").active(true).build();
        employee2 = Employee.builder()
                .employeeId("NV002").fullName("Tran Thi B").branchId("CN01").active(true).build();

        product1 = Product.builder()
                .productId("SP001").productName("Ao Thun").price(BigDecimal.valueOf(200000)).active(true).build();
        product2 = Product.builder()
                .productId("SP002").productName("Quan Jean").price(BigDecimal.valueOf(500000)).active(true).build();

        adminAccount = Account.builder()
                .accountId(1).username("admin").role(Role.ADMIN).active(true).build();
        managerAccount = Account.builder()
                .accountId(2).username("manager.q1").employeeId("NV001").role(Role.MANAGER).active(true).build();
        staffAccount = Account.builder()
                .accountId(3).username("staff.q1").employeeId("NV002").role(Role.STAFF).active(true).build();
    }

    @Test
    void getAdminDashboard_success() {
        when(branchRepository.findAll()).thenReturn(List.of(branchQ1, branchQ7));
        when(productRepository.findAll()).thenReturn(List.of(product1, product2));
        when(employeeRepository.findAll()).thenReturn(List.of(employee1, employee2));

        Invoice inv1 = Invoice.builder()
                .invoiceId("HD001").branchId("CN01").customerId("KH001").employeeId("NV001")
                .createdAt(LocalDateTime.now().minusDays(1))
                .totalAmount(BigDecimal.valueOf(400000)).status("COMPLETED").build();
        Invoice inv2 = Invoice.builder()
                .invoiceId("HD002").branchId("CN02").customerId("KH002").employeeId("NV002")
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(600000)).status("COMPLETED").build();

        when(invoiceRepository.findAllByCreatedAtBetween(any(), any()))
                .thenReturn(List.of(inv1, inv2)) // current
                .thenReturn(Collections.emptyList()); // previous

        Inventory item1 = Inventory.builder()
                .branchId("CN01").productId("SP001").quantity(3).minimumStock(5).build();
        Inventory item2 = Inventory.builder()
                .branchId("CN02").productId("SP002").quantity(0).minimumStock(5).build();
        when(inventoryRepository.findAll()).thenReturn(List.of(item1, item2));

        InvoiceDetail detail1 = InvoiceDetail.builder()
                .invoiceId("HD001").productId("SP001").quantity(2).lineTotal(BigDecimal.valueOf(400000)).build();
        InvoiceDetail detail2 = InvoiceDetail.builder()
                .invoiceId("HD002").productId("SP002").quantity(1).lineTotal(BigDecimal.valueOf(500000)).build();
        when(invoiceDetailRepository.findAllByInvoiceIdIn(anySet())).thenReturn(List.of(detail1, detail2));

        AdminDashboardResponse response = dashboardService.getAdminDashboard(30);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(1000000), response.summary().revenue());
        assertEquals(2, response.summary().orders());
        assertEquals(2, response.summary().customers());
        assertEquals(2, response.inventory().totalSkus());
        assertEquals(1, response.inventory().lowStock());
        assertEquals(1, response.inventory().outOfStock());
        assertEquals(30, response.revenueSeries().size());
        assertEquals(2, response.branchRevenue().size());
        assertEquals(2, response.topProducts().size());
        assertEquals(2, response.lowStockItems().size());
        assertEquals(2, response.lowStockTotal());
        assertEquals(2, response.employeeSales().size());
    }

    @Test
    void getAdminDashboard_emptyInvoices_noDivisionByZero() {
        when(branchRepository.findAll()).thenReturn(List.of(branchQ1));
        when(productRepository.findAll()).thenReturn(List.of(product1));
        when(employeeRepository.findAll()).thenReturn(List.of(employee1));
        when(invoiceRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(Collections.emptyList());
        when(inventoryRepository.findAll()).thenReturn(Collections.emptyList());

        AdminDashboardResponse response = dashboardService.getAdminDashboard(0);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.summary().revenue());
        assertEquals(0, response.summary().orders());
        assertEquals(BigDecimal.ZERO, response.summary().averageOrderValue());
        assertEquals(BigDecimal.ZERO, response.summary().averageDailyRevenue());
    }

    @Test
    void getManagerDashboard_asManager_success() {
        when(accountRepository.findByUsername("manager.q1")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findById("NV001")).thenReturn(Optional.of(employee1));
        when(branchRepository.findAll()).thenReturn(List.of(branchQ1, branchQ7));
        when(productRepository.findAll()).thenReturn(List.of(product1));
        when(employeeRepository.findAll()).thenReturn(List.of(employee1));

        Invoice inv = Invoice.builder()
                .invoiceId("HD001").branchId("CN01").customerId("KH001").employeeId("NV001")
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(200000)).status("COMPLETED").build();
        when(invoiceRepository.findAllByBranchIdAndCreatedAtBetween(eq("CN01"), any(), any()))
                .thenReturn(List.of(inv))
                .thenReturn(Collections.emptyList());

        when(inventoryRepository.findAllByBranchId("CN01")).thenReturn(Collections.emptyList());

        ManagerDashboardResponse response = dashboardService.getManagerDashboard("manager.q1", "CN01", 30);

        assertNotNull(response);
        assertEquals("CN01", response.branchId());
        assertEquals("Chi nhánh Quận 1", response.branchName());
        assertEquals(BigDecimal.valueOf(200000), response.summary().revenue());
        assertEquals(1, response.summary().orders());
        // Verify aliases
        assertEquals(response.series(), response.revenueSeries());
        assertEquals(response.top(), response.topProducts());
        assertEquals(response.low(), response.lowStockItems());
        assertEquals(response.employees(), response.employeeSales());
    }

    @Test
    void getManagerDashboard_asManager_wrongBranch_throwsAccessDenied() {
        when(accountRepository.findByUsername("manager.q1")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findById("NV001")).thenReturn(Optional.of(employee1));

        AppException ex = assertThrows(AppException.class,
                () -> dashboardService.getManagerDashboard("manager.q1", "CN02", 30));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void getManagerDashboard_asAdmin_customBranch_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(branchRepository.findAll()).thenReturn(List.of(branchQ7));
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(employeeRepository.findAll()).thenReturn(Collections.emptyList());
        when(invoiceRepository.findAllByBranchIdAndCreatedAtBetween(eq("CN02"), any(), any()))
                .thenReturn(Collections.emptyList());
        when(inventoryRepository.findAllByBranchId("CN02")).thenReturn(Collections.emptyList());

        ManagerDashboardResponse response = dashboardService.getManagerDashboard("admin", "CN02", 30);

        assertNotNull(response);
        assertEquals("CN02", response.branchId());
    }

    @Test
    void getStaffDashboard_success() {
        when(accountRepository.findByUsername("staff.q1")).thenReturn(Optional.of(staffAccount));
        when(employeeRepository.findById("NV002")).thenReturn(Optional.of(employee2));
        when(branchRepository.findAll()).thenReturn(List.of(branchQ1));

        Invoice todayInv = Invoice.builder()
                .invoiceId("HD001").branchId("CN01").employeeId("NV002")
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(300000)).status("COMPLETED").build();

        when(invoiceRepository.findAllByEmployeeIdAndCreatedAtBetween(eq("NV002"), any(), any()))
                .thenReturn(List.of(todayInv)) // today
                .thenReturn(List.of(todayInv)); // month

        when(invoiceRepository.findTop5ByEmployeeIdOrderByCreatedAtDesc("NV002"))
                .thenReturn(List.of(todayInv));

        StaffDashboardResponse response = dashboardService.getStaffDashboard("staff.q1");

        assertNotNull(response);
        assertEquals("NV002", response.employeeId());
        assertEquals("Tran Thi B", response.employeeName());
        assertEquals("CN01", response.branchId());
        assertEquals(BigDecimal.valueOf(300000), response.todayRevenue());
        assertEquals(1, response.todayOrders());
        assertEquals(BigDecimal.valueOf(300000), response.monthRevenue());
        assertEquals(1, response.monthOrders());
        assertEquals(1, response.recentInvoices().size());
        assertEquals("HD001", response.recentInvoices().get(0).invoiceId());
    }

    @Test
    void getStaffDashboard_employeeNotFound_throwsAppException() {
        Account noEmpAccount = Account.builder().username("ghost").employeeId(null).active(true).build();
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.of(noEmpAccount));

        AppException ex = assertThrows(AppException.class,
                () -> dashboardService.getStaffDashboard("ghost"));
        assertEquals(ErrorCode.EMPLOYEE_NOT_FOUND, ex.getErrorCode());
    }
}
