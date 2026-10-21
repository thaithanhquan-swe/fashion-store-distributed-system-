package com.fashionstore.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Mã nhân viên không được để trống")
        @Size(max = 10, message = "Mã nhân viên không được vượt quá 10 ký tự")
        String employeeId,
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(max = 50, message = "Tên đăng nhập không được vượt quá 50 ký tự")
        String username,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String password) {
}
