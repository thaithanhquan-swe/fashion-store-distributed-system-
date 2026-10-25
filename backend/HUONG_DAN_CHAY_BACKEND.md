# HƯỚNG DẪN CHẠY BACKEND FASHION STORE

Tài liệu này hướng dẫn chi tiết từng bước để bất kỳ thành viên nào trong nhóm cũng có thể khởi chạy backend một cách dễ dàng, nhanh chóng và không gặp lỗi.

---

## 1. Yêu cầu môi trường (Cài đặt trước)

1. **Java JDK 21 trở lên**:
   - Kiểm tra bằng lệnh: `java -version`
   - Nếu chưa có, tải và cài đặt [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) hoặc [Eclipse Temurin 21](https://adoptium.net/).
2. **Microsoft SQL Server**:
   - SQL Server 2019 / 2022 (Developer hoặc Standard edition).
   - Đã bật xác thực **SQL Server and Windows Authentication mode** (tài khoản `sa`).
   - Đã bật TCP/IP trên cổng mặc định `1433`.
3. **Database**:
   - Tên database mặc định: `Fashion_Central` (hoặc `Fashion_Q1`).
   - Mở SQL Server Management Studio (SSMS) và chạy script tạo database và bảng từ thư mục `database/` (ví dụ `database/Fashion_Q1.sql` hoặc script central).

---

## 2. Cách chạy nhanh nhất: Cấu hình trong `application.properties`

Tất cả cấu hình mặc định (cổng 8080, URL kết nối database, CORS cho Vite 5173, tự động khởi tạo tài khoản demo) đã được thiết lập sẵn trong file `backend/src/main/resources/application.properties`.

### Bước 1: Mở file `application.properties`
Mở file `backend/src/main/resources/application.properties` và điền hai trường bảo mật:

1. **Mật khẩu SQL Server** (dòng 8): Điền mật khẩu tài khoản `sa` của SQL Server máy bạn:
   ```properties
   spring.datasource.password=mat_khau_sql_server_cua_ban
   ```

2. **Khóa bí mật JWT** (dòng 22): Điền một chuỗi bí mật bất kỳ dài tối thiểu 64 ký tự (512 bits) để ký token bảo mật HS512:
   ```properties
   jwt.signerKey=chuoi_khoa_bi_mat_jwt_rieng_cua_ban_it_nhat_64_ky_tu_1234567890abcdef
   ```

> [!TIP]
> Bạn có thể tạo nhanh chuỗi 64 ký tự ngẫu nhiên hoặc dùng bất kỳ chuỗi văn bản nào có độ dài từ 64 ký tự trở lên.

### Bước 2: Khởi chạy Backend
Mở PowerShell tại thư mục `backend` và chạy:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

*(Trên Linux / macOS / Git Bash: `./mvnw spring-boot:run`)*

Khi thấy dòng chữ sau xuất hiện ở cuối terminal là backend đã khởi động thành công:
```text
Started BackendApplication in X.XXX seconds
```

---

## 3. Cách 2: Chạy bằng biến môi trường (Không cần sửa file)

Nếu không muốn sửa file `application.properties`, bạn có thể truyền thẳng mật khẩu SQL Server và khóa JWT qua biến môi trường trong PowerShell:

```powershell
cd backend
$env:SPRING_DATASOURCE_PASSWORD = "<mat_khau_sql_server_cua_ban>"
$env:JWT_SIGNER_KEY = "<chuoi_khoa_bi_mat_jwt_hs512_toi_thieu_64_bytes>"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

---

## 4. Cách 3: Chạy trực tiếp trong IDE (IntelliJ IDEA / VS Code)

1. Mở thư mục dự án `fashion-store-distributed-system` bằng IntelliJ IDEA hoặc VS Code.
2. Tìm đến file `backend/src/main/java/com/fashionstore/backend/BackendApplication.java`.
3. Nhấn nút **Run** (biểu tượng tam giác xanh ▶️ bên cạnh class `BackendApplication`).
4. Đảm bảo đã điền mật khẩu database và khóa JWT trong `application.properties` (hoặc cấu hình Environment Variables trong Run Configuration của IDE).

---

## 5. Tài khoản đăng nhập có sẵn (Bootstrap Demo Accounts)

Khi backend khởi động, hệ thống **tự động kiểm tra và tạo sẵn** 3 tài khoản demo đầy đủ quyền để bạn đăng nhập ngay trên frontend hoặc test API:

| Tên đăng nhập | Vai trò (Role) | Chi nhánh | Mật khẩu chung | Mô tả quyền hạn |
| :--- | :--- | :--- | :--- | :--- |
| **`admin`** | `ADMIN` | Toàn chuỗi | `123456` | Toàn quyền quản trị: xem thống kê toàn chuỗi, quản lý tài khoản, đổi role, xem dashboard admin. |
| **`manager`** | `MANAGER` | Chi nhánh Q1 | `123456` | Quản lý cửa hàng: xem thống kê và nhân viên chi nhánh mình, nhập kho, cảnh báo hàng sắp hết. |
| **`staff`** | `STAFF` | Chi nhánh Q1 | `123456` | Nhân viên bán hàng: lập hóa đơn, xem tồn kho chi nhánh, hiệu suất cá nhân. |

*(Backend cũng tạo sẵn các alias `manager.q1` và `staff.q1` với mật khẩu `123456`)*.

---

## 6. Kiểm tra xem Backend đã hoạt động tốt chưa

Sau khi chạy backend, bạn mở trình duyệt web lên và kiểm tra:

1. **Giao diện Swagger UI (Tài liệu API tương tác trực quan)**:
   - Đường dẫn: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
   - Tại đây bạn có thể xem tất cả endpoint và bấm "Try it out" để gọi thử API trực tiếp.
2. **OpenAPI JSON**:
   - Đường dẫn: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
3. **Thử đăng nhập qua PowerShell**:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"admin","password":"123456"}'
   ```
   Nếu nhận về `token` JWT nghĩa là backend đã sẵn sàng 100%!

---

## 7. Khắc phục các lỗi thường gặp (Troubleshooting)

| Lỗi gặp phải | Nguyên nhân | Cách xử lý |
| :--- | :--- | :--- |
| **`Login failed for user 'sa'`** | Mật khẩu `sa` của SQL Server không khớp. | Kiểm tra lại mật khẩu `spring.datasource.password` trong file `application.properties`. Hãy thử đăng nhập vào SSMS bằng tài khoản `sa` với mật khẩu đó trước. |
| **`Cannot open database "Fashion_Central"`** | Chưa có database trong SQL Server. | Mở SSMS, tạo database mới bằng lệnh `CREATE DATABASE Fashion_Central;` sau đó chạy file schema tạo bảng. |
| **`The TCP/IP connection to the host localhost, port 1433 has failed`** | Dịch vụ SQL Server chưa bật cổng TCP/IP. | Mở `SQL Server Configuration Manager` > `SQL Server Network Configuration` > `Protocols for MSSQLSERVER` > Bật **TCP/IP** thành `Enabled` > Khởi động lại service SQL Server. |
| **`JWT signer key must be at least 64 bytes for HS512`** | Chưa cấu hình hoặc cấu hình chuỗi bí mật JWT quá ngắn. | Đảm bảo file `application.properties` (hoặc biến môi trường `JWT_SIGNER_KEY`) có cấu hình `jwt.signerKey` với chuỗi bí mật độ dài tối thiểu 64 bytes (512 bits) để bảo mật cho thuật toán HS512. |
| **`Address already in use: bind (port 8080)`** | Đang có ứng dụng khác chạy chiếm cổng 8080. | Tìm và tắt tiến trình đang chiếm cổng 8080 bằng lệnh `netstat -ano \| findstr :8080` rồi `taskkill /F /PID <PID>`, hoặc đổi cổng backend thành `server.port=8081`. |
| **`Lỗi CORS khi gọi từ Frontend`** | Frontend Vite chạy ở port khác `5173`. | Kiểm tra file `application.properties` có dòng `app.cors.allowed-origins=http://localhost:5173`. |
