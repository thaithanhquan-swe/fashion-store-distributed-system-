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
        Branch branch = findBranch(branchId.trim());
        return branchMapper.toResponse(branch);
    }

    private Branch findBranch(String branchId) {
        java.util.Optional<Branch> branch = branchRepository.findById(branchId);
        if (branch.isPresent()) {
            return branch.get();
        }
        if (branchId.matches("(?i)CN\\d+")) {
            try {
                int num = Integer.parseInt(branchId.substring(2));
                for (String alias : java.util.List.of(
                        String.format(java.util.Locale.ROOT, "CN%03d", num),
                        String.format(java.util.Locale.ROOT, "CN%02d", num),
                        String.format(java.util.Locale.ROOT, "CN%d", num))) {
                    if (!alias.equalsIgnoreCase(branchId)) {
                        java.util.Optional<Branch> found = branchRepository.findById(alias);
                        if (found.isPresent()) {
                            return found.get();
                        }
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
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
