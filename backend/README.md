# Fashion Store Backend

Backend tối giản dùng Spring Boot 4.1.0, Java 21 và Maven. Project không có WebSocket.

## Cấu trúc

```text
src/main/java/com/fashionstore/backend/
├── configuration/
├── controller/
├── dto/
├── entity/
├── enums/
├── exception/
├── mail/
├── mapper/
├── repository/
├── scheduled/
├── service/
└── BackendApplication.java
```

`src/main/resources` chứa `static`, `templates`, `application.properties`, `application-local.properties` và `application-local.properties.example`.

## Bảo mật API

Tài khoản nhân viên được `ADMIN` tạo tại `POST /accounts`, gắn với hồ sơ nhân viên
đã có trong DB và có thể được cấp vai trò phù hợp. Nhân viên dùng tài khoản đó để
đăng nhập tại `POST /api/auth/login` (hoặc `/auth/login`); mật khẩu được so khớp bằng BCrypt. Tài khoản
không hoạt động hoặc thông tin đăng nhập sai sẽ bị từ chối. Đăng nhập thành công
trả về JWT HS512. Các API còn lại cần bearer token hợp lệ; Swagger UI và OpenAPI
được truy cập công khai. Decoder kiểm tra chữ ký, thời hạn token và trạng thái/vai
trò hiện tại của tài khoản trong DB.
CORS nhận danh sách origin từ `CORS_ALLOWED_ORIGINS`, mặc định là `http://localhost:5173`.

Các cấu hình JWT lấy qua hai thuộc tính `jwt.signerKey`, `jwt.valid-duration` và
`jwt.refreshable-duration`. Thời hạn được tính bằng giây. Đăng nhập trả `token` và
`refresh_token`; gửi refresh token đến `POST /api/auth/refresh_token` để cấp cặp token mới. Dùng access
token để gọi `GET /api/auth/me` lấy tài khoản hiện tại hoặc `PUT /api/auth/change-password` để
đổi mật khẩu; request đổi mật khẩu gồm `currentPassword` và `newPassword`.

## Chạy local

Yêu cầu JDK 21 và Microsoft SQL Server. Xem hướng dẫn chi tiết từng bước tại:
👉 [HUONG_DAN_CHAY_BACKEND.md](./HUONG_DAN_CHAY_BACKEND.md)

### Cách 1: Dùng file `application-local.properties` (Khuyên dùng)

1. Sao chép file cấu hình mẫu:
   ```powershell
   Copy-Item src\main\resources\application-local.properties.example src\main\resources\application-local.properties
   ```
2. Mở file `src/main/resources/application-local.properties` và sửa lại mật khẩu tài khoản `sa` của SQL Server máy bạn.
3. Chạy backend bằng Maven Wrapper trên PowerShell:
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

### Cách 2: Chạy bằng biến môi trường trên PowerShell

```powershell
cd backend
$env:SPRING_DATASOURCE_PASSWORD = "<mat-khau-sql-server>"
$env:JWT_SIGNER_KEY = "dbItA/Z1RTFExd5j5wIWbAYa+zKg5YODeg2uTInnVuL0x32XP96v3HDj8Gr+gR5uQzngh+xpXxrsdoOhDL5iFw=="
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

Ứng dụng chạy tại `http://localhost:8080`.
- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Tài khoản local có sẵn

Profile mặc định `local` tự khởi tạo 3 tài khoản demo khi backend chạy:

| Tên đăng nhập | Vai trò | Mật khẩu | Chi nhánh |
| --- | --- | --- | --- |
| `admin` | `ADMIN` | `123456` | Toàn chuỗi |
| `manager` | `MANAGER` | `123456` | Chi nhánh Q1 |
| `staff` | `STAFF` | `123456` | Chi nhánh Q1 |

Mật khẩu được lưu bằng BCrypt. Mỗi lần khởi động, 3 tài khoản trên được đồng bộ
mật khẩu, vai trò và trạng thái hoạt động; tài khoản đã tồn tại được dùng lại,
không tạo trùng hồ sơ nhân viên. Tài khoản mới được gắn với hồ sơ nhân viên riêng
và chi nhánh đang hoạt động đầu tiên; nếu chưa có thì tạo chi nhánh local.

Có thể đổi mật khẩu chung bằng `APP_BOOTSTRAP_ACCOUNTS_PASSWORD`, hoặc tắt khởi
tạo/đồng bộ bằng `APP_BOOTSTRAP_ACCOUNTS_ENABLED=false` để giữ các thay đổi tài khoản
qua những lần chạy lại. Chức năng này chỉ chạy trong profile `local`.

## Kiểm tra

```powershell
.\mvnw.cmd test
```

## API tồn kho

Tất cả endpoint dùng bearer access token và trả `ApiResponse`. Chi nhánh hiện tại
được lấy từ `TaiKhoan.MaNhanVien` → `NhanVien.MaChiNhanh` trong DB, không nhận
`branchId` từ request nhập/điều chỉnh kho. Nhân viên phải đang hoạt động.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/inventory` | Danh sách tồn kho chi nhánh hiện tại |
| GET | `/api/inventory/products/{productId}` | Tồn kho sản phẩm tại chi nhánh hiện tại |
| GET | `/api/inventory/branches/{branchId}` | Tồn kho chi nhánh được chỉ định |
| POST | `/api/inventory/import` | Cộng số lượng nhập, tạo bản ghi nếu chưa có |
| PATCH | `/api/inventory/{productId}` | Điều chỉnh số lượng tồn hoặc mức tồn tối thiểu |
| GET | `/api/inventory/low-stock` | Sản phẩm có `quantity <= minimumStock`, bao gồm hết hàng |

Các API danh sách nhận `page` (mặc định `0`) và `pageSize` (mặc định `20`, từ
`1` đến `100`), sắp theo `productId`. `result` có `content`, `pageNumber`,
`pageSize`, `totalElements`, `totalPages`, `last` như API sản phẩm.

`STAFF` được đọc kho chi nhánh của mình. `ADMIN` và `MANAGER` được đọc chi nhánh
khác và nhập/điều chỉnh kho chi nhánh hiện tại. Chi nhánh ngừng hoạt động vẫn có
thể xem nhưng không thể nhập/điều chỉnh. Không thay đổi quyền hoặc chi nhánh bằng
dữ liệu do client gửi lên.

Nhập kho (HTTP `200` cho cả nhập lần đầu và nhập thêm):

```http
POST /api/inventory/import
Content-Type: application/json
Authorization: Bearer <access-token>

{"productId":"SP00000001","quantity":10}
```

`quantity` bắt buộc là số nguyên dương. Sản phẩm phải tồn tại. Bản ghi mới có
`minimumStock = 5`, đúng mặc định trong schema; nhập tiếp giữ mức tối thiểu hiện có.

Điều chỉnh kho (HTTP `200`):

```http
PATCH /api/inventory/SP00000001
Content-Type: application/json
Authorization: Bearer <access-token>

{"quantity":25,"minimumStock":8}
```

Gửi một hoặc cả hai trường; trường bỏ qua hoặc `null` được giữ nguyên. Phải có ít
nhất một giá trị khác `null`, mỗi giá trị là số nguyên không âm. `quantity` ở
PATCH là số lượng tuyệt đối sau kiểm kê, còn POST import là số lượng cộng thêm.
PATCH không tạo bản ghi; dùng import trước nếu sản phẩm chưa có trong kho.

Một bản ghi trả về trong `result` (hoặc `result.content` với API danh sách):

```json
{
  "branchId": "CN01",
  "productId": "SP00000001",
  "quantity": 25,
  "minimumStock": 8,
  "updatedAt": "2026-10-22T12:00:00",
  "lowStock": false
}
```

Lỗi nghiệp vụ: `404/1014` sản phẩm không tồn tại, `404/1003` chi nhánh không tồn
tại, `404/1021` chưa có bản ghi tồn kho, `409/1022` tổng số lượng vượt giới hạn
SQL `INT`, `400/1023` PATCH rỗng, `409/1024` chi nhánh ngừng hoạt động,
`409/1025` xung đột giao dịch (có thể thử lại). Dữ liệu không hợp lệ trả `400`,
chưa đăng nhập trả `401`, thiếu quyền trả `403`.

Nhập và điều chỉnh dùng transaction `SERIALIZABLE` cùng khóa ghi để bảo vệ cập
nhật đồng thời, kể cả nhập lần đầu khi bản ghi chưa có. `updatedAt` được cập nhật
mỗi lần ghi. Việc gọi GET kiểm tra kho chưa giữ hàng; khi triển khai thanh toán,
cần kiểm tra và trừ tồn kho trong cùng transaction tạo hóa đơn.

Các truy vấn hiện chạy trên database đang kết nối. API theo `branchId` không tự
truy vấn Linked Server/site khác; nếu DB cục bộ không có tồn kho của chi nhánh đó
thì danh sách rỗng. Không tạo hoặc thay đổi bảng `dbo.TonKho`.


## API quản lý nhân viên

Tất cả endpoint quản lý nhân viên dùng bearer access token, phân quyền `ADMIN` và `MANAGER`.

| Method | Endpoint | Quyền | Chức năng |
| --- | --- | --- | --- |
| GET | `/api/employees` | `ADMIN`, `MANAGER` | Lấy danh sách nhân viên (hỗ trợ lọc `keyword`, `branchId`, `position`, `active`, phân trang). `MANAGER` chỉ xem được nhân viên chi nhánh mình. |
| GET | `/api/employees/{employeeId}` | `ADMIN`, `MANAGER` | Xem chi tiết nhân viên theo mã |
| POST | `/api/employees` | `ADMIN`, `MANAGER` | Thêm nhân viên mới (`MANAGER` chỉ tạo nhân viên cho chi nhánh mình) |
| PUT | `/api/employees/{employeeId}` | `ADMIN`, `MANAGER` | Cập nhật thông tin nhân viên (`MANAGER` không được đổi chi nhánh nhân viên) |
| PATCH | `/api/employees/{employeeId}/status` | `ADMIN`, `MANAGER` | Khóa hoặc mở trạng thái nhân viên (đồng bộ trạng thái tài khoản liên kết; không cho phép tự khóa chính mình) |

Đồng thời hỗ trợ ánh xạ đường dẫn tương thích ngược `/employees/**`.


## API quản lý tài khoản

Tất cả endpoint quản lý tài khoản dùng bearer access token.

| Method | Endpoint | Quyền | Chức năng |
| --- | --- | --- | --- |
| GET | `/api/accounts` | `ADMIN`, `MANAGER` | Lấy danh sách tài khoản nhân viên (hỗ trợ lọc `keyword`, `branchId`, `role`, `active`, phân trang). `MANAGER` chỉ xem được tài khoản chi nhánh mình. |
| GET | `/api/accounts/{accountId}` | `ADMIN`, `MANAGER` | Xem chi tiết tài khoản theo `accountId` (hoặc `username`). |
| POST | `/api/accounts` | `ADMIN`, `MANAGER` | Tạo tài khoản cho nhân viên (`MANAGER` chỉ tạo tài khoản vai trò `STAFF` cho nhân viên thuộc chi nhánh mình). |
| PATCH | `/api/accounts/{accountId}/role` | `ADMIN` | Thay đổi quyền của tài khoản (không được tự đổi quyền chính mình). |
| PATCH | `/api/accounts/{accountId}/status` | `ADMIN`, `MANAGER` | Khóa hoặc mở tài khoản (`MANAGER` chỉ khóa/mở tài khoản nhân viên chi nhánh mình; không cho phép tự khóa chính mình). |
| PATCH | `/api/accounts/{accountId}/reset-password` | `ADMIN`, `MANAGER` | Reset mật khẩu tài khoản (tự sinh mật khẩu tạm `FSxxxxxx` hoặc đặt theo request; trả về mật khẩu mới). |

Đồng thời hỗ trợ ánh xạ đường dẫn tương thích ngược `/accounts/**`.


## API chi nhánh

Tất cả endpoint chi nhánh dùng bearer access token. Hệ thống gồm các chi nhánh cố định (Q1, Q7, Thủ Đức).

| Method | Endpoint | Quyền | Chức năng |
| --- | --- | --- | --- |
| GET | `/api/branches` | `ADMIN`, `MANAGER`, `STAFF` | Lấy danh sách tất cả chi nhánh (hỗ trợ query param `active=true/false`). |
| GET | `/api/branches/{branchId}` | `ADMIN`, `MANAGER`, `STAFF` | Xem thông tin chi tiết một chi nhánh theo mã (`CN01`, `CN02`, `CN03`). |
| POST | `/api/branches` | `ADMIN` | Tạo chi nhánh mới (dự phòng). |

Đồng thời hỗ trợ ánh xạ đường dẫn tương thích ngược `/branches/**`.


## API Dashboard

Tất cả endpoint dashboard dùng bearer access token để tổng hợp số liệu thời gian thực từ hóa đơn, tồn kho, sản phẩm và chi nhánh.

| Method | Endpoint | Quyền | Chức năng |
| --- | --- | --- | --- |
| GET | `/api/dashboard/admin` | `ADMIN` | Lấy dữ liệu tổng quan toàn hệ thống cho Admin (doanh thu, hóa đơn, khách hàng, tồn kho, chuỗi thời gian, doanh thu chi nhánh, top sản phẩm, mặt hàng sắp hết, doanh số nhân viên; param `days`, mặc định 30). |
| GET | `/api/dashboard/manager` | `ADMIN`, `MANAGER` | Lấy dữ liệu tổng quan chi nhánh cho Manager (Manager xem chi nhánh mình; Admin có thể truyền `branchId`; param `days`, mặc định 30). |
| GET | `/api/dashboard/staff` | `ADMIN`, `MANAGER`, `STAFF` | Lấy dữ liệu làm việc cá nhân của Staff (doanh thu hôm nay, trong tháng, số hóa đơn đã lập và 5 hóa đơn gần nhất). |

Đồng thời hỗ trợ ánh xạ đường dẫn tương thích ngược `/dashboard/**`.


## API Thống kê

Tất cả endpoint thống kê dùng bearer access token để tổng hợp báo cáo kinh doanh theo kỳ và theo chi nhánh.

| Method | Endpoint | Quyền | Chức năng |
| --- | --- | --- | --- |
| GET | `/api/statistics/revenue` | `ADMIN`, `MANAGER` | Thống kê doanh thu theo khoảng thời gian (hỗ trợ params `branchId`, `days`, `from`, `to`). `MANAGER` chỉ xem chi nhánh mình. |
| GET | `/api/statistics/revenue/branches` | `ADMIN`, `MANAGER` | So sánh doanh thu và tỷ trọng giữa các chi nhánh (hỗ trợ params `days`, `from`, `to`). |
| GET | `/api/statistics/revenue/branches/{branchId}` | `ADMIN`, `MANAGER` | Xem doanh thu chi tiết của một chi nhánh theo mã. |
| GET | `/api/statistics/top-products` | `ADMIN`, `MANAGER`, `STAFF` | Thống kê sản phẩm bán chạy (hỗ trợ params `branchId`, `days`, `limit`, `sortBy=revenue/quantity`). |
| GET | `/api/statistics/employees` | `ADMIN`, `MANAGER` | Thống kê doanh số theo nhân viên (hỗ trợ params `branchId`, `days`, `limit`). |

Đồng thời hỗ trợ ánh xạ đường dẫn tương thích ngược `/statistics/**`.
