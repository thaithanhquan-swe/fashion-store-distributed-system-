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
        Branch branch = branchRepository.findById(branchId.trim())
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
        return branchMapper.toResponse(branch);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BranchResponse create(CreateBranchRequest request) {
        String branchId = GeneratedId.next("CN", branchRepository.findAllByBranchIdStartingWith("CN")
                .stream().map(Branch::getBranchId).toList(), ErrorCode.BRANCH_ID_CAPACITY_EXCEEDED);

        Branch branch = branchMapper.toEntity(request, branchId);
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }
}
