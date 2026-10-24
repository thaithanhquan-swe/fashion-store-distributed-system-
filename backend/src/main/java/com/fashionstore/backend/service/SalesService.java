package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CheckStockRequest;
import com.fashionstore.backend.dto.request.CheckoutRequest;
import com.fashionstore.backend.dto.request.SaleItemRequest;
import com.fashionstore.backend.dto.response.CheckoutItemResponse;
import com.fashionstore.backend.dto.response.CheckoutResponse;
import com.fashionstore.backend.dto.response.StockCheckItemResponse;
import com.fashionstore.backend.dto.response.StockCheckResponse;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.entity.Customer;
import com.fashionstore.backend.entity.Employee;
import com.fashionstore.backend.entity.Inventory;
import com.fashionstore.backend.entity.InventoryId;
import com.fashionstore.backend.entity.Invoice;
import com.fashionstore.backend.entity.InvoiceDetail;
import com.fashionstore.backend.entity.Product;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.BranchRepository;
import com.fashionstore.backend.repository.CustomerRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import com.fashionstore.backend.repository.InventoryRepository;
import com.fashionstore.backend.repository.InvoiceDetailRepository;
import com.fashionstore.backend.repository.InvoiceRepository;
import com.fashionstore.backend.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SalesService {

    AccountRepository accountRepository;
    EmployeeRepository employeeRepository;
    BranchRepository branchRepository;
    CustomerRepository customerRepository;
    ProductRepository productRepository;
    InventoryRepository inventoryRepository;
    InvoiceRepository invoiceRepository;
    InvoiceDetailRepository invoiceDetailRepository;

    @Transactional(readOnly = true)
    public StockCheckResponse checkStock(String username, CheckStockRequest request) {
        Branch branch = currentBranch(username);
        Map<String, Integer> quantities = aggregate(request.items());
        List<StockCheckItemResponse> items = new ArrayList<>();
        boolean allAvailable = true;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            requireActiveProduct(entry.getKey());
            int available = inventoryRepository.findById(new InventoryId(branch.getBranchId(), entry.getKey()))
                    .map(Inventory::getQuantity)
                    .orElse(0);
            boolean sufficient = available >= entry.getValue();
            allAvailable &= sufficient;
            items.add(new StockCheckItemResponse(entry.getKey(), entry.getValue(), available, sufficient));
        }
        return new StockCheckResponse(allAvailable, items);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CheckoutResponse checkout(String username, CheckoutRequest request) {
        Branch branch = currentBranch(username);
        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new AppException(ErrorCode.SALE_BRANCH_INACTIVE);
        }

        Map<String, Integer> quantities = aggregate(request.items());
        Map<String, Inventory> lockedInventory = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            requireActiveProduct(entry.getKey());
            Inventory inventory = inventoryRepository.findForUpdate(branch.getBranchId(), entry.getKey())
                    .orElse(null);
            if (inventory == null || inventory.getQuantity() < entry.getValue()) {
                throw new AppException(ErrorCode.SALE_INSUFFICIENT_STOCK);
            }
            lockedInventory.put(entry.getKey(), inventory);
        }

        Customer customer = null;
        if (request.customerId() != null && !request.customerId().isBlank()) {
            customer = customerRepository.findForUpdateByCustomerId(request.customerId().trim())
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
            if (!Boolean.TRUE.equals(customer.getActive())) {
                throw new AppException(ErrorCode.SALE_CUSTOMER_INACTIVE);
            }
        }

        BigDecimal total = BigDecimal.ZERO.setScale(2);
        List<CheckoutItemResponse> responseItems = new ArrayList<>();
        List<InvoiceDetail> details = new ArrayList<>();
        String invoiceId = "HD" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            Product product = requireActiveProduct(entry.getKey());
            BigDecimal unitPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(entry.getValue()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(lineTotal);
            if (total.precision() > 18) {
                throw new AppException(ErrorCode.SALE_TOTAL_OVERFLOW);
            }
            BigDecimal discount = BigDecimal.ZERO.setScale(2);
            details.add(InvoiceDetail.builder()
                    .invoiceId(invoiceId)
                    .productId(entry.getKey())
                    .quantity(entry.getValue())
                    .unitPrice(unitPrice)
                    .discount(discount)
                    .lineTotal(lineTotal)
                    .build());
            responseItems.add(new CheckoutItemResponse(entry.getKey(), entry.getValue(), unitPrice, discount, lineTotal));
        }

        LocalDateTime now = LocalDateTime.now();
        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .branchId(branch.getBranchId())
                .customerId(customer == null ? null : customer.getCustomerId())
                .employeeId(currentEmployeeId(username))
                .createdAt(now)
                .totalAmount(total)
                .paymentMethod(request.paymentMethod().trim())
                .status("DA_THANH_TOAN")
                .build();
        invoiceRepository.save(invoice);
        invoiceDetailRepository.saveAll(details);

        quantities.forEach((productId, quantity) -> {
            Inventory inventory = lockedInventory.get(productId);
            inventory.setQuantity(inventory.getQuantity() - quantity);
            inventory.setUpdatedAt(now);
        });

        Integer pointsEarned = 0;
        if (customer != null) {
            long earned = total.divideToIntegralValue(BigDecimal.valueOf(10_000)).longValue();
            if (earned > Integer.MAX_VALUE) {
                throw new AppException(ErrorCode.SALE_LOYALTY_OVERFLOW);
            }
            pointsEarned = (int) earned;
            long newPoints = (long) customer.getLoyaltyPoints() + pointsEarned;
            if (newPoints > Integer.MAX_VALUE) {
                throw new AppException(ErrorCode.SALE_LOYALTY_OVERFLOW);
            }
            customer.setLoyaltyPoints((int) newPoints);
        }

        return new CheckoutResponse(invoiceId, branch.getBranchId(),
                customer == null ? null : customer.getCustomerId(), invoice.getEmployeeId(), now,
                total, invoice.getPaymentMethod(), invoice.getStatus(), pointsEarned, responseItems);
    }

    private Map<String, Integer> aggregate(List<SaleItemRequest> items) {
        Map<String, Integer> quantities = new TreeMap<>();
        for (SaleItemRequest item : items) {
            String productId = item.productId().trim();
            long quantity = (long) quantities.getOrDefault(productId, 0) + item.quantity();
            if (quantity > Integer.MAX_VALUE) {
                throw new AppException(ErrorCode.SALE_INVALID_ITEMS);
            }
            quantities.put(productId, (int) quantity);
        }
        return quantities;
    }

    private Branch currentBranch(String username) {
        Account account = accountRepository.findByUsername(username)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
        if (account.getRole() != Role.STAFF && account.getRole() != Role.MANAGER) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        Employee employee = employeeRepository.findById(account.getEmployeeId())
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
        return branchRepository.findById(employee.getBranchId())
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
    }

    private String currentEmployeeId(String username) {
        Account account = accountRepository.findByUsername(username)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
        return account.getEmployeeId();
    }

    private Product requireActiveProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new AppException(ErrorCode.SALE_PRODUCT_INACTIVE);
        }
        return product;
    }
}
