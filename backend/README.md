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

## Dependency hiện tại

Chỉ giữ `spring-boot-starter-webmvc` để ứng dụng khởi động một web server.
Các dependency cho database, security, validation, Swagger và những chức năng
khác sẽ được thêm dần khi bắt đầu triển khai.

## Chạy local

Yêu cầu JDK 21. Chạy bằng Maven Wrapper trên PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Các package nghiệp vụ hiện được để trống để nhóm bổ sung class theo schema thật.
Ứng dụng chạy tại `http://localhost:8080`; hiện chưa có controller nên truy cập
đường dẫn gốc sẽ trả về `404`.

## Kiểm tra

```powershell
.\mvnw.cmd package
```

Khi bắt đầu làm từng chức năng, thêm dependency cần thiết vào `pom.xml`.
