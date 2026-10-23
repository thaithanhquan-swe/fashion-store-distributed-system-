package com.fashionstore.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 10) String branchId,
        @NotBlank @Size(max = 100) String fullName,
        @Past LocalDate birthDate,
        @Size(max = 10) String gender,
        @Size(max = 15) String phoneNumber,
        @Email @Size(max = 100) String email,
        @Size(max = 50) String position) {
}
