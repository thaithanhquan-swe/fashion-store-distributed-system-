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
   - Tên database: `application-local.properties` mẫu đang trỏ `Fashion_Q1`; đổi `spring.datasource.url` theo máy (Central / Q1 / Q7 / Thủ Đức).
   - Mở SQL Server Management Studio (SSMS) và chạy script tạo database và bảng từ thư mục `database/` (ví dụ `database/Fashion_Q1.sql` hoặc script central).

---

## 2. Cách chạy nhanh nhất: copy `application-local.properties`

> ⚠️ **Đã thay đổi từ commit `1629aba`:** `application.properties` **bị gitignore và không còn
> trong repo**. File mẫu được track là `backend/src/main/resources/application-local.properties`.
> Spring Boot chỉ đọc file `application-local.properties` khi bật profile `local`, nên nếu chỉ
> chạy `mvnw spring-boot:run` ngay sau khi clone, backend sẽ lỗi thiếu datasource.

### Bước 1: Copy file mẫu thành `application.properties`

```powershell
cd backend\src\main\resources
Copy-Item application-local.properties application.properties
```

File `application.properties` đã nằm trong `.gitignore` nên không sợ commit nhầm mật khẩu.

### Bước 2: Sửa cấu hình cho máy của bạn

Mở `backend/src/main/resources/application.properties` và sửa:

1. **Tên database** — chọn đúng database của máy:
   ```properties
   # Central
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=Fashion_Central;encrypt=true;trustServerCertificate=true
   # Q1
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=Fashion_Q1;encrypt=true;trustServerCertificate=true
   ```

2. **Mật khẩu SQL Server**:
   ```properties
   spring.datasource.password=mat_khau_sql_server_cua_ban
   ```

3. **Khóa bí mật JWT** — chuỗi tối thiểu 64 ký tự (512 bits) cho HS512, **giống nhau trên 4 máy**:
   ```properties
   jwt.signerKey=chuoi_khoa_bi_mat_jwt_rieng_cua_ban_it_nhat_64_ky_tu_1234567890abcdef
   ```

> [!TIP]
> Bạn có thể tạo nhanh chuỗi 64 ký tự ngẫu nhiên hoặc dùng bất kỳ chuỗi văn bản nào có độ dài từ 64 ký tự trở lên.

### Bước 3: Khởi chạy Backend

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

## 3. Cách 2: Dùng profile `local` + biến môi trường (không tạo file mới)

Nếu không muốn copy file, bật profile `local` để Spring đọc `application-local.properties`,
rồi truyền mật khẩu / URL / khóa JWT qua biến môi trường:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "local"
$env:SPRING_DATASOURCE_URL = "jdbc:sqlserver://localhost:1433;databaseName=Fashion_Q1;encrypt=true;trustServerCertificate=true"
$env:SPRING_DATASOURCE_PASSWORD = "<mat_khau_sql_server_cua_ban>"
$env:JWT_SIGNER_KEY = "<chuoi_khoa_bi_mat_jwt_hs512_toi_thieu_64_bytes>"
.\mvnw.cmd spring-boot:run
```

> `app.cors.allowed-origins` đã mặc định `http://localhost:5173` trong file mẫu, không cần đặt lại
> trừ khi frontend chạy cổng khác.

---

## 4. Cách 3: Chạy trực tiếp trong IDE (IntelliJ IDEA / VS Code)

1. Mở thư mục dự án `fashion-store-distributed-system` bằng IntelliJ IDEA hoặc VS Code.
2. Tìm đến file `backend/src/main/java/com/fashionstore/backend/BackendApplication.java`.
3. Nhấn nút **Run** (biểu tượng tam giác xanh ▶️ bên cạnh class `BackendApplication`).
4. Đảm bảo đã copy `application-local.properties` → `application.properties` và điền mật khẩu database, khóa JWT (hoặc cấu hình Environment Variables + `SPRING_PROFILES_ACTIVE=local` trong Run Configuration của IDE).

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
| **`Failed to configure a DataSource` / không có URL database** | Chưa có `application.properties` và chưa bật profile `local`. | Làm mục 2: copy `application-local.properties` → `application.properties`, hoặc đặt `SPRING_PROFILES_ACTIVE=local`. |
| **`Cannot open database "Fashion_Central"`** | Chưa có database trong SQL Server hoặc `spring.datasource.url` trỏ sai tên. | Mở SSMS, chạy đúng file `database/Fashion_*.sql`; sửa `databaseName=` cho khớp máy. |
| **`The TCP/IP connection to the host localhost, port 1433 has failed`** | Dịch vụ SQL Server chưa bật cổng TCP/IP. | Mở `SQL Server Configuration Manager` > `SQL Server Network Configuration` > `Protocols for MSSQLSERVER` > Bật **TCP/IP** thành `Enabled` > Khởi động lại service SQL Server. |
| **`JWT signer key must be at least 64 bytes for HS512`** | Chưa cấu hình hoặc cấu hình chuỗi bí mật JWT quá ngắn. | Đảm bảo file `application.properties` (hoặc biến môi trường `JWT_SIGNER_KEY`) có cấu hình `jwt.signerKey` với chuỗi bí mật độ dài tối thiểu 64 bytes (512 bits) để bảo mật cho thuật toán HS512. |
| **`Address already in use: bind (port 8080)`** | Đang có ứng dụng khác chạy chiếm cổng 8080. | Tìm và tắt tiến trình đang chiếm cổng 8080 bằng lệnh `netstat -ano \| findstr :8080` rồi `taskkill /F /PID <PID>`, hoặc đổi cổng backend thành `server.port=8081`. |
| **`Lỗi CORS khi gọi từ Frontend`** | Frontend Vite chạy ở port khác `5173`. | Kiểm tra file `application.properties` có dòng `app.cors.allowed-origins=http://localhost:5173`. |
