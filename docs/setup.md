# Hướng dẫn sử dụng bộ khung

## Trạng thái hiện tại

Repository chứa tài liệu và script kiểm tra chỉ đọc. Chưa có bản backup, DDL tạo bảng, ứng dụng chạy được hoặc cấu hình replication tự động. README ghi nhận database đã có sẵn trên các máy; cần lấy schema/backup thực tế từ nhóm để triển khai tiếp.

## Thứ tự thao tác

1. Chuẩn bị bốn máy, SQL Server, SSMS, driver và Radmin VPN theo README.
2. Restore database hiện có đúng tên trên từng máy. Không chạy lệnh tạo lại bảng khi chưa đối chiếu schema.
3. Trong SSMS, mở file tương ứng và xác nhận đúng SQL Server trước khi thực thi:

   | Máy | Database | File |
   | --- | --- | --- |
   | Central | Fashion_Central | database/central.sql |
   | Quận 1 | Fashion_Q1 | database/q1.sql |
   | Quận 7 | Fashion_Q7 | database/q7.sql |
   | Thủ Đức | Fashion_ThuDuc | database/thuduc.sql |

4. Chọn database vừa kiểm tra trong SSMS rồi chạy `database/common.sql` để xem tên cột và kiểu dữ liệu. Đối chiếu thêm khóa và ràng buộc từ script xuất schema của SSMS.
5. Cấu hình Linked Server theo README mục 24–27 bằng thông tin thực tế của nhóm. Các file mới không tự tạo kết nối hoặc login.
6. Sau khi xác nhận schema và kết nối, chạy từng câu kiểm tra/truy vấn mẫu đang được comment trong file của site hoặc Central.
7. Bổ sung nghiệp vụ bán hàng và cơ chế replication, sau đó thực hiện các kiểm tra bên dưới.

Không chạy tuần tự cả bốn file site trên cùng một máy theo mặc định. `common.sql` dùng database đang chọn; các file còn lại có lệnh `USE` chỉ định database tương ứng.

## Tiêu chí kiểm tra khi đã triển khai nghiệp vụ

- Mỗi site chỉ chứa `TonKho` và `HoaDon` đúng mã chi nhánh; chi tiết liên kết được với hóa đơn cha.
- Bán 2 sản phẩm từ tồn 10: tạo đúng một hóa đơn và tồn còn 8; các site khác không đổi.
- Bán vượt tồn hoặc lỗi ghi chi tiết: không tạo hóa đơn dở dang, tồn không bị trừ.
- Hai giao dịch mua cùng lúc: không làm tồn âm.
- Gửi lại cùng yêu cầu sau lỗi mạng: không tạo thêm hóa đơn.
- Central tổng hợp được đủ ba site; nếu một site mất kết nối thì báo thiếu dữ liệu/lỗi rõ ràng.
- Ngắt Central: site vẫn bán hàng cục bộ; kết nối lại thì thay đổi khách hàng được đồng bộ theo cơ chế đã chọn.
- Cập nhật cùng khách hàng ở hai site: xử lý xung đột theo quy tắc đã thống nhất.

Các mục này là tiêu chí cần chạy trên hệ thống thực tế, chưa phải kết quả kiểm thử.
