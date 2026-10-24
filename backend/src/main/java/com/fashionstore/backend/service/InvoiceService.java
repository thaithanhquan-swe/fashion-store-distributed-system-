package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.response.CustomerInvoiceResponse;
import com.fashionstore.backend.dto.response.InvoiceDetailResponse;
import com.fashionstore.backend.dto.response.InvoiceResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Customer;
import com.fashionstore.backend.entity.Employee;
import com.fashionstore.backend.entity.Inventory;
import com.fashionstore.backend.entity.Invoice;
import com.fashionstore.backend.entity.InvoiceDetail;
import com.fashionstore.backend.entity.Product;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.CustomerInvoiceMapper;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.CustomerRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import com.fashionstore.backend.repository.InventoryRepository;
import com.fashionstore.backend.repository.InvoiceDetailRepository;
import com.fashionstore.backend.repository.InvoiceRepository;
import com.fashionstore.backend.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceService {

    AccountRepository accountRepository;
    CustomerInvoiceMapper customerInvoiceMapper;
    EmployeeRepository employeeRepository;
    CustomerRepository customerRepository;
    InvoiceRepository invoiceRepository;
    InvoiceDetailRepository invoiceDetailRepository;
    ProductRepository productRepository;
    InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public PageResponse<CustomerInvoiceResponse> list(String username, Pageable pageable) {
        Account account = findAccount(username);
        Page<Invoice> invoices;
        if (account.getRole() == Role.ADMIN) {
            invoices = invoiceRepository.findAll(pageable);
        } else {
            String branchId = currentBranchId(account);
            invoices = invoiceRepository.findAllByBranchId(branchId, pageable);
        }
        return PageResponse.of(invoices.map(customerInvoiceMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(String username, String invoiceId) {
        Account account = findAccount(username);
        Invoice invoice = findInvoice(invoiceId);
        ensureCanAccess(account, invoice);
        return toResponse(invoice);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InvoiceResponse cancel(String username, String invoiceId) {
        Account account = findAccount(username);
        Invoice invoice = invoiceRepository.findForUpdate(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));
        ensureCanAccess(account, invoice);
        if ("DA_HUY".equals(invoice.getStatus())) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_CANCELLED);
        }
        if (!"DA_THANH_TOAN".equals(invoice.getStatus())) {
            throw new AppException(ErrorCode.INVOICE_CANNOT_CANCEL);
        }

        List<InvoiceDetail> details = invoiceDetailRepository.findAllByInvoiceIdOrderByProductId(invoiceId);
        Map<String, Inventory> inventories = new TreeMap<>();
        for (InvoiceDetail detail : details) {
            Inventory inventory = inventoryRepository.findForUpdate(invoice.getBranchId(), detail.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
            if ((long) inventory.getQuantity() + detail.getQuantity() > Integer.MAX_VALUE) {
                throw new AppException(ErrorCode.INVOICE_STOCK_OVERFLOW);
            }
            inventories.put(detail.getProductId(), inventory);
        }

        for (InvoiceDetail detail : details) {
            Inventory inventory = inventories.get(detail.getProductId());
            inventory.setQuantity(inventory.getQuantity() + detail.getQuantity());
            inventory.setUpdatedAt(java.time.LocalDateTime.now());
        }

        if (invoice.getCustomerId() != null) {
            Customer customer = customerRepository.findForUpdateByCustomerId(invoice.getCustomerId())
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
            long earnedPoints = invoice.getTotalAmount()
                    .divideToIntegralValue(java.math.BigDecimal.valueOf(10_000)).longValue();
            customer.setLoyaltyPoints((int) Math.max(0L, customer.getLoyaltyPoints() - earnedPoints));
        }
        invoice.setStatus("DA_HUY");
        return toResponse(invoice);
    }

    private Account findAccount(String username) {
        return accountRepository.findByUsername(username)
                .filter(account -> Boolean.TRUE.equals(account.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
    }

    private Invoice findInvoice(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));
    }

    private void ensureCanAccess(Account account, Invoice invoice) {
        if (account.getRole() == Role.ADMIN) {
            return;
        }
        if (account.getRole() != Role.STAFF && account.getRole() != Role.MANAGER) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (!currentBranchId(account).equals(invoice.getBranchId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private String currentBranchId(Account account) {
        Employee employee = employeeRepository.findById(account.getEmployeeId())
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
        return employee.getBranchId();
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceDetail> details = invoiceDetailRepository.findAllByInvoiceIdOrderByProductId(invoice.getInvoiceId());
        Map<String, Product> products = new TreeMap<>();
        for (Product product : productRepository.findAllById(
                details.stream().map(InvoiceDetail::getProductId).toList())) {
            products.put(product.getProductId(), product);
        }
        List<InvoiceDetailResponse> items = details.stream().map(detail -> {
            Product product = products.get(detail.getProductId());
            return new InvoiceDetailResponse(
                    detail.getProductId(),
                    product == null ? null : product.getProductName(),
                    product == null ? null : product.getCategory(),
                    product == null ? null : product.getColor(),
                    product == null ? null : product.getSize(),
                    detail.getQuantity(), detail.getUnitPrice(), detail.getDiscount(), detail.getLineTotal());
        }).toList();
        return new InvoiceResponse(invoice.getInvoiceId(), invoice.getBranchId(), invoice.getCustomerId(),
                invoice.getEmployeeId(), invoice.getCreatedAt(), invoice.getTotalAmount(),
                invoice.getPaymentMethod(), invoice.getStatus(), items);
    }

}
