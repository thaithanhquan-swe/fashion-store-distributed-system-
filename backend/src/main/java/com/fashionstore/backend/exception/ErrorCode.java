package com.fashionstore.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(1001, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    EMPLOYEE_ALREADY_EXISTS(1002, "Mã nhân viên đã tồn tại", HttpStatus.CONFLICT),
    BRANCH_NOT_FOUND(1003, "Không tìm thấy chi nhánh", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(1004, "Tên đăng nhập đã tồn tại", HttpStatus.CONFLICT),
    EMPLOYEE_ACCOUNT_ALREADY_EXISTS(1005, "Nhân viên đã có tài khoản", HttpStatus.CONFLICT),
    EMPLOYEE_NOT_FOUND(1006, "Không tìm thấy nhân viên", HttpStatus.NOT_FOUND),
    EMPLOYEE_ID_CAPACITY_EXCEEDED(1007, "Đã vượt giới hạn tạo mã nhân viên", HttpStatus.CONFLICT),
    BRANCH_ID_CAPACITY_EXCEEDED(1008, "Đã vượt giới hạn tạo mã chi nhánh", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(1009, "Tên đăng nhập hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(1010, "Refresh token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1011, "Bạn không có quyền truy cập endpoint này", HttpStatus.FORBIDDEN),
    CURRENT_PASSWORD_INCORRECT(1012, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
