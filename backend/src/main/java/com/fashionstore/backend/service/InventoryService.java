package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.response.InventoryResponse;
import com.fashionstore.backend.dto.response.PageResponse;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.entity.Employee;
import com.fashionstore.backend.entity.Inventory;
import com.fashionstore.backend.entity.InventoryId;
import com.fashionstore.backend.enums.Role;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.InventoryMapper;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.BranchRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import com.fashionstore.backend.repository.InventoryRepository;
import com.fashionstore.backend.repository.ProductRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryService {

    InventoryRepository inventoryRepository;
    InventoryMapper inventoryMapper;
    AccountRepository accountRepository;
    EmployeeRepository employeeRepository;
    BranchRepository branchRepository;
    ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getCurrentBranch(String username, Pageable pageable) {
        Branch branch = currentBranch(findAccount(username));
        return listBranch(branch.getBranchId(), pageable);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getProduct(String username, String productId) {
        Branch branch = currentBranch(findAccount(username));
        requireProduct(productId);
        Inventory inventory = inventoryRepository.findById(new InventoryId(branch.getBranchId(), productId))
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getBranch(String username, String branchId, Pageable pageable) {
        Account account = findAccount(username);
        Branch ownBranch = currentBranch(account);
        if (!ownBranch.getBranchId().equals(branchId)
                && account.getRole() != Role.ADMIN && account.getRole() != Role.MANAGER) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        findBranch(branchId);
        return listBranch(branchId, pageable);
    }

    private PageResponse<InventoryResponse> listBranch(String branchId, Pageable pageable) {
        return PageResponse.of(inventoryRepository.findAllByBranchId(branchId, pageable)
                .map(inventoryMapper::toResponse));
    }

    private Account findAccount(String username) {
        return accountRepository.findByUsername(username)
                .filter(account -> Boolean.TRUE.equals(account.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
    }

    private Branch currentBranch(Account account) {
        Employee employee = employeeRepository.findById(account.getEmployeeId())
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
        return findBranch(employee.getBranchId());
    }

    private Branch findBranch(String branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
    }

    private void requireProduct(String productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
