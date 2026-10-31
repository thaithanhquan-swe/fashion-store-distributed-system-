package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.response.BranchResponse;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.BranchMapper;
import com.fashionstore.backend.repository.BranchRepository;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BranchService {

    BranchRepository branchRepository;
    BranchMapper branchMapper;

    // Chỉ chấp nhận 3 mã chi nhánh chuẩn: CN1, CN2, CN3 (khớp PDF filter MACN='CN1')
    private static final java.util.Set<String> VALID_BRANCH_CODES =
            java.util.Set.of("CN1", "CN2", "CN3");

    @Transactional(readOnly = true)
    public List<BranchResponse> getAll(Boolean active) {
        List<Branch> branches;
        if (active != null) {
            branches = branchRepository.findAllByActiveOrderByBranchIdAsc(active);
        } else {
            branches = branchRepository.findAll(Sort.by(Sort.Direction.ASC, "branchId"));
        }
        return branches.stream().map(branchMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getById(String branchId) {
        if (branchId == null || branchId.isBlank()) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }
        String trimmed = branchId.trim().toUpperCase(java.util.Locale.ROOT);
        if (!VALID_BRANCH_CODES.contains(trimmed)) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }
        Branch branch = branchRepository.findById(trimmed)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
        return branchMapper.toResponse(branch);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BranchResponse create(CreateBranchRequest request, String branchId) {
        // Chỉ cho phép tạo 3 chi nhánh cố định CN1/CN2/CN3
        String trimmed = branchId != null ? branchId.trim().toUpperCase(java.util.Locale.ROOT) : "";
        if (!VALID_BRANCH_CODES.contains(trimmed)) {
            throw new AppException(ErrorCode.BRANCH_INVALID_ID);
        }
        if (branchRepository.existsById(trimmed)) {
            throw new AppException(ErrorCode.BRANCH_ALREADY_EXISTS);
        }
        Branch branch = branchMapper.toEntity(request, trimmed);
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }
}
