package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateCustomerRequest;
import com.fashionstore.backend.dto.request.UpdateCustomerRequest;
import com.fashionstore.backend.dto.request.UpdateCustomerStatusRequest;
import com.fashionstore.backend.dto.response.CustomerInvoiceResponse;
import com.fashionstore.backend.dto.response.CustomerResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.entity.Customer;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.CustomerInvoiceMapper;
import com.fashionstore.backend.mapper.CustomerMapper;
import com.fashionstore.backend.repository.CustomerRepository;
import com.fashionstore.backend.repository.InvoiceRepository;
import com.fashionstore.backend.specification.CustomerSpecifications;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerService {

    CustomerRepository customerRepository;
    InvoiceRepository invoiceRepository;
    CustomerMapper customerMapper;
    CustomerInvoiceMapper customerInvoiceMapper;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(Boolean active, Pageable pageable) {
        return PageResponse.of(customerRepository.findAll(
                        CustomerSpecifications.filter(null, null, active), pageable)
                .map(customerMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(
            String keyword, String phone, Boolean active, Pageable pageable) {
        return PageResponse.of(customerRepository.findAll(
                        CustomerSpecifications.filter(normalize(keyword), normalize(phone), active), pageable)
                .map(customerMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(String customerId) {
        return customerMapper.toResponse(findCustomer(customerId));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CustomerResponse create(CreateCustomerRequest request) {
        String phone = normalize(request.phoneNumber());
        String customerId = GeneratedId.next("KH", customerRepository.findAllByCustomerIdStartingWith("KH")
                .stream().map(Customer::getCustomerId).toList(), ErrorCode.CUSTOMER_ID_CAPACITY_EXCEEDED);
        checkPhoneAvailable(phone, null);

        Customer customer = customerMapper.toEntity(request, customerId, LocalDate.now());
        customer.setFullName(request.fullName().trim());
        customer.setPhoneNumber(phone);
        customer.setEmail(normalize(request.email()));
        customer.setAddress(normalize(request.address()));
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(String customerId, UpdateCustomerRequest request) {
        Customer customer = findCustomer(customerId);
        String phone = normalize(request.phoneNumber());
        checkPhoneAvailable(phone, customer.getPhoneNumber());

        customerMapper.updateEntity(request, customer);
        customer.setFullName(request.fullName().trim());
        customer.setPhoneNumber(phone);
        customer.setEmail(normalize(request.email()));
        customer.setAddress(normalize(request.address()));
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse updateStatus(String customerId, UpdateCustomerStatusRequest request) {
        Customer customer = findCustomer(customerId);
        customer.setActive(request.active());
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerInvoiceResponse> getInvoices(String customerId, Pageable pageable) {
        findCustomer(customerId);
        return PageResponse.of(invoiceRepository.findByCustomerId(customerId, pageable)
                .map(customerInvoiceMapper::toResponse));
    }

    private Customer findCustomer(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private void checkPhoneAvailable(String phone, String existingPhone) {
        if (phone != null && !phone.equals(existingPhone)
                && customerRepository.existsByPhoneNumber(phone)) {
            throw new AppException(ErrorCode.CUSTOMER_PHONE_ALREADY_EXISTS);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
