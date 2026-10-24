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
    PRODUCT_NOT_FOUND(1014, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    INVALID_PRODUCT_PRICE_RANGE(1015, "Giá tối thiểu không được lớn hơn giá tối đa", HttpStatus.BAD_REQUEST),
    PRODUCT_ID_CAPACITY_EXCEEDED(1016, "Đã vượt giới hạn tạo mã sản phẩm", HttpStatus.CONFLICT),
    INVALID_PAGE_REQUEST(1017, "Trang phải từ 0 trở lên và kích thước trang từ 1 đến 100", HttpStatus.BAD_REQUEST),
    CUSTOMER_NOT_FOUND(1018, "Không tìm thấy khách hàng", HttpStatus.NOT_FOUND),
    CUSTOMER_PHONE_ALREADY_EXISTS(1019, "Số điện thoại khách hàng đã tồn tại", HttpStatus.CONFLICT),
    CUSTOMER_ID_CAPACITY_EXCEEDED(1020, "Đã vượt giới hạn tạo mã khách hàng", HttpStatus.CONFLICT),
    INVENTORY_NOT_FOUND(1021, "Chưa có tồn kho của sản phẩm tại chi nhánh này", HttpStatus.NOT_FOUND),
    INVENTORY_QUANTITY_OVERFLOW(1022, "Số lượng tồn vượt giới hạn cho phép", HttpStatus.CONFLICT),
    EMPTY_INVENTORY_UPDATE(1023, "Cần cung cấp số lượng tồn hoặc mức tồn tối thiểu", HttpStatus.BAD_REQUEST),
    INVENTORY_BRANCH_INACTIVE(1024, "Chi nhánh đã ngừng hoạt động, không thể thay đổi tồn kho", HttpStatus.CONFLICT),
    CONCURRENT_UPDATE(1025, "Dữ liệu đang được cập nhật bởi giao dịch khác, vui lòng thử lại", HttpStatus.CONFLICT),
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
