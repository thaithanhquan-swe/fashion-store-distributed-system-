package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.response.dashboard.AdminDashboardResponse;
import com.fashionstore.backend.dto.response.dashboard.BranchRevenueDto;
import com.fashionstore.backend.dto.response.dashboard.DailyRevenueDto;
import com.fashionstore.backend.dto.response.dashboard.EmployeeSaleDto;
import com.fashionstore.backend.dto.response.dashboard.InventorySummaryDto;
import com.fashionstore.backend.dto.response.dashboard.LowStockItemDto;
import com.fashionstore.backend.dto.response.dashboard.ManagerDashboardResponse;
import com.fashionstore.backend.dto.response.dashboard.RecentInvoiceDto;
import com.fashionstore.backend.dto.response.dashboard.RevenueSummaryDto;
import com.fashionstore.backend.dto.response.dashboard.StaffDashboardResponse;
import com.fashionstore.backend.dto.response.dashboard.TopProductDto;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardService {

    AccountRepository accountRepository;
    EmployeeRepository employeeRepository;
    BranchRepository branchRepository;
    ProductRepository productRepository;
    InvoiceRepository invoiceRepository;
    InvoiceDetailRepository invoiceDetailRepository;
    InventoryRepository inventoryRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard(Integer daysParam) {
        int days = normalizeDays(daysParam);
        TimeWindow window = TimeWindow.of(days);

        List<Invoice> currentInvoices = invoiceRepository.findAllByCreatedAtBetween(window.from, window.to);
        List<Invoice> previousInvoices = invoiceRepository.findAllByCreatedAtBetween(window.prevFrom, window.prevTo);

        Map<String, String> branchNames = loadBranchNameMap();
        Map<String, Product> productMap = loadProductMap();
        Map<String, Employee> employeeMap = loadEmployeeMap();
        List<Inventory> allInventory = inventoryRepository.findAll();

        RevenueSummaryDto summary = computeRevenueSummary(currentInvoices, previousInvoices, days);
        InventorySummaryDto inventorySummary = computeInventorySummary(allInventory, productMap);
        List<DailyRevenueDto> series = computeDailyRevenueSeries(currentInvoices, window.from.toLocalDate(), days);
        List<BranchRevenueDto> branchRevenue = computeBranchRevenue(currentInvoices, branchNames, summary.revenue());
        List<TopProductDto> topProducts = computeTopProducts(currentInvoices, productMap, 5);
        List<LowStockItemDto> lowStockItems = computeLowStockItems(allInventory, branchNames, productMap);
        long lowStockTotal = lowStockItems.size();
        List<LowStockItemDto> lowStockSubset = lowStockItems.stream().limit(8).toList();
        List<EmployeeSaleDto> employeeSales = computeEmployeeSales(currentInvoices, employeeMap, branchNames, 5);

        return new AdminDashboardResponse(
                summary,
                inventorySummary,
                series,
                branchRevenue,
                topProducts,
                lowStockSubset,
                lowStockTotal,
                employeeSales);
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(String username, String branchIdParam, Integer daysParam) {
        Account account = findAccount(username);
        String effectiveBranchId = resolveManagerBranchId(account, branchIdParam);
        int days = normalizeDays(daysParam);
        TimeWindow window = TimeWindow.of(days);

        List<Invoice> currentInvoices = invoiceRepository
                .findAllByBranchIdAndCreatedAtBetween(effectiveBranchId, window.from, window.to);
        List<Invoice> previousInvoices = invoiceRepository
                .findAllByBranchIdAndCreatedAtBetween(effectiveBranchId, window.prevFrom, window.prevTo);

        Map<String, String> branchNames = loadBranchNameMap();
        Map<String, Product> productMap = loadProductMap();
        Map<String, Employee> employeeMap = loadEmployeeMap();
        List<Inventory> branchInventory = inventoryRepository.findAllByBranchId(effectiveBranchId);

        String branchName = branchNames.getOrDefault(effectiveBranchId, effectiveBranchId);
        RevenueSummaryDto summary = computeRevenueSummary(currentInvoices, previousInvoices, days);
        InventorySummaryDto inventorySummary = computeInventorySummary(branchInventory, productMap);
        List<DailyRevenueDto> series = computeDailyRevenueSeries(currentInvoices, window.from.toLocalDate(), days);
        List<TopProductDto> topProducts = computeTopProducts(currentInvoices, productMap, 5);
        List<LowStockItemDto> lowStockItems = computeLowStockItems(branchInventory, branchNames, productMap);
        List<EmployeeSaleDto> employeeSales = computeEmployeeSales(currentInvoices, employeeMap, branchNames, 5);

        return new ManagerDashboardResponse(
                effectiveBranchId,
                branchName,
                summary,
                inventorySummary,
                series,
                topProducts,
                lowStockItems,
                employeeSales);
    }

    @Transactional(readOnly = true)
    public StaffDashboardResponse getStaffDashboard(String username) {
        Account account = findAccount(username);
        if (account.getEmployeeId() == null || account.getEmployeeId().isBlank()) {
            throw new AppException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }
        Employee employee = employeeRepository.findById(account.getEmployeeId())
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();

        List<Invoice> todayInvoices = invoiceRepository
                .findAllByEmployeeIdAndCreatedAtBetween(employee.getEmployeeId(), todayStart, now);
        List<Invoice> monthInvoices = invoiceRepository
                .findAllByEmployeeIdAndCreatedAtBetween(employee.getEmployeeId(), monthStart, now);

        BigDecimal todayRevenue = todayInvoices.stream()
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthRevenue = monthInvoices.stream()
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RecentInvoiceDto> recent = invoiceRepository
                .findTop5ByEmployeeIdOrderByCreatedAtDesc(employee.getEmployeeId())
                .stream()
                .map(inv -> new RecentInvoiceDto(
                        inv.getInvoiceId(),
                        inv.getCreatedAt(),
                        inv.getTotalAmount(),
                        inv.getCustomerId(),
                        inv.getPaymentMethod(),
                        inv.getStatus()))
                .toList();

        Map<String, String> branchNames = loadBranchNameMap();
        String branchName = branchNames.getOrDefault(employee.getBranchId(), employee.getBranchId());

        return new StaffDashboardResponse(
                employee.getEmployeeId(),
                employee.getFullName(),
                employee.getBranchId(),
                branchName,
                todayRevenue,
                todayInvoices.size(),
                monthRevenue,
                monthInvoices.size(),
                recent);
    }

    private int normalizeDays(Integer days) {
        if (days == null || days <= 0 || days > 365) {
            return 30;
        }
        return days;
    }

    // Chỉ chấp nhận 3 mã chi nhánh chuẩn: CN1, CN2, CN3
    private static final java.util.Set<String> VALID_BRANCH_CODES =
            java.util.Set.of("CN1", "CN2", "CN3");

    private String resolveManagerBranchId(Account account, String branchIdParam) {
        if (account.getRole() == Role.ADMIN) {
            if (branchIdParam != null && !branchIdParam.isBlank()) {
                String trimmed = branchIdParam.trim().toUpperCase(java.util.Locale.ROOT);
                if (!VALID_BRANCH_CODES.contains(trimmed)) {
                    throw new AppException(ErrorCode.BRANCH_INVALID_ID);
                }
                return trimmed;
            }
            return branchRepository.findFirstByActiveTrueOrderByBranchIdAsc()
                    .map(Branch::getBranchId)
                    .orElse("CN1");
        }

        if (account.getRole() == Role.MANAGER) {
            Employee manager = employeeRepository.findById(account.getEmployeeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
            String managerBranchId = manager.getBranchId();
            if (branchIdParam != null && !branchIdParam.isBlank()
                    && !branchIdParam.trim().equalsIgnoreCase(managerBranchId)) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            return managerBranchId;
        }

        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private Account findAccount(String username) {
        return accountRepository.findByUsername(username)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    private Map<String, String> loadBranchNameMap() {
        return branchRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Branch::getBranchId,
                        b -> b.getBranchName() != null ? b.getBranchName() : b.getBranchId(),
                        (existing, replacement) -> existing));
    }

    private Map<String, Product> loadProductMap() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));
    }

    private Map<String, Employee> loadEmployeeMap() {
        return employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e, (a, b) -> a));
    }

    private RevenueSummaryDto computeRevenueSummary(
            List<Invoice> current, List<Invoice> previous, int days) {
        BigDecimal revenue = current.stream()
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal prevRevenue = previous.stream()
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long orders = current.size();
        long prevOrders = previous.size();

        long customers = current.stream()
                .map(Invoice::getCustomerId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .count();

        long prevCustomers = previous.stream()
                .map(Invoice::getCustomerId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .count();

        BigDecimal averageOrderValue = orders > 0
                ? revenue.divide(BigDecimal.valueOf(orders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal averageDailyRevenue = days > 0
                ? revenue.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double revenueChange = percentChange(revenue.doubleValue(), prevRevenue.doubleValue());
        double ordersChange = percentChange(orders, prevOrders);
        double customersChange = percentChange(customers, prevCustomers);

        return new RevenueSummaryDto(
                revenue,
                orders,
                customers,
                averageOrderValue,
                averageDailyRevenue,
                prevRevenue,
                prevOrders,
                revenueChange,
                ordersChange,
                customersChange);
    }

    private InventorySummaryDto computeInventorySummary(
            List<Inventory> inventoryList, Map<String, Product> productMap) {
        long totalSkus = inventoryList.size();
        long totalQuantity = 0;
        long available = 0;
        long lowStock = 0;
        long outOfStock = 0;
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Inventory inv : inventoryList) {
            int qty = inv.getQuantity() != null ? inv.getQuantity() : 0;
            int min = inv.getMinimumStock() != null ? inv.getMinimumStock() : 5;
            totalQuantity += qty;

            if (qty <= 0) {
                outOfStock++;
            } else if (qty <= min) {
                lowStock++;
            } else {
                available++;
            }

            if (qty > 0) {
                Product product = productMap.get(inv.getProductId());
                if (product != null && product.getPrice() != null) {
                    totalValue = totalValue.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
                }
            }
        }

        return new InventorySummaryDto(
                totalSkus, totalQuantity, available, lowStock, outOfStock, totalValue);
    }

    private List<DailyRevenueDto> computeDailyRevenueSeries(
            List<Invoice> invoices, LocalDate startDate, int days) {
        Map<String, List<Invoice>> groupedByDate = invoices.stream()
                .filter(inv -> inv.getCreatedAt() != null)
                .collect(Collectors.groupingBy(inv -> inv.getCreatedAt().toLocalDate().format(DATE_FORMATTER)));

        List<DailyRevenueDto> series = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateKey = date.format(DATE_FORMATTER);
            String label = date.format(LABEL_FORMATTER);

            List<Invoice> dayInvoices = groupedByDate.getOrDefault(dateKey, Collections.emptyList());
            BigDecimal dayRevenue = dayInvoices.stream()
                    .map(Invoice::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            series.add(new DailyRevenueDto(dateKey, label, dayRevenue, dayInvoices.size()));
        }
        return series;
    }

    private List<BranchRevenueDto> computeBranchRevenue(
            List<Invoice> invoices, Map<String, String> branchNames, BigDecimal totalRevenue) {
        Map<String, List<Invoice>> byBranch = invoices.stream()
                .filter(inv -> inv.getBranchId() != null)
                .collect(Collectors.groupingBy(Invoice::getBranchId));

        List<BranchRevenueDto> list = new ArrayList<>();
        double totalRev = totalRevenue != null ? totalRevenue.doubleValue() : 0.0;

        for (Map.Entry<String, String> entry : branchNames.entrySet()) {
            String bId = entry.getKey();
            String bName = entry.getValue();
            List<Invoice> branchInvoices = byBranch.getOrDefault(bId, Collections.emptyList());

            BigDecimal bRevenue = branchInvoices.stream()
                    .map(Invoice::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long count = branchInvoices.size();
            double share = totalRev > 0
                    ? Math.round((bRevenue.doubleValue() / totalRev) * 1000.0) / 10.0
                    : 0.0;

            BigDecimal avgOrder = count > 0
                    ? bRevenue.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            list.add(new BranchRevenueDto(bId, bName, bRevenue, count, share, avgOrder));
        }

        list.sort(Comparator.comparing(BranchRevenueDto::revenue).reversed());
        return list;
    }

    private List<TopProductDto> computeTopProducts(
            List<Invoice> invoices, Map<String, Product> productMap, int limit) {
        Set<String> invoiceIds = invoices.stream()
                .map(Invoice::getInvoiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (invoiceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<InvoiceDetail> details = invoiceDetailRepository.findAllByInvoiceIdIn(invoiceIds);

        Map<String, ProductAgg> aggMap = new HashMap<>();
        for (InvoiceDetail detail : details) {
            String pId = detail.getProductId();
            ProductAgg agg = aggMap.computeIfAbsent(pId, id -> new ProductAgg(id));
            agg.quantity += (detail.getQuantity() != null ? detail.getQuantity() : 0);
            if (detail.getLineTotal() != null) {
                agg.revenue = agg.revenue.add(detail.getLineTotal());
            }
        }

        List<ProductAgg> sorted = new ArrayList<>(aggMap.values());
        sorted.sort(Comparator.comparing((ProductAgg a) -> a.revenue).reversed());

        List<TopProductDto> result = new ArrayList<>();
        int rank = 1;
        for (ProductAgg agg : sorted) {
            if (result.size() >= limit) {
                break;
            }
            Product p = productMap.get(agg.productId);
            String pName = p != null && p.getProductName() != null ? p.getProductName() : agg.productId;
            result.add(new TopProductDto(agg.productId, pName, agg.quantity, agg.revenue, rank++));
        }
        return result;
    }

    private List<LowStockItemDto> computeLowStockItems(
            List<Inventory> inventoryList, Map<String, String> branchNames, Map<String, Product> productMap) {
        List<LowStockItemDto> items = new ArrayList<>();
        for (Inventory inv : inventoryList) {
            int qty = inv.getQuantity() != null ? inv.getQuantity() : 0;
            int min = inv.getMinimumStock() != null ? inv.getMinimumStock() : 5;
            if (qty <= min) {
                String bName = branchNames.getOrDefault(inv.getBranchId(), inv.getBranchId());
                Product p = productMap.get(inv.getProductId());
                String pName = p != null && p.getProductName() != null ? p.getProductName() : inv.getProductId();
                String status = qty <= 0 ? "OUT" : "LOW";
                items.add(new LowStockItemDto(
                        inv.getBranchId(),
                        bName,
                        inv.getProductId(),
                        pName,
                        qty,
                        min,
                        status,
                        inv.getUpdatedAt()));
            }
        }

        items.sort((a, b) -> Integer.compare(b.minQuantity() - b.quantity(), a.minQuantity() - a.quantity()));
        return items;
    }

    private List<EmployeeSaleDto> computeEmployeeSales(
            List<Invoice> invoices,
            Map<String, Employee> employeeMap,
            Map<String, String> branchNames,
            int limit) {
        Map<String, List<Invoice>> byEmployee = invoices.stream()
                .filter(inv -> inv.getEmployeeId() != null)
                .collect(Collectors.groupingBy(Invoice::getEmployeeId));

        List<EmployeeSaleDto> list = new ArrayList<>();
        for (Map.Entry<String, List<Invoice>> entry : byEmployee.entrySet()) {
            String empId = entry.getKey();
            List<Invoice> empInvoices = entry.getValue();

            BigDecimal rev = empInvoices.stream()
                    .map(Invoice::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Employee emp = employeeMap.get(empId);
            String empName = emp != null && emp.getFullName() != null ? emp.getFullName() : empId;
            String branchId = emp != null && emp.getBranchId() != null ? emp.getBranchId() : "";
            String branchName = branchNames.getOrDefault(branchId, branchId);

            list.add(new EmployeeSaleDto(empId, empName, branchId, branchName, empInvoices.size(), rev));
        }

        list.sort(Comparator.comparing(EmployeeSaleDto::revenue).reversed());
        return list.stream().limit(limit).toList();
    }

    private double percentChange(double current, double previous) {
        if (previous <= 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        double change = ((current - previous) / previous) * 100.0;
        return Math.round(change * 10.0) / 10.0;
    }

    private static class ProductAgg {
        final String productId;
        long quantity = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        ProductAgg(String productId) {
            this.productId = productId;
        }
    }

    private record TimeWindow(
            LocalDateTime from,
            LocalDateTime to,
            LocalDateTime prevFrom,
            LocalDateTime prevTo) {

        static TimeWindow of(int days) {
            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.toLocalDate().minusDays(days - 1).atStartOfDay();
            LocalDateTime prevTo = from;
            LocalDateTime prevFrom = from.toLocalDate().minusDays(days).atStartOfDay();
            return new TimeWindow(from, to, prevFrom, prevTo);
        }
    }
}
