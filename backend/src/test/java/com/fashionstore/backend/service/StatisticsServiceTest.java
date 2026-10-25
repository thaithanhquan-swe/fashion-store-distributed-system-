package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.response.dashboard.BranchRevenueDto;
import com.fashionstore.backend.dto.response.statistics.EmployeeStatisticDto;
import com.fashionstore.backend.dto.response.statistics.RevenueStatisticsResponse;
import com.fashionstore.backend.dto.response.statistics.TopProductStatisticDto;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.entity.Employee;
import com.fashionstore.backend.entity.Invoice;
import com.fashionstore.backend.entity.InvoiceDetail;
import com.fashionstore.backend.entity.Product;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.BranchRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import com.fashionstore.backend.repository.InvoiceDetailRepository;
import com.fashionstore.backend.repository.InvoiceRepository;
import com.fashionstore.backend.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class StatisticsServiceTest {

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

    @InjectMocks
    StatisticsService statisticsService;

    Branch branchQ1;
    Branch branchQ7;
    Employee employee1;
    Employee employee2;
    Product product1;
    Product product2;
    Account adminAccount;
    Account managerAccount;

    @BeforeEach
    void setUp() {
        branchQ1 = Branch.builder().branchId("CN01").branchName("Chi nhánh Quận 1").active(true).build();
        branchQ7 = Branch.builder().branchId("CN02").branchName("Chi nhánh Quận 7").active(true).build();

        employee1 = Employee.builder()
                .employeeId("NV001").fullName("Nguyen Van A").branchId("CN01").position("Manager").active(true).build();
        employee2 = Employee.builder()
                .employeeId("NV002").fullName("Tran Thi B").branchId("CN02").position("Sales").active(true).build();

        product1 = Product.builder()
                .productId("SP001").productName("Ao Thun").category("Ao").brand("BrandA").price(BigDecimal.valueOf(200000)).active(true).build();
        product2 = Product.builder()
                .productId("SP002").productName("Quan Jean").category("Quan").brand("BrandB").price(BigDecimal.valueOf(500000)).active(true).build();

        adminAccount = Account.builder()
                .accountId(1).username("admin").role(Role.ADMIN).active(true).build();
        managerAccount = Account.builder()
                .accountId(2).username("manager.q1").employeeId("NV001").role(Role.MANAGER).active(true).build();
    }

    @Test
    void getRevenueStatistics_admin_allBranches() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));

        Invoice inv = Invoice.builder()
                .invoiceId("HD001").branchId("CN01").customerId("KH001").employeeId("NV001")
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(1000000)).status("COMPLETED").build();

        when(invoiceRepository.findAllByCreatedAtBetween(any(), any()))
                .thenReturn(List.of(inv))
                .thenReturn(Collections.emptyList());

        RevenueStatisticsResponse response =
                statisticsService.getRevenueStatistics("admin", null, 30, null, null);

        assertNotNull(response);
        assertNull(response.branchId());
        assertEquals("Toàn hệ thống", response.branchName());
        assertEquals(BigDecimal.valueOf(1000000), response.summary().revenue());
        assertEquals(1, response.summary().orders());
        assertEquals(30, response.series().size());
    }

    @Test
    void getRevenueStatistics_manager_ownBranch() {
        when(accountRepository.findByUsername("manager.q1")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findById("NV001")).thenReturn(Optional.of(employee1));
        when(branchRepository.findById("CN01")).thenReturn(Optional.of(branchQ1));

        Invoice inv = Invoice.builder()
                .invoiceId("HD001").branchId("CN01").customerId("KH001").employeeId("NV001")
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(500000)).status("COMPLETED").build();

        when(invoiceRepository.findAllByBranchIdAndCreatedAtBetween(eq("CN01"), any(), any()))
                .thenReturn(List.of(inv))
                .thenReturn(Collections.emptyList());

        RevenueStatisticsResponse response =
                statisticsService.getRevenueStatistics("manager.q1", "CN01", 30, null, null);

        assertNotNull(response);
        assertEquals("CN01", response.branchId());
        assertEquals("Chi nhánh Quận 1", response.branchName());
        assertEquals(BigDecimal.valueOf(500000), response.summary().revenue());
    }

    @Test
    void getRevenueStatistics_manager_wrongBranch_throwsAccessDenied() {
        when(accountRepository.findByUsername("manager.q1")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findById("NV001")).thenReturn(Optional.of(employee1));

        AppException ex = assertThrows(AppException.class,
                () -> statisticsService.getRevenueStatistics("manager.q1", "CN02", 30, null, null));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void getRevenueStatistics_withCustomDates_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(invoiceRepository.findAllByCreatedAtBetween(any(), any()))
                .thenReturn(Collections.emptyList());

        LocalDate from = LocalDate.now().minusDays(5);
        LocalDate to = LocalDate.now();

        RevenueStatisticsResponse response =
                statisticsService.getRevenueStatistics("admin", null, null, from, to);

        assertNotNull(response);
        assertEquals(6, response.series().size());
    }

    @Test
    void getRevenueStatistics_invalidDateRange_throwsInvalidRequest() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));

        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().minusDays(5);

        AppException ex = assertThrows(AppException.class,
                () -> statisticsService.getRevenueStatistics("admin", null, null, from, to));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void getBranchRevenueStatistics_success() {
        when(branchRepository.findAll()).thenReturn(List.of(branchQ1, branchQ7));

        Invoice inv1 = Invoice.builder()
                .invoiceId("HD001").branchId("CN01").createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(600000)).build();
        Invoice inv2 = Invoice.builder()
                .invoiceId("HD002").branchId("CN02").createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(400000)).build();

        when(invoiceRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(List.of(inv1, inv2));

        List<BranchRevenueDto> list = statisticsService.getBranchRevenueStatistics(30, null, null);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("CN01", list.get(0).branchId());
        assertEquals(BigDecimal.valueOf(600000), list.get(0).revenue());
        assertEquals(60.0, list.get(0).share());
        assertEquals("CN02", list.get(1).branchId());
        assertEquals(BigDecimal.valueOf(400000), list.get(1).revenue());
        assertEquals(40.0, list.get(1).share());
    }

    @Test
    void getBranchRevenueDetail_success() {
        when(branchRepository.findById("CN01")).thenReturn(Optional.of(branchQ1));
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(invoiceRepository.findAllByBranchIdAndCreatedAtBetween(eq("CN01"), any(), any()))
                .thenReturn(Collections.emptyList());

        RevenueStatisticsResponse response =
                statisticsService.getBranchRevenueDetail("admin", "CN01", 30, null, null);

        assertNotNull(response);
        assertEquals("CN01", response.branchId());
    }

    @Test
    void getBranchRevenueDetail_branchNotFound_throwsAppException() {
        when(branchRepository.findById("CN99")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> statisticsService.getBranchRevenueDetail("admin", "CN99", 30, null, null));
        assertEquals(ErrorCode.BRANCH_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getTopProducts_sortByRevenue_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));

        Invoice inv = Invoice.builder().invoiceId("HD001").createdAt(LocalDateTime.now()).build();
        when(invoiceRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(List.of(inv));

        InvoiceDetail d1 = InvoiceDetail.builder()
                .invoiceId("HD001").productId("SP001").quantity(5).lineTotal(BigDecimal.valueOf(1000000)).build();
        InvoiceDetail d2 = InvoiceDetail.builder()
                .invoiceId("HD001").productId("SP002").quantity(1).lineTotal(BigDecimal.valueOf(500000)).build();
        when(invoiceDetailRepository.findAllByInvoiceIdIn(anySet())).thenReturn(List.of(d1, d2));
        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        List<TopProductStatisticDto> list =
                statisticsService.getTopProducts("admin", null, 30, 10, "revenue");

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).rank());
        assertEquals("SP001", list.get(0).productId());
        assertEquals(2, list.get(1).rank());
        assertEquals("SP002", list.get(1).productId());
    }

    @Test
    void getTopProducts_sortByQuantity_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));

        Invoice inv = Invoice.builder().invoiceId("HD001").createdAt(LocalDateTime.now()).build();
        when(invoiceRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(List.of(inv));

        InvoiceDetail d1 = InvoiceDetail.builder()
                .invoiceId("HD001").productId("SP001").quantity(2).lineTotal(BigDecimal.valueOf(1000000)).build();
        InvoiceDetail d2 = InvoiceDetail.builder()
                .invoiceId("HD001").productId("SP002").quantity(10).lineTotal(BigDecimal.valueOf(500000)).build();
        when(invoiceDetailRepository.findAllByInvoiceIdIn(anySet())).thenReturn(List.of(d1, d2));
        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        List<TopProductStatisticDto> list =
                statisticsService.getTopProducts("admin", null, 30, 10, "quantity");

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("SP002", list.get(0).productId()); // higher quantity first
    }

    @Test
    void getEmployeeSales_success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));

        Invoice inv1 = Invoice.builder()
                .invoiceId("HD001").employeeId("NV001").createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(800000)).build();
        Invoice inv2 = Invoice.builder()
                .invoiceId("HD002").employeeId("NV002").createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(400000)).build();

        when(invoiceRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(List.of(inv1, inv2));
        when(employeeRepository.findAll()).thenReturn(List.of(employee1, employee2));
        when(branchRepository.findAll()).thenReturn(List.of(branchQ1, branchQ7));

        List<EmployeeStatisticDto> list =
                statisticsService.getEmployeeSales("admin", null, 30, 50);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).rank());
        assertEquals("NV001", list.get(0).employeeId());
        assertEquals(BigDecimal.valueOf(800000), list.get(0).revenue());
        assertEquals(2, list.get(1).rank());
        assertEquals("NV002", list.get(1).employeeId());
    }
}
