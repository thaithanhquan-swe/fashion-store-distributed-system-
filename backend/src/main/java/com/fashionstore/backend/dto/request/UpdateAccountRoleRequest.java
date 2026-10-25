package com.fashionstore.backend.dto.request;

import com.fashionstore.backend.enums.Role;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountRoleRequest(
        @NotNull @JsonAlias({"role", "vaiTro"}) Role role) {
}
