# Frontend

Thư mục dành cho giao diện. Hiện chưa có mã ứng dụng hoặc framework được chọn.

| Màn hình | Nội dung |
| --- | --- |
| Sản phẩm | Danh sách, tìm kiếm, giá bán và trạng thái |
| Tồn kho | Số lượng sản phẩm của chi nhánh hiện tại |
| Khách hàng | Tìm kiếm, tạo và xem thông tin khách hàng |
| Bán hàng | Chọn sản phẩm, số lượng, khách hàng và xác nhận thanh toán |
| Hóa đơn | Danh sách và chi tiết giao dịch |
| Báo cáo Central | Tồn kho, doanh thu từng chi nhánh và trạng thái kết nối |

Giao diện chi nhánh gọi backend tại chi nhánh đó; giao diện Central gọi backend Central. Frontend không kết nối trực tiếp SQL Server và không giữ mật khẩu database.

Tổ chức mã khi triển khai theo các nhóm `pages`, `components`, `services` và `config`. Địa chỉ API được cấu hình theo máy triển khai.

Hiển thị rõ chi nhánh đang sử dụng. Khi xác nhận bán hàng, dùng giá và kết quả kiểm tra tồn do backend trả về; ngăn bấm gửi lặp và hiển thị lỗi nghiệp vụ. Khi mất Central, hiển thị trạng thái đồng bộ nhưng vẫn cho thao tác cục bộ nếu backend chi nhánh còn hoạt động.

Xem [kiến trúc tổng thể](../docs/architecture.md).
