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
    BRANCH_INVALID_ID(1004, "Mã chi nhánh không hợp lệ (chỉ chấp nhận CN1, CN2, CN3)", HttpStatus.BAD_REQUEST),
    BRANCH_ALREADY_EXISTS(1005, "Chi nhánh đã tồn tại", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS(1006, "Tên đăng nhập đã tồn tại", HttpStatus.CONFLICT),
    EMPLOYEE_ACCOUNT_ALREADY_EXISTS(1007, "Nhân viên đã có tài khoản", HttpStatus.CONFLICT),
    EMPLOYEE_NOT_FOUND(1008, "Không tìm thấy nhân viên", HttpStatus.NOT_FOUND),
    EMPLOYEE_ID_CAPACITY_EXCEEDED(1009, "Đã vượt giới hạn tạo mã nhân viên", HttpStatus.CONFLICT),
    BRANCH_ID_CAPACITY_EXCEEDED(1010, "Đã vượt giới hạn tạo mã chi nhánh", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(1011, "Tên đăng nhập hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(1012, "Refresh token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1013, "Bạn không có quyền truy cập endpoint này", HttpStatus.FORBIDDEN),
    CURRENT_PASSWORD_INCORRECT(1014, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(1015, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    INVALID_PRODUCT_PRICE_RANGE(1016, "Giá tối thiểu không được lớn hơn giá tối đa", HttpStatus.BAD_REQUEST),
    PRODUCT_ID_CAPACITY_EXCEEDED(1017, "Đã vượt giới hạn tạo mã sản phẩm", HttpStatus.CONFLICT),
    INVALID_PAGE_REQUEST(1018, "Trang phải từ 0 trở lên và kích thước trang từ 1 đến 100", HttpStatus.BAD_REQUEST),
    CUSTOMER_NOT_FOUND(1019, "Không tìm thấy khách hàng", HttpStatus.NOT_FOUND),
    CUSTOMER_PHONE_ALREADY_EXISTS(1020, "Số điện thoại khách hàng đã tồn tại", HttpStatus.CONFLICT),
    CUSTOMER_ID_CAPACITY_EXCEEDED(1021, "Đã vượt giới hạn tạo mã khách hàng", HttpStatus.CONFLICT),
    INVENTORY_NOT_FOUND(1022, "Chưa có tồn kho của sản phẩm tại chi nhánh này", HttpStatus.NOT_FOUND),
    INVENTORY_QUANTITY_OVERFLOW(1023, "Số lượng tồn vượt giới hạn cho phép", HttpStatus.CONFLICT),
    EMPTY_INVENTORY_UPDATE(1024, "Cần cung cấp số lượng tồn hoặc mức tồn tối thiểu", HttpStatus.BAD_REQUEST),
    INVENTORY_BRANCH_INACTIVE(1025, "Chi nhánh đã ngừng hoạt động, không thể thay đổi tồn kho", HttpStatus.CONFLICT),
    CONCURRENT_UPDATE(1026, "Dữ liệu đang được cập nhật bởi giao dịch khác, vui lòng thử lại", HttpStatus.CONFLICT),
    SALE_INVALID_ITEMS(1027, "Danh sách sản phẩm không hợp lệ hoặc tổng số lượng vượt giới hạn", HttpStatus.BAD_REQUEST),
    SALE_INSUFFICIENT_STOCK(1028, "Tồn kho không đủ để hoàn tất thanh toán", HttpStatus.CONFLICT),
    SALE_PRODUCT_INACTIVE(1029, "Sản phẩm đã ngừng kinh doanh", HttpStatus.CONFLICT),
    SALE_CUSTOMER_INACTIVE(1030, "Khách hàng đã ngừng hoạt động", HttpStatus.CONFLICT),
    SALE_BRANCH_INACTIVE(1031, "Chi nhánh đã ngừng hoạt động, không thể bán hàng", HttpStatus.CONFLICT),
    SALE_TOTAL_OVERFLOW(1032, "Tổng tiền hóa đơn vượt giới hạn cho phép", HttpStatus.BAD_REQUEST),
    SALE_LOYALTY_OVERFLOW(1033, "Điểm tích lũy khách hàng vượt giới hạn cho phép", HttpStatus.CONFLICT),
    INVOICE_NOT_FOUND(1034, "Không tìm thấy hóa đơn", HttpStatus.NOT_FOUND),
    INVOICE_ALREADY_CANCELLED(1035, "Hóa đơn đã được hủy trước đó", HttpStatus.CONFLICT),
    INVOICE_CANNOT_CANCEL(1036, "Chỉ có thể hủy hóa đơn đã thanh toán", HttpStatus.CONFLICT),
    INVOICE_STOCK_OVERFLOW(1037, "Số lượng tồn vượt giới hạn khi hoàn hàng", HttpStatus.CONFLICT),
    ACCOUNT_NOT_FOUND(1038, "Không tìm thấy tài khoản", HttpStatus.NOT_FOUND),
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
