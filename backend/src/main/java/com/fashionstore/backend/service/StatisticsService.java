package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.response.dashboard.BranchRevenueDto;
import com.fashionstore.backend.dto.response.dashboard.DailyRevenueDto;
import com.fashionstore.backend.dto.response.dashboard.RevenueSummaryDto;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
public class StatisticsService {

    AccountRepository accountRepository;
    EmployeeRepository employeeRepository;
    BranchRepository branchRepository;
    ProductRepository productRepository;
    InvoiceRepository invoiceRepository;
    InvoiceDetailRepository invoiceDetailRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

    @Transactional(readOnly = true)
    public RevenueStatisticsResponse getRevenueStatistics(
            String username, String branchIdParam, Integer daysParam, LocalDate fromDate, LocalDate toDate) {
        Account account = findAccount(username);
        String effectiveBranchId = resolveBranchFilter(account, branchIdParam);
        TimeWindow window = resolveTimeWindow(daysParam, fromDate, toDate);

        List<Invoice> currentInvoices;
        List<Invoice> previousInvoices;

        if (effectiveBranchId != null) {
            currentInvoices = invoiceRepository
                    .findAllByBranchIdAndCreatedAtBetween(effectiveBranchId, window.from, window.to);
            previousInvoices = invoiceRepository
                    .findAllByBranchIdAndCreatedAtBetween(effectiveBranchId, window.prevFrom, window.prevTo);
        } else {
            currentInvoices = invoiceRepository.findAllByCreatedAtBetween(window.from, window.to);
            previousInvoices = invoiceRepository.findAllByCreatedAtBetween(window.prevFrom, window.prevTo);
        }

        RevenueSummaryDto summary = computeRevenueSummary(currentInvoices, previousInvoices, window.days);
        List<DailyRevenueDto> series = computeDailyRevenueSeries(currentInvoices, window.from.toLocalDate(), window.days);

        String branchName;
        if (effectiveBranchId != null) {
            branchName = branchRepository.findById(effectiveBranchId)
                    .map(Branch::getBranchName)
                    .orElse(effectiveBranchId);
        } else {
            branchName = "Toàn hệ thống";
        }

        return new RevenueStatisticsResponse(effectiveBranchId, branchName, summary, series);
    }

    @Transactional(readOnly = true)
    public List<BranchRevenueDto> getBranchRevenueStatistics(
            Integer daysParam, LocalDate fromDate, LocalDate toDate) {
        TimeWindow window = resolveTimeWindow(daysParam, fromDate, toDate);
        List<Invoice> invoices = invoiceRepository.findAllByCreatedAtBetween(window.from, window.to);
        Map<String, String> branchNames = loadBranchNameMap();

        BigDecimal totalRevenue = invoices.stream()
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<Invoice>> byBranch = invoices.stream()
                .filter(inv -> inv.getBranchId() != null)
                .collect(Collectors.groupingBy(Invoice::getBranchId));

        List<BranchRevenueDto> list = new ArrayList<>();
        double totalRev = totalRevenue.doubleValue();

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

    @Transactional(readOnly = true)
    public RevenueStatisticsResponse getBranchRevenueDetail(
            String username, String branchId, Integer daysParam, LocalDate fromDate, LocalDate toDate) {
        if (branchId == null || branchId.isBlank()) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }
        Branch branch = branchRepository.findById(branchId.trim())
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));

        return getRevenueStatistics(username, branch.getBranchId(), daysParam, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public List<TopProductStatisticDto> getTopProducts(
            String username, String branchIdParam, Integer daysParam, Integer limitParam, String sortByParam) {
        Account account = findAccount(username);
        String effectiveBranchId = resolveBranchFilter(account, branchIdParam);
        int days = normalizeDays(daysParam, 30);
        int limit = normalizeLimit(limitParam, 10, 100);
        TimeWindow window = TimeWindow.of(days);

        List<Invoice> invoices;
        if (effectiveBranchId != null) {
            invoices = invoiceRepository
                    .findAllByBranchIdAndCreatedAtBetween(effectiveBranchId, window.from, window.to);
        } else {
            invoices = invoiceRepository.findAllByCreatedAtBetween(window.from, window.to);
        }

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

        Map<String, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));

        List<ProductAgg> list = new ArrayList<>(aggMap.values());
        boolean sortByQuantity = "quantity".equalsIgnoreCase(sortByParam);
        if (sortByQuantity) {
            list.sort(Comparator.comparing((ProductAgg a) -> a.quantity).reversed());
        } else {
            list.sort(Comparator.comparing((ProductAgg a) -> a.revenue).reversed());
        }

        List<TopProductStatisticDto> result = new ArrayList<>();
        int rank = 1;
        for (ProductAgg agg : list) {
            if (result.size() >= limit) {
                break;
            }
            Product p = productMap.get(agg.productId);
            String pName = p != null && p.getProductName() != null ? p.getProductName() : agg.productId;
            String cat = p != null && p.getCategory() != null ? p.getCategory() : "";
            String brand = p != null && p.getBrand() != null ? p.getBrand() : "";

            result.add(new TopProductStatisticDto(
                    rank++, agg.productId, pName, cat, brand, agg.quantity, agg.revenue));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<EmployeeStatisticDto> getEmployeeSales(
            String username, String branchIdParam, Integer daysParam, Integer limitParam) {
        Account account = findAccount(username);
        String effectiveBranchId = resolveBranchFilter(account, branchIdParam);
        int days = normalizeDays(daysParam, 30);
        int limit = normalizeLimit(limitParam, 50, 100);
        TimeWindow window = TimeWindow.of(days);

        List<Invoice> invoices;
        if (effectiveBranchId != null) {
            invoices = invoiceRepository
                    .findAllByBranchIdAndCreatedAtBetween(effectiveBranchId, window.from, window.to);
        } else {
            invoices = invoiceRepository.findAllByCreatedAtBetween(window.from, window.to);
        }

        Map<String, List<Invoice>> byEmployee = invoices.stream()
                .filter(inv -> inv.getEmployeeId() != null)
                .collect(Collectors.groupingBy(Invoice::getEmployeeId));

        Map<String, Employee> employeeMap = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e, (a, b) -> a));
        Map<String, String> branchNames = loadBranchNameMap();

        List<EmployeeStatisticDto> list = new ArrayList<>();
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
            String position = emp != null && emp.getPosition() != null ? emp.getPosition() : "";

            list.add(new EmployeeStatisticDto(
                    0, empId, empName, branchId, branchName, position, empInvoices.size(), rev));
        }

        list.sort(Comparator.comparing(EmployeeStatisticDto::revenue).reversed());

        List<EmployeeStatisticDto> rankedList = new ArrayList<>();
        int rank = 1;
        for (EmployeeStatisticDto dto : list) {
            if (rankedList.size() >= limit) {
                break;
            }
            rankedList.add(new EmployeeStatisticDto(
                    rank++,
                    dto.employeeId(),
                    dto.employeeName(),
                    dto.branchId(),
                    dto.branchName(),
                    dto.position(),
                    dto.orders(),
                    dto.revenue()));
        }

        return rankedList;
    }

    private String resolveBranchFilter(Account account, String branchIdParam) {
        if (account.getRole() == Role.ADMIN) {
            return (branchIdParam != null && !branchIdParam.isBlank()) ? branchIdParam.trim() : null;
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

        if (account.getRole() == Role.STAFF) {
            Employee staff = employeeRepository.findById(account.getEmployeeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
            return staff.getBranchId();
        }

        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private Account findAccount(String username) {
        return accountRepository.findByUsername(username)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    private TimeWindow resolveTimeWindow(Integer daysParam, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            if (toDate.isBefore(fromDate)) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            LocalDateTime from = fromDate.atStartOfDay();
            LocalDateTime to = toDate.atTime(23, 59, 59);
            int days = (int) ChronoUnit.DAYS.between(fromDate, toDate) + 1;
            LocalDateTime prevTo = from.minusNanos(1);
            LocalDateTime prevFrom = from.minusDays(days);
            return new TimeWindow(from, to, prevFrom, prevTo, days);
        }

        int days = normalizeDays(daysParam, 30);
        return TimeWindow.of(days);
    }

    private int normalizeDays(Integer days, int defaultValue) {
        if (days == null || days <= 0 || days > 365) {
            return defaultValue;
        }
        return days;
    }

    private int normalizeLimit(Integer limit, int defaultValue, int maxValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, maxValue);
    }

    private Map<String, String> loadBranchNameMap() {
        return branchRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Branch::getBranchId,
                        b -> b.getBranchName() != null ? b.getBranchName() : b.getBranchId(),
                        (existing, replacement) -> existing));
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
            LocalDateTime prevTo,
            int days) {

        static TimeWindow of(int days) {
            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.toLocalDate().minusDays(days - 1).atStartOfDay();
            LocalDateTime prevTo = from;
            LocalDateTime prevFrom = from.toLocalDate().minusDays(days).atStartOfDay();
            return new TimeWindow(from, to, prevFrom, prevTo, days);
        }
    }
}
