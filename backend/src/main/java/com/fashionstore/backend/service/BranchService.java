package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.response.BranchResponse;
import com.fashionstore.backend.entity.Branch;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.mapper.BranchMapper;
import com.fashionstore.backend.repository.BranchRepository;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BranchService {

    BranchRepository branchRepository;
    BranchMapper branchMapper;

    @Transactional
    public BranchResponse create(CreateBranchRequest request) {
        Integer highestNumber = branchRepository.findHighestGeneratedBranchNumber();
        int nextNumber = (highestNumber == null ? 0 : highestNumber) + 1;
        if (nextNumber > 99_999_999) {
            throw new AppException(ErrorCode.BRANCH_ID_CAPACITY_EXCEEDED);
        }
        String branchId = String.format(Locale.ROOT, "CN%08d", nextNumber);

        Branch branch = branchMapper.toEntity(request, branchId);
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }
}
