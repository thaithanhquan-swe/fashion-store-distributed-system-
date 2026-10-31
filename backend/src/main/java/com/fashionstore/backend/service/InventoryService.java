package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.ImportInventoryRequest;
import com.fashionstore.backend.dto.request.UpdateInventoryRequest;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    // Chỉ chấp nhận 3 mã chi nhánh chuẩn: CN1, CN2, CN3
    private static final java.util.Set<String> VALID_BRANCH_CODES =
            java.util.Set.of("CN1", "CN2", "CN3");

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
        String trimmed = validateBranchId(branchId);
        Account account = findAccount(username);
        Branch ownBranch = currentBranch(account);
        Branch targetBranch = findBranch(trimmed);
        if (!ownBranch.getBranchId().equalsIgnoreCase(targetBranch.getBranchId())
                && account.getRole() != Role.ADMIN && account.getRole() != Role.MANAGER) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return listBranch(targetBranch.getBranchId(), pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getLowStock(String username, Pageable pageable) {
        Branch branch = currentBranch(findAccount(username));
        return PageResponse.of(inventoryRepository.findLowStock(branch.getBranchId(), pageable)
                .map(inventoryMapper::toResponse));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InventoryResponse importStock(String username, ImportInventoryRequest request) {
        Branch branch = writableBranch(username);
        requireProduct(request.productId());
        Inventory inventory = inventoryRepository.findForUpdate(branch.getBranchId(), request.productId())
                .orElseGet(() -> Inventory.builder()
                        .branchId(branch.getBranchId())
                        .productId(request.productId())
                        .build());
        long newQuantity = (long) inventory.getQuantity() + request.quantity();
        if (newQuantity > Integer.MAX_VALUE) {
            throw new AppException(ErrorCode.INVENTORY_QUANTITY_OVERFLOW);
        }
        inventory.setQuantity((int) newQuantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InventoryResponse update(String username, String productId, UpdateInventoryRequest request) {
        Branch branch = writableBranch(username);
        if (request.quantity() == null && request.minimumStock() == null) {
            throw new AppException(ErrorCode.EMPTY_INVENTORY_UPDATE);
        }
        requireProduct(productId);
        Inventory inventory = inventoryRepository.findForUpdate(branch.getBranchId(), productId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
        inventoryMapper.updateEntity(request, inventory);
        inventory.setUpdatedAt(LocalDateTime.now());
        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
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

    private Branch writableBranch(String username) {
        Account account = findAccount(username);
        if (account.getRole() != Role.ADMIN && account.getRole() != Role.MANAGER) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        Branch branch = currentBranch(account);
        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new AppException(ErrorCode.INVENTORY_BRANCH_INACTIVE);
        }
        return branch;
    }

    private Branch findBranch(String branchId) {
        String trimmed = validateBranchId(branchId);
        return branchRepository.findById(trimmed)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
    }

    private String validateBranchId(String branchId) {
        if (branchId == null || branchId.isBlank()) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }
        String trimmed = branchId.trim().toUpperCase(java.util.Locale.ROOT);
        if (!VALID_BRANCH_CODES.contains(trimmed)) {
            throw new AppException(ErrorCode.BRANCH_INVALID_ID);
        }
        return trimmed;
    }

    private void requireProduct(String productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
