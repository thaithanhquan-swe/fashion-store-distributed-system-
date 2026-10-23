package com.fashionstore.backend.dto.response;

public record BranchResponse(
        String branchId,
        String branchName,
        String address,
        String phoneNumber,
        Boolean active) {
}
