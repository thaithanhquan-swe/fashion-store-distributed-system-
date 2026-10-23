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

`src/main/resources` chứa `static`, `templates`, `application.properties` và `application-local.properties`.

## Bảo mật API

Tài khoản nhân viên được `ADMIN` tạo tại `POST /accounts`, gắn với hồ sơ nhân viên
đã có trong DB và có thể được cấp vai trò phù hợp. Nhân viên dùng tài khoản đó để
đăng nhập tại `POST /auth/login`; mật khẩu được so khớp bằng BCrypt. Tài khoản
không hoạt động hoặc thông tin đăng nhập sai sẽ bị từ chối. Đăng nhập thành công
trả về JWT HS512. Các API còn lại cần bearer token hợp lệ; Swagger UI và OpenAPI
được truy cập công khai. Decoder kiểm tra chữ ký, thời hạn token và trạng thái/vai
trò hiện tại của tài khoản trong DB.
CORS nhận danh sách origin từ `CORS_ALLOWED_ORIGINS`, mặc định là
`http://localhost:3000`.

Các cấu hình JWT lấy từ `application-local.properties` qua ba thuộc tính
`jwt.signerKey`, `jwt.valid-duration` và `jwt.refreshable-duration`. Thời hạn
được tính bằng giây. Đăng nhập trả `token` và `refresh_token`; gửi refresh token
đến `POST /auth/refresh_token` để cấp cặp token mới. Dùng access token để gọi
`GET /auth/me` lấy tài khoản hiện tại hoặc `PUT /auth/change-password` để đổi mật
khẩu; request đổi mật khẩu gồm `currentPassword` và `newPassword`.

## Chạy local

Yêu cầu JDK 21. Chạy bằng Maven Wrapper trên PowerShell:

```powershell
$env:APP_CONFIG_JWT_SIGNER_KEY = "<chuoi-bi-mat-it-nhat-64-byte>"
$env:APP_CONFIG_JWT_VALID_DURATION = "86400"
$env:APP_CONFIG_JWT_REFRESHABLE_DURATION = "86400"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000"
.\mvnw.cmd spring-boot:run
```

Ứng dụng chạy tại `http://localhost:8080`.

## Kiểm tra

```powershell
.\mvnw.cmd package
```

Khi bắt đầu làm từng chức năng, thêm dependency cần thiết vào `pom.xml`.
