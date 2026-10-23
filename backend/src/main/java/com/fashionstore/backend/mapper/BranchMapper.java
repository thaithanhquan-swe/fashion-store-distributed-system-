package com.fashionstore.backend.mapper;

import com.fashionstore.backend.dto.request.CreateBranchRequest;
import com.fashionstore.backend.dto.response.BranchResponse;
import com.fashionstore.backend.entity.Branch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    @Mapping(target = "branchId", source = "branchId")
    @Mapping(target = "active", constant = "true")
    Branch toEntity(CreateBranchRequest request, String branchId);

    BranchResponse toResponse(Branch branch);
}
