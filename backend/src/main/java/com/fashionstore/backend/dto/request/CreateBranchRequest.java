package com.fashionstore.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @NotBlank @Size(max = 100) String branchName,
        @Size(max = 255) String address,
        @Size(max = 15) String phoneNumber) {
}
