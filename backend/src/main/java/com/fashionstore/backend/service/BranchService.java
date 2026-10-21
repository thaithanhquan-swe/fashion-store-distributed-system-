package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.response.BranchResponse;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.BranchMapper;
import com.fashionstore.backend.repository.BranchRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BranchService {

    BranchRepository branchRepository;
    BranchMapper branchMapper;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BranchResponse create(CreateBranchRequest request) {
        String branchId = GeneratedId.next("CN", branchRepository.findAllByBranchIdStartingWith("CN")
                .stream().map(Branch::getBranchId).toList(), ErrorCode.BRANCH_ID_CAPACITY_EXCEEDED);

        Branch branch = branchMapper.toEntity(request, branchId);
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }
}
