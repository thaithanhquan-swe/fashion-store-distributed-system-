package com.fashionstore.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Size(min = 6, max = 72) @JsonAlias({"newPassword", "password", "matKhauMoi"}) String newPassword) {
}
