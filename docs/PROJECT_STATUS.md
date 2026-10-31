# Trạng thái & kế hoạch tổng thể dự án

> **Đây là file nguồn sự thật duy nhất (single source of truth) của dự án.**
> Mọi phiên làm việc mới — dù là người hay AI — phải đọc file này trước khi code.
>
> - Cập nhật lần cuối: **2026-10-30** (sau khi pull tới `e2b0e3f`)
> - Commit đối chiếu: **`e2b0e3f`** (nhánh `develop`, đồng bộ `origin/develop`)
> - Working tree lúc cập nhật: `M README.md`, `M docs/setup.md`, `M docs/gap-analysis.md`, `M docs/architecture.md`, `M backend/README.md`, `M backend/HUONG_DAN_CHAY_BACKEND.md`, `M backend/AGENTS.md`, `M frontend/README.md`, `?? docs/PROJECT_STATUS.md`, `?? docs/setup.docx` (chưa commit — theo quy ước của nhóm)
> - Phạm vi: database, backend, frontend, phân tán, tài liệu, demo
> - Tài liệu tham chiếu gốc: `docs/gap-analysis.md` (phân tích lệch với mẫu giảng viên)
> - Mẫu giảng viên: `[6][4]TONG HOP TOM TAT CAI DAT CSDL PT - QUAN LY THUC THI GIAO DICH- SV.pdf`

---

## Mục lục

1. [Cách dùng file này](#1-cách-dùng-file-này)
2. [Yêu cầu bắt buộc của môn học](#2-yêu-cầu-bắt-buộc-của-môn-học)
3. [Tóm tắt tiến độ](#3-tóm-tắt-tiến-độ)
4. [Kiến trúc & bản đồ dự án](#4-kiến-trúc--bản-đồ-dự-án)
5. [Quy ước kỹ thuật & bẫy thường gặp](#5-quy-ước-kỹ-thuật--bẫy-thường-gặp)
6. [Đối chiếu chi tiết: yêu cầu → hiện trạng](#6-đối-chiếu-chi-tiết-yêu-cầu--hiện-trạng)
7. [Backlog còn thiếu, xếp theo thứ tự](#7-backlog-còn-thiếu-xếp-theo-thứ-tự)
8. [Kịch bản làm việc theo phiên](#8-kịch-bản-làm-việc-theo-phiên)
9. [Kiểm chứng & checklist demo](#9-kiểm-chứng--checklist-demo)
10. [Quyết định đã chốt & câu hỏi còn mở](#10-quyết-định-đã-chốt--câu-hỏi-còn-mở)
11. [Lịch sử cập nhật](#11-lịch-sử-cập-nhật)

---

## 1. Cách dùng file này

### 1.1. Đọc gì trước

| Thời điểm | Đọc mục |
| --- | --- |
| Mới vào dự án, cần nắm tổng quan | 2 → 3 → 4 |
| Chuẩn bị code | 5 (quy ước) → 7 (chọn việc) → 8 (phiên tương ứng) |
| Sau khi code xong | 6 (cập nhật trạng thái) → 9 (kiểm chứng) → 11 (ghi changelog) |
| Chuẩn bị demo | 2.4 (10 tiêu chí) → 9.3 (checklist demo) |
| Dựng hệ thống 4 máy | `docs/setup.md` (runbook) → mục 10 (quyết định còn mở) |

### 1.2. Quy tắc cập nhật

- **Chỉ sửa file này**, không tạo thêm file trạng thái khác. Nếu cần chi tiết, thêm mục con vào đây.
- Khi hoàn thành một việc: đổi `❌` → `✅` trong mục 6, tick `[x]` trong mục 7, thêm dòng vào mục 11.
- Khi phát hiện thông tin sai: sửa ngay, ghi rõ ngày và commit đối chiếu.
- Khi có quyết định kiến trúc mới: ghi vào mục 10, kèm lý do và ảnh hưởng.
- **Không đánh dấu ✅ nếu chưa kiểm chứng thật.** Ghi rõ “đã chạy lệnh gì, kết quả gì”.

### 1.3. Ký hiệu trạng thái

| Ký hiệu | Nghĩa |
| --- | --- |
| ✅ | Xong, đã kiểm chứng |
| 🟡 | Một phần, còn thiếu |
| 🔵 | Đang làm |
| ❌ | Chưa bắt đầu |
| ⛔ | Bị chặn bởi việc khác |

---

## 2. Yêu cầu bắt buộc của môn học

### 2.1. Yêu cầu chung cho mọi đề tài

Mỗi đề tài có **2–4 site**. Nhóm phải triển khai **tối thiểu**:

1. **Phân mảnh** dữ liệu.
2. **Replication** (nhân bản) dữ liệu dùng chung.
3. **Transaction** trong nghiệp vụ.
4. **Đồng bộ dữ liệu** giữa các site.

Đồng thời phải **chứng minh**:

- Tính **đầy đủ** (completeness): mọi bản ghi đều thuộc ít nhất một mảnh.
- Tính **tái thiết** (reconstruction): ghép các mảnh lại được quan hệ gốc.
- Tính **tách biệt** (disjointness): các mảnh không trùng bản ghi.
- Tính **trong suốt** (transparency): stored procedure viết một lần, chạy được ở mọi site mà không sửa đường dẫn.

Điểm cân bằng cần lưu ý: **không được chỉ làm database, cũng không được chỉ làm giao diện**. Phải có cả hai và chứng minh chúng nói chuyện với nhau.

### 2.2. Yêu cầu riêng – Đề tài 4: Quản lý chuỗi cửa hàng thời trang

- **3 cửa hàng**: Store A – Quận 1, Store B – Quận 7, Store C – Thủ Đức.
- **6 bảng chính**: `SanPham`, `ChiNhanh`, `TonKho`, `KhachHang`, `HoaDon`, `ChiTietHoaDon`.
- **Phân mảnh**:
  - Sản phẩm / tồn kho theo chi nhánh.
  - Hóa đơn theo chi nhánh.
  - `ChiTietHoaDon` phân mảnh dẫn xuất theo `HoaDon`.
- **Replication**: `KhachHang` giữa các site.
- **Nghiệp vụ**: Bán hàng → cập nhật tồn kho → đồng bộ dữ liệu.

### 2.3. Sản phẩm phải nộp theo mẫu giảng viên

| Nhóm | Nội dung bắt buộc | Có trong dự án? |
| --- | --- | --- |
| 1 | Điều kiện môi trường: SQL Server Agent, ≥ 3 server, không dùng Express, firewall, TCP/IP 1433, mixed auth, quyền account liên kết | 🟡 |
| 2 | Publication / Subscription: chỉ rõ Publisher, Distributor, Subscriber | ❌ |
| 3 | Hình thức phân mảnh: ngang, dọc, hỗn hợp; ngang nguyên thủy & ngang dẫn xuất | 🟡 |
| 4 | Ba quy tắc phân mảnh: đầy đủ, tái thiết, tách biệt | ❌ |
| 5 | Linked Server: `sp_addlinkedserver` + `sp_addlinkedsrvlogin`, kiểm tra bằng `SELECT` xuyên server | ❌ |
| 6 | Quy tắc trong suốt: tên CSDL giống nhau, tên Linked Server giống nhau | ❌ |
| 7 | Stored Procedure `SELECT … UNION` gom dữ liệu từ các site | ❌ |
| 8 | SP tìm / sửa / xóa xuyên server bằng chuỗi `IF EXISTS` | ❌ |
| 9 | Trigger đồng bộ ở mỗi site + trigger kiểm soát update/delete | ❌ |
| 10 | Tài liệu thiết kế: 7.1 → 7.5, 3.5 → 3.8 | 🟡 |
| 11 | Chức năng nghiệp vụ & giao diện: khách hàng, nhân viên, hóa đơn, tìm kiếm, thống kê, báo cáo, phân quyền, in hóa đơn | 🟡 |

Các mục tài liệu chi tiết:

- **7.1** Phân tích thực thể: bảng STT / Tên thuộc tính / Kiểu / Ràng buộc / Khóa / Mô tả.
- **7.2** Lược đồ liên kết thực thể (ERD).
- **7.3** Lược đồ quan hệ liên kết.
- **7.4** Quan hệ giữa các bảng.
- **7.5** Thiết kế phân mảnh ngang & nhân bản: bảng **chung** chỉ sửa ở máy chủ rồi nhân bản xuống site; bảng **riêng** do từng site quản lý.
- **3.5** Sơ đồ định vị: cột “Trụ sở chính / Chi nhánh”, ký hiệu **W** (ghi), **E** (sửa), **D** (xóa), **R** (đọc), **H** (tần suất cao), **L** (tần suất thấp).
- **3.6** Thiết kế nhân bản, đồng bộ hóa.
- **3.7** Thiết kế vật lý các bảng tại các trạm.
- **3.8** Cài đặt CSDL, tạo liên kết giữa các bảng.
- **Mục 1** Kịch bản.
- **Mục 2.2** Phân tích chức năng tại đại lý tổng và từng đại lý.
- **Mục 3** Chức năng ở máy trạm.
- **Mục 4** Chức năng ở máy chủ.
- **Mục 5** Phân quyền theo nhóm đối tượng.
- **Mục 6** Thiết kế mạng tổng quan.

### 2.4. 10 tiêu chí demo giảng viên chấm

| # | Tiêu chí | Trạng thái hiện tại |
| --- | --- | --- |
| 1 | 4 máy kết nối với nhau | ❌ |
| 2 | Central truy cập được 3 site | ❌ |
| 3 | Tồn kho phân mảnh theo chi nhánh | 🟡 schema đúng, chưa chứng minh |
| 4 | Hóa đơn phân mảnh theo chi nhánh | 🟡 schema đúng, chưa chứng minh |
| 5 | `ChiTietHoaDon` đi theo `HoaDon` | 🟡 schema đúng, chưa chứng minh |
| 6 | `KhachHang` replication giữa các site | ❌ |
| 7 | Bán hàng tại một site | 🟡 backend chạy local, frontend còn mock |
| 8 | Sau bán hàng tồn kho tại site đó giảm | 🟡 backend có transaction, chưa demo end-to-end |
| 9 | Central xem được dữ liệu toàn hệ thống | ❌ |
| 10 | Khi mất Central, site vẫn thực hiện nghiệp vụ cục bộ | ❌ |

---

## 3. Tóm tắt tiến độ

### 3.1. Dashboard

| Mảng | Ước lượng | Trạng thái | Bằng chứng chính |
| --- | ---: | :---: | --- |
| Điều kiện môi trường | 70% | 🟡 | `docs/setup.md` đã có đủ quy trình (Agent, edition, mixed auth, snapshot, firewall); chưa chạy thật trên 4 máy |
| Thiết kế phân mảnh | 55% | 🟡 | Schema 4 DB đúng hướng; chưa có sơ đồ, chưa chứng minh 3 quy tắc |
| Replication | 0% | ❌ | Không có script publication/subscription |
| Linked Server | 20% | 🟡 | `docs/setup.md` có script `sp_addlinkedserver`; chưa chạy; SP ở Central dùng tên server sai quy ước |
| Stored Procedure phân tán | 10% | 🟡 | Có `sp_DongBoSanPham` trong `Fashion_Central.sql`; chỉ đồng bộ 1 cột, chưa chạy được |
| Trigger | 0% | ❌ | Chưa có trigger nào |
| Transaction nghiệp vụ | 70% | 🟡 | `SalesService` dùng `SERIALIZABLE` + khóa tồn; thiếu idempotency |
| Đồng bộ dữ liệu | 0% | ❌ | Chưa có queue/trigger/replication |
| Database schema | 65% | 🟡 | 4 file (đã có Central), 8 bảng mỗi file; thiếu seed, định danh toàn cục, UTF-8 |
| Backend | 90% | 🟡 | 12 controller, 50 endpoint, 73 test pass; thiếu API phân tán + cấu hình theo máy |
| Frontend – giao diện | 100% | ✅ | 30 file trang, phủ 27 route menu + 7 route chi tiết (cấu trúc trang; chất lượng từng trạng thái xem F03) |
| Frontend – nối API | 85% | 🟡 | 10/11 service gọi API thật; còn `siteService` mock; mã chi nhánh lệch DB |
| Tài liệu thiết kế | 50% | 🟡 | Có kiến trúc, setup 4 máy, gap-analysis, PROJECT_STATUS; thiếu ERD, phân mảnh, định vị, kịch bản |
| Sẵn sàng demo | 35% | 🟡 | FE gọi API thật nhưng DB chưa có seed, chưa có phân tán |

**Tự đánh giá tổng: khoảng 50–55% nếu tính cả phần phân tán.**
Nếu chỉ tính phạm vi database + backend + frontend: **khoảng 85–88%**.

> Đây là tự đánh giá theo bằng chứng trong repo, không phải điểm giảng viên chấm.

### 3.2. Ba việc quan trọng nhất lúc này

1. **Chốt mã chi nhánh + tên CSDL + cách cấu hình backend** — ba thứ này đang lệch giữa FE, BE và DB, chặn cả demo lẫn phân tán.
2. **Seed dữ liệu 3 chi nhánh** — DB trống, backend tự tạo `Local Demo Branch` (`CN001`), nên Q7/Thủ Đức chưa có dữ liệu để chứng minh nghiệp vụ.
3. **Làm phần phân tán** (replication, linked server, SP, trigger) — trọng tâm môn học, vẫn gần 0%.

---

## 4. Kiến trúc & bản đồ dự án

### 4.1. Mô hình 4 máy (theo README)

```text
                         MÁY 1 – CENTRAL
                       Fashion_Central
                              │
                         Radmin VPN
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
   MÁY 2 – Q1           MÁY 3 – Q7         MÁY 4 – THỦ ĐỨC
   Fashion_Q1           Fashion_Q7         Fashion_ThuDuc
   CN01                 CN02               CN03
```

- Site không bắt buộc kết nối trực tiếp với nhau; Central là trung tâm.
- Central truy vấn các site qua Linked Server.
- Mỗi site có Linked Server `CENTRAL`.
- Tên Linked Server quy ước theo **đích**: `SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`, `CENTRAL`.

### 4.2. Phân bố dữ liệu – mục tiêu

| Bảng | Loại | Ghi ở đâu | Đọc ở đâu | Ghi chú |
| --- | --- | --- | --- | --- |
| `SanPham` | Chung | Central | Tất cả | Central sửa, nhân bản xuống site |
| `ChiNhanh` | Chung | Central | Tất cả | Danh mục dùng chung |
| `KhachHang` | Riêng + đồng bộ | Site | Tất cả | Tạo tại site, đồng bộ lên Central rồi xuống site khác |
| `NhanVien` | Riêng | Site | Site + Central | Đồng bộ lên Central |
| `TonKho` | Riêng (ngang) | Site sở hữu | Site + Central | Phân mảnh theo `MaChiNhanh` |
| `HoaDon` | Riêng (ngang) | Site sở hữu | Site + Central | Phân mảnh theo `MaChiNhanh` |
| `ChiTietHoaDon` | Riêng (dẫn xuất) | Site của hóa đơn | Site + Central | Đi theo `HoaDon` |
| `TaiKhoan` | Chỉ Central (đề xuất) | Central | Central | Hiện là bảng local ở cả 3 site |

> Bảng phân bố này là **đề xuất**, chưa được nhóm chốt chính thức. Xem mục 10.

### 4.3. Cây thư mục thực tế

```text
fashion-store-distributed-system/
├── README.md                     Mô tả đề tài, kiến trúc, hướng dẫn môi trường
├── database/
│   ├── Fashion_Central.sql       Tạo DB Central + 8 bảng + distributed_user + sp_DongBoSanPham
│   ├── Fashion_Q1.sql            Tạo DB + 8 bảng cho Q1 (UTF-16 LE BOM)
│   ├── Fashion_Q7.sql            Tạo DB + 8 bảng cho Q7 (thiếu distributed_user)
│   └── Fashion_TD.sql            Tạo DB + 8 bảng cho Thủ Đức (tên DB sai)
├── backend/
│   ├── AGENTS.md                 Quy ước code backend
│   ├── HUONG_DAN_CHAY_BACKEND.md Hướng dẫn chạy backend (còn nói sửa application.properties)
│   ├── README.md                 Thiết kế backend
│   ├── pom.xml                   Java 21, Spring Boot 4.x, JPA, Security, MapStruct
│   └── src/
│       ├── main/java/com/fashionstore/backend/
│       │   ├── configuration/    Security, JWT, CORS, bootstrap tài khoản
│       │   ├── controller/       12 controller, 50 endpoint
│       │   ├── dto/              Request / response / PageResponse / ApiResponse
│       │   ├── entity/           8 entity + 2 khóa phức hợp: Account, Branch,
│       │   │                     Customer, Employee, Inventory, Invoice,
│       │   │                     InvoiceDetail, Product
│       │   ├── enums/            Role
│       │   ├── exception/        AppException, ErrorCode, GlobalExceptionHandler
│       │   ├── mapper/           MapStruct
│       │   ├── repository/       Spring Data JPA
│       │   ├── service/          Nghiệp vụ + GeneratedId
│       │   └── specification/    Bộ lọc tìm kiếm
│       ├── main/resources/
│       │   ├── application-local.properties   Cấu hình mẫu (password/JWT để trống)
│       │   └── application.properties          Bị gitignore, không có trong repo
│       └── test/java/            11 file test, 73 @Test
├── frontend/
│   ├── AGENTS.md                 Quy ước bắt buộc cho agent/dev frontend
│   ├── README.md                 Quy ước frontend, phân công, màu, route
│   ├── src/
│   │   ├── api/                  client.js (fetch + token + refresh), authApi.js
│   │   ├── components/layout/    Sidebar, Topbar, AppLayout
│   │   ├── components/ui/        Bộ component dùng chung (Khang phụ trách)
│   │   ├── config/               nav.js, branches.js, env.js
│   │   ├── context/              AuthContext
│   │   ├── hooks/                useAuth, useAsyncData
│   │   ├── pages/                30 file .jsx (trang + modal)
│   │   ├── routes/               RoleGuard
│   │   ├── services/             11 service + mockData + mockStore
│   │   └── utils/format.js       formatMoney, formatDate, nhãn trạng thái
│   └── .env.example              VITE_API_URL=http://localhost:8080
└── docs/
    ├── architecture.md           Kiến trúc (đã cập nhật 2026-10-29)
    ├── setup.md                  Runbook triển khai 4 máy (đã viết lại 2026-10-29)
    ├── setup.docx                Bản Word của setup.md (sinh tự động, 19 trang)
    ├── gap-analysis.md           Đối chiếu mẫu giảng viên (đã cập nhật 2026-10-29)
    └── PROJECT_STATUS.md         File này
```

### 4.4. Luồng nghiệp vụ chính

```text
Nhân viên đăng nhập
    ↓
Chọn sản phẩm tại POS
    ↓
Kiểm tra tồn kho chi nhánh (TonKho WHERE MaChiNhanh = site)
    ↓
Đủ hàng?
  ├── Không → báo hết hàng
  └── Có
        ↓
   Mở transaction SERIALIZABLE
        ↓
   Khóa dòng tồn kho (PESSIMISTIC_WRITE)
        ↓
   Tạo HoaDon + ChiTietHoaDon
        ↓
   Trừ TonKho
        ↓
   Cộng điểm khách hàng
        ↓
   Commit
        ↓
   Trigger / replication đồng bộ lên Central
        ↓
   Central tổng hợp báo cáo toàn chuỗi
```

### 4.5. Trạng thái thực tế so với mục tiêu

| Hạng mục | Mục tiêu | Thực tế |
| --- | --- | --- |
| Tên CSDL 3 site | Giống nhau (quy tắc trong suốt) | `Fashion_Q1`, `Fashion_Q7`, `Fashion_TD` |
| Tên CSDL Thủ Đức | `Fashion_ThuDuc` | `Fashion_TD` |
| Central | Có `Fashion_Central` + linked server | ✅ Có `Fashion_Central.sql` (8 bảng); ❌ chưa có script linked server |
| `distributed_user` | Có ở cả 3 site | 🟡 Q1 + Central; Q7/TD thiếu |
| Dữ liệu mẫu | Có seed theo kịch bản | 0 câu `INSERT` |
| Encoding SQL | UTF-8 | UTF-16 LE BOM |
| Mã chi nhánh | `CN01` / `CN02` / `CN03` (Quận 1 / Quận 7 / Thủ Đức) | FE hardcode `CN001` / `CN002` / `CN003`, tên "Quận 3"; backend nhận cả 3 dạng alias; DB trống nên bootstrap tạo `CN001` |
| Backend kết nối | Mỗi máy trỏ DB của mình | `application-local.properties` trỏ `Fashion_Q1`; `application.properties` bị gitignore và không có trong repo |
| Frontend dữ liệu | Gọi API thật | 10/11 service gọi API thật; `siteService` còn mock |
| Phân tán | Replication + linked server + trigger | Chưa có |

---

## 5. Quy ước kỹ thuật & bẫy thường gặp

### 5.1. Backend

- Java 21, Spring Boot 4.x, package gốc `com.fashionstore.backend`.
- `controller` → `service` → `repository` → `entity`; DTO không lộ entity.
- MapStruct cho mapping; Lombok theo style hiện có.
- `ddl-auto=none`; **không tự tạo/sửa bảng** khi làm feature thường.
- Mọi API trả `ApiResponse<T>` bọc `{ code, message, result }`.
- Danh sách trả `PageResponse` với `content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `last`.
- Phân trang backend bắt đầu từ **0**; frontend bắt đầu từ **1** → phải trừ 1 khi gọi.
- Lỗi nghiệp vụ dùng `AppException` + `ErrorCode`, không tự xử lý trong controller.
- Không in credential ra log/response.
- Chạy kiểm tra: `cd backend && ./mvnw.cmd -DskipTests compile` và `./mvnw.cmd test`.

### 5.2. Frontend

- Trang **không gọi `fetch` trực tiếp**; chỉ gọi hàm trong `src/services/`.
- Khi backend chưa có API, service trả mock; khi có API chỉ sửa thân hàm, **không sửa trang**.
- Mọi bảng/danh sách phải có đủ **đang tải / rỗng / lỗi**.
- Văn bản giao diện tiếng Việt, dữ liệu mẫu cũng tiếng Việt.
- Chỉ dùng tên màu trong `index.css` (`bg`, `card`, `ink`, `accent`, `ok`, `warn`, `bad`), **không viết mã màu rời**.
- Không sửa `components/ui` (Khang phụ trách); cần component mới thì báo.
- Không thêm thư viện khi chưa báo nhóm.
- Mỗi trang một file, tên kết thúc bằng `Page.jsx`.
- Route khai báo trong `config/nav.js` + `App.jsx` (`PAGES` hoặc `EXTRA_PAGES`).
- Chạy kiểm tra: `cd frontend && npm run lint && npm run build`.

### 5.3. Vai trò & phạm vi dữ liệu

| Role backend | Role frontend | Phạm vi |
| --- | --- | --- |
| `ADMIN` | `admin` | Toàn chuỗi |
| `MANAGER` | `manager` | Chi nhánh của mình |
| `STAFF` | `staff` | Chi nhánh của mình |

- Backend trả role **chữ hoa**; `authService` đã hạ về chữ thường.
- `LoginResponse` đã có `branchId`; `authService` đã lưu vào user.
- `RoleGuard` so sánh trực tiếp role chữ thường.

### 5.4. Bẫy đã gặp và cách xử lý

| Bẫy | Cách xử lý hiện tại |
| --- | --- |
| Windows scrollbar sáng trên sidebar tối | Class `.sidebar-scroll` trong `index.css`, gắn vào `<nav>` của `Sidebar.jsx` |
| `react-hooks/set-state-in-effect` chặn `setState` trong effect | `useAsyncData` suy luận `loading` từ `key`, chỉ setState trong callback promise |
| Lệch ngày do `toISOString()` chuyển UTC | Dùng `dayKey()` theo giờ địa phương |
| Mock đổi số mỗi lần reload | `mockData.js` dùng bộ sinh số tất định |
| Nhập kho mất khi F5 | `mockStore.js` lưu vào `localStorage` |
| PowerShell báo exit 1 dù Maven BUILD SUCCESS | Do warning Mockito ghi ra stderr; đọc dòng `BUILD SUCCESS` thay vì exit code |
| File SQL khó diff | Đang là UTF-16 LE BOM; cần chuyển UTF-8 + `.gitattributes` |
| Clone mới không start được backend | `application.properties` bị gitignore và **không có trong repo**; chỉ có `application-local.properties` mà Spring Boot chỉ đọc khi bật profile `local`. Cách chạy tạm: copy `application-local.properties` → `application.properties`, hoặc đặt `SPRING_PROFILES_ACTIVE=local` + biến môi trường `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SIGNER_KEY` |
| Mã chi nhánh lệch giữa FE / BE / DB | FE hardcode `CN001/CN002/CN003` và "Store Quận 3"; đề bài + `AGENTS.md` + `docs/setup.md` dùng `CN01/CN02/CN03` (Quận 1 / Quận 7 / Thủ Đức). `BranchService` + `InventoryService` có alias `CN1`/`CN01`/`CN001`, nhưng `StatisticsService`/`DashboardService` dùng `branchId` thô → lọc sai nếu DB lưu mã khác |
| `sp_DongBoSanPham` không chạy được | Dùng tên 3 phần `Fashion_Q1.dbo.SanPham` → đòi linked server tên **`Fashion_Q1`**, lệch quy ước `SITE_Q1` trong `docs/setup.md`; và chỉ đồng bộ cột `TenSanPham` |
| Bootstrap tạo chi nhánh rác | DB trống nên `LocalAccountBootstrapService` tự tạo `Local Demo Branch` (`CN001`) thay vì 3 chi nhánh thật |

### 5.5. Trạng thái tài liệu sau khi đồng bộ (2026-10-29)

| Tài liệu | Trước đây | Hiện tại |
| --- | --- | --- |
| `README.md` mục “Bộ khung” | Nói chưa có schema/backend, frontend mới có tài liệu | ✅ Đã ghi đúng: 4 schema, 50 endpoint, 30 trang, phân tán 0% |
| `README.md` mục 12–13 | Liệt kê `central.sql`, `q1.sql`, `common.sql` | ✅ Đã đổi sang `Fashion_*.sql` + mô tả đúng nội dung từng file |
| `README.md` mục 30 | Nói “đã có database và dữ liệu sơ bộ” | ✅ Đã ghi rõ 0 câu `INSERT`, chưa có linked server/replication |
| `docs/architecture.md` | Nói SQL chỉ kiểm tra kết nối, backend/replication chưa cài | ✅ Đã viết lại: hiện trạng + mục tiêu, bảng file đúng |
| `docs/setup.md` | Hướng dẫn file không tồn tại | ✅ Runbook 4 máy; thêm mục 3.4 cấu hình backend, 0.3 mã chi nhánh |
| `docs/gap-analysis.md` | Nói thiếu entity tồn kho, API thống kê, `branchId`, FE mock | ✅ Đã cập nhật đầu file + mục 5; giữ làm lịch sử |
| `backend/HUONG_DAN_CHAY_BACKEND.md` | Hướng dẫn sửa `application.properties` (file không còn) | ✅ Đã đổi sang copy `application-local.properties` hoặc profile `local` |
| `backend/README.md` | Cùng lỗi trên; nói profile mặc định `local` | ✅ Đã sửa; ghi rõ bootstrap chạy mọi profile |
| `frontend/README.md` | Nói gộp vào `main`, service còn mock | ✅ Đã đổi sang `develop`, ghi 10/11 service gọi API thật |
| `backend/AGENTS.md` | Nói database là `Fashion_Q1` | ✅ Đã ghi rõ đây là cấu hình mẫu cho máy Q1; máy khác override URL |

---

## 6. Đối chiếu chi tiết: yêu cầu → hiện trạng

### 6.1. Database (SQL Server - đã hoàn thành các file, chưa chạy thử)

| ID | Yêu cầu | Trạng thái | Bằng chứng / việc cần làm |
| --- | --- | --- | --- |
| D01 | 3 site có schema 8 bảng | ✅ | `database/sites/Fashion_Store.sql` (chạy cho cả 3 site) |
| D02 | Central có schema riêng | ✅ | `database/Fashion_Central.sql` (8 bảng + `distributed_user` + `sp_DongBoSanPham`) |
| D03 | `common.sql` chứa schema/SP dùng chung | ✅ | `database/common.sql` — 8 bảng, FK, indexes, SP/trigger templates |
| D04 | Dữ liệu mẫu theo kịch bản demo | ✅ | `database/seed.sql` đã mở rộng lên **100 sản phẩm** (`SP001` - `SP100`), **300 dòng tồn kho** (đủ cả 3 chi nhánh CN1, CN2, CN3), 15 khách hàng có tiền tố site, 10 nhân viên & tài khoản demo |
| D05 | Tên CSDL 3 site giống nhau | ✅ | `database/sites/Fashion_Store.sql` tạo DB `Fashion_Store` cho tất cả 3 site (sử dụng sqlcmd variables: `$(SiteBranch)`, `$(SiteLinkedServerName)`, ...) |
| D06 | Tên CSDL Thủ Đức đúng | ✅ | Mọi site đều dùng `Fashion_Store` (không còn `Fashion_TD`) |
| D07 | `distributed_user` ở cả 3 site | 🟡 | Đã có sẵn trong script `sites/Fashion_Store.sql` và `Fashion_Central.sql`; cần thực thi khi dựng 4 máy |
| D08 | `MaKhachHang` định danh toàn cục | ✅ | `database/seed.sql` dùng định danh site-prefixed: `Q1-KH001`, `Q7-KH001`, `TD-KH001` |
| D09 | `MaHoaDon` định danh toàn cục | ✅ | `database/seed.sql` dùng `Q1-HD000001`, `Q7-HD000001`, `TD-HD000001` (giữ hậu tố `HD` + số thứ tự) |
| D10 | Bỏ `UNIQUE(SoDienThoai)` trên `KhachHang` | ✅ | Đã xóa constraint UNIQUE trong `common.sql`, thay bằng `IX_KhachHang_SoDienThoai` để tránh xung đột khi replicate/sync giữa các site |
| D11 | Cột `LastModified` / version để giải quyết xung đột | ✅ | Đã thêm `[LastModified] [datetime2](3) NOT NULL DEFAULT SYSUTCDATETIME()` vào `SanPham`, `KhachHang`, `NhanVien`, `HoaDon`, `TonKho` và hỗ trợ luật Last-In-Wins trong MERGE SPs (`central-procs.sql`, `site-procs.sql`) |
| D12 | Script idempotent (chạy lại không lỗi) | 🟡 | `Fashion_Central.sql` & `Fashion_Store.sql` có `DROP IF EXISTS`; `linked-servers.sql` có `sp_dropserver`; `common.sql` cần thêm check table exists nếu chạy độc lập |
| D13 | File SQL UTF-8 + `.gitattributes` | ✅ | Đã tạo `.gitattributes`; toàn bộ các script SQL mới đều lưu chuẩn UTF-8 (chỉ còn 3 file cũ Q1/Q7/TD là legacy) |
| D14 | Thiết kế vật lý bảng tại từng trạm | ❌ | Chưa có tài liệu chi tiết (filegroup, datafile, logfile) |
| D15 | Script linked server cho Central | ✅ | Đã có `database/linked-servers.sql` cấu hình cả Central và Site |
| D16 | SP phân tán dùng đúng tên linked server | ✅ | Đã có `database/central-procs.sql` và `database/sites/site-procs.sql` dùng đúng `SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`, `CENTRAL` |

### 6.2. Backend (Spring Boot)

| ID | Yêu cầu | Trạng thái | Bằng chứng / việc cần làm |
| --- | --- | --- | --- |
| B01 | Đăng nhập JWT + refresh token | ✅ | `AuthenticationController`, `JwtAuthenticationConfig` |
| B02 | Phân quyền theo role | ✅ | `@PreAuthorize`, `SecurityConfig` |
| B03 | CRUD nhân viên / tài khoản / chi nhánh / sản phẩm / khách hàng | ✅ | 5 controller + service + test |
| B04 | API tồn kho: xem, nhập, điều chỉnh, sắp hết | ✅ | `InventoryController` 6 endpoint |
| B05 | Bán hàng có transaction, chống tồn âm | ✅ | `SalesService` dùng `SERIALIZABLE` + `findForUpdate` |
| B06 | Hóa đơn: danh sách, chi tiết, hủy | ✅ | `InvoiceController` |
| B07 | Dashboard 3 role | ✅ | `/api/dashboard/admin`, `/manager`, `/staff` |
| B08 | Thống kê doanh thu, chi nhánh, top SP, nhân viên | ✅ | `StatisticsController` 5 endpoint |
| B09 | `branchId` trong response đăng nhập | ✅ | `LoginResponse.branchId` |
| B10 | Cấu hình theo từng máy (Central / site) | 🟡 | Có `application-local.properties` (mẫu trỏ `Fashion_Q1`) nhưng `application.properties` bị gitignore và không có trong repo; chưa có profile `central`/`site` |
| B11 | API tồn kho toàn chuỗi cho Admin | ❌ | Chỉ có `/api/inventory` (chi nhánh hiện tại) và `/branches/{id}` |
| B12 | API trạng thái site (ping linked server) | ❌ | Trang `/admin/sites` đang dùng mock |
| B13 | Tổng hợp dữ liệu toàn chuỗi ở Central | ❌ | Statistics/Dashboard đọc repository cục bộ, không qua linked server |
| B14 | Idempotency khi gửi lại checkout | ❌ | `CheckoutRequest` không có `requestId`; mỗi lần gọi tạo hóa đơn mới |
| B15 | Response tồn kho đủ thông tin cho FE | 🟡 | `InventoryResponse` thiếu `productName`, `category`, `brand`, `price`; FE phải ghép từ `productService` |
| B16 | Test nghiệp vụ tồn kho / bán hàng | ❌ | 73 test nhưng chưa có test cho inventory, sales, product, customer, auth |
| B17 | Swagger/OpenAPI | ✅ | springdoc, `/swagger-ui/index.html` |
| B18 | Mã hóa đơn có tiền tố site | ❌ | Đang dùng UUID, chưa theo quy ước site |
| B19 | Bootstrap tài khoản chạy mọi môi trường | ✅ | Bỏ `@Profile("local")`; `app.bootstrap-accounts.enabled` mặc định `true`; tạo đủ tài khoản theo `Role` + `manager.q1`/`staff.q1` (mật khẩu `123456`) |
| B20 | Nhận alias mã chi nhánh | 🟡 | `BranchService` + `InventoryService` nhận `CN1`/`CN01`/`CN001`; `StatisticsService`/`DashboardService` vẫn so khớp thô |
| B21 | API bán hàng dùng cho POS | ✅ | `SalesController` `POST /api/sales/checkout`, `CheckoutModal` đã gọi |

### 6.3. Frontend

| ID | Yêu cầu | Trạng thái | Bằng chứng / việc cần làm |
| --- | --- | --- | --- |
| F01 | Đủ trang theo menu 3 role | ✅ | 27/27 route trong `nav.js` có trong `PAGES` |
| F02 | Trang chi tiết | ✅ | 7 route trong `EXTRA_PAGES` |
| F03 | Đủ trạng thái tải / rỗng / lỗi | 🟡 | Phần lớn dùng `useAsyncData` + `DataTable` + `EmptyState` + `Spinner`; cần rà lại từng trang |
| F04 | Staff/Manager chỉ thấy chi nhánh mình | 🟡 | Logic UI truyền `user.branchId`; backend đã lọc theo token; DB trống nên chưa chứng minh bằng dữ liệu thật |
| F05 | Đăng nhập gọi API thật | ✅ | `authService` dùng `authApi`; lưu token + refresh token |
| F06 | 10 service còn lại gọi API thật | ✅ | 10/11 đã gọi (`account`, `branch`, `customer`, `dashboard`, `employee`, `inventory`, `invoice`, `product`, `statistics`); còn `siteService` |
| F07 | Trang trạng thái site dùng dữ liệu thật | ❌ | `siteService` mock hoàn toàn (chờ B12) |
| F08 | Adapter khớp shape API | ✅ | Mỗi service có hàm `to*` đổi shape BE → FE; `getAllPages()` gom phân trang 0-based |
| F09 | Lint + build sạch | ✅ | `npm run lint`, `npm run build` (439 kB) |
| F10 | Không gọi `fetch` trong page | ✅ | Đã quét, không có; page chỉ gọi service |
| F11 | Không sửa `components/ui` | ✅ | Chỉ dùng component có sẵn |
| F12 | Sidebar cuộn đẹp | ✅ | `.sidebar-scroll` |
| F13 | Client tự refresh token khi 401 | ✅ | `api/client.js`: gắn Bearer, refresh 1 lần, phát `SESSION_EXPIRED_EVENT`; `AuthContext` đăng xuất |
| F14 | Mã chi nhánh hiển thị đúng | ✅ | `config/branches.js` cập nhật `CN1` (Quận 1), `CN2` (Quận 7), `CN3` (Thủ Đức) — khớp PDF filter MACN='CN1' |

### 6.4. Phân tán

| ID | Yêu cầu | Trạng thái | Việc cần làm |
| --- | --- | --- | --- |
| X01 | SQL Server Agent chạy, snapshot folder | ❌ | Bật Agent, tạo `\\CENTRAL\REPLDATA`, quyền Everyone Read/Write |
| X02 | Configure Distribution | ❌ | Chạy wizard trên Central |
| X03 | Publication | ❌ | Tạo publication cho bảng chung + bảng riêng |
| X04 | Add Filter + Add Join | ❌ | Filter `MaChiNhanh`, join `TonKho` → `HoaDon` → `ChiTietHoaDon` |
| X05 | Subscription 3 site | ❌ | DB đích cùng tên |
| X06 | Linked Server 4 chiều | ❌ | `sp_addlinkedserver` + `sp_addlinkedsrvlogin` |
| X07 | SP `UNION` tồn kho / doanh thu | ❌ | `usp_TonKhoToanHeThong`, `usp_TongHopDoanhThu` |
| X08 | SP tìm / sửa / xóa xuyên site | ❌ | `usp_TimHoaDon`, `usp_TimKhachHang`, `usp_UpdateKhachHang`, `usp_DeleteKhachHang` |
| X09 | Trigger đồng bộ site → Central | ❌ | Kèm bảng queue `SyncQueue` |
| X10 | Trigger kiểm soát update/delete | ❌ | Chặn xóa `SanPham` đang có trong `TonKho`/`ChiTietHoaDon` |
| X11 | Đồng bộ khi mất mạng | ❌ | Queue bền vững + retry + chống trùng |
| X12 | Quy tắc giải quyết xung đột | ❌ | `LastModified` + quy tắc thắng/thua |
| X13 | Kiểm thử mất Central | ❌ | Ngắt link, site vẫn bán, nối lại đồng bộ |

### 6.5. Tài liệu

| ID | Tài liệu | Trạng thái | Ghi chú |
| --- | --- | --- | --- |
| T01 | Kịch bản demo | ❌ | `docs/kich-ban.md` |
| T02 | ERD 7.2 | ❌ | `docs/erd.md` |
| T03 | Lược đồ quan hệ 7.3 / 7.4 | ❌ | Trong `docs/erd.md` |
| T04 | Thiết kế phân mảnh 7.5 + 3 quy tắc | ❌ | `docs/phan-manh.md` |
| T05 | Sơ đồ định vị 3.5 (W/E/D/R/H/L) | ❌ | `docs/dinh-vi.md` |
| T06 | Nhân bản, đồng bộ 3.6 | ❌ | `docs/nhan-ban-dong-bo.md` |
| T07 | Thiết kế vật lý 3.7 / 3.8 | ❌ | Trong `docs/architecture.md` |
| T08 | Phân tích chức năng theo vị trí | ❌ | `docs/phan-tich-chuc-nang.md` |
| T09 | Phân quyền theo nhóm đối tượng | 🟡 | Có trong README/frontend README, cần gom thành mục riêng |
| T10 | Sơ đồ mạng tổng quan | 🟡 | Có mermaid trong `architecture.md`, cần bản vẽ đẹp |
| T11 | Checklist demo 10 tiêu chí | ❌ | `docs/demo-checklist.md` |
| T12 | Cập nhật README / architecture / setup | ✅ | `setup.md` runbook 4 máy; `README.md` + `architecture.md` đã đồng bộ 2026-10-29 (xem 5.5) |
| T13 | Cập nhật gap-analysis | ✅ | Đã cập nhật trạng thái BE/FE 2026-10-29; giữ làm lịch sử, file này là nguồn sự thật |

---

## 7. Backlog còn thiếu, xếp theo thứ tự

> Nguyên tắc: **không làm frontend nối API trước khi chốt schema**, vì API có thể đổi theo schema.
> Tuy nhiên có thể làm song song 2 luồng: **Luồng A – phân tán** (trọng tâm môn học) và **Luồng B – ứng dụng**.

### Giai đoạn 0 – Chốt quyết định (chặn mọi việc khác)

- [ ] **P0-01** Chốt tên CSDL dùng chung cho cả 4 máy.
      Đề xuất: đổi cả 4 về `Fashion_Store` (hoặc giữ nguyên nếu nhóm chấp nhận vi phạm quy tắc trong suốt).
      Ảnh hưởng: toàn bộ script, linked server, `application.properties`.
- [ ] **P0-02** Chốt bảng **chung** / bảng **riêng** (xem 4.2).
- [ ] **P0-03** Chốt cơ chế đồng bộ: transactional replication cho bảng chung + trigger/queue cho bảng riêng.
- [ ] **P0-04** Chốt chiến lược định danh toàn cục:
      `KH` + mã site + số thứ tự, hay GUID, hay `KH` + timestamp.
- [ ] **P0-05** Chốt tên Linked Server theo đích: `SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`, `CENTRAL`.
- [ ] **P0-06** Chốt kịch bản demo và dữ liệu seed (SP001 tồn 10 tại Q7, KH001, 3 chi nhánh, nhân viên, tài khoản).
- [ ] **P0-07** Chốt **mã chi nhánh**: `CN01` / `CN02` / `CN03` (Quận 1 / Quận 7 / Thủ Đức) theo đề bài và `AGENTS.md`, rồi sửa `frontend/src/config/branches.js` + alias ở backend.
- [ ] **P0-08** Chốt **cách cấu hình backend cho từng máy**: giữ `application-local.properties` + `SPRING_PROFILES_ACTIVE=local`, hay tạo profile `central`/`site`; đồng thời sửa `HUONG_DAN_CHAY_BACKEND.md` và `backend/README.md`.

**Xong khi:** các quyết định được ghi vào mục 10 và `docs/architecture.md`.

### Giai đoạn 1 – Database

- [x] **P1-01** Dựng lại cấu trúc `database/` — ✅ Đã có `common.sql`, `Fashion_Central.sql`, `sites/Fashion_Store.sql`, `seed.sql`, `linked-servers.sql`, `central-procs.sql`, `sites/site-procs.sql`, `replication.sql`.
- [x] **P1-02** Sửa schema: định danh toàn cục (`Q1-*`, `Q7-*`, `TD-*`), bỏ `UNIQUE(SoDienThoai)`, thêm `LastModified`, tích hợp `distributed_user`, dùng chung tên `Fashion_Store`.
- [x] **P1-03** Viết `seed.sql` mở rộng: 100 sản phẩm, 300 dòng tồn kho (CN1, CN2, CN3), 15 khách hàng, 10 nhân viên, 10 tài khoản, hóa đơn mẫu.
- [x] **P1-04** Chuyển file SQL sang UTF-8, thêm file `.gitattributes`.
- [ ] **P1-05** Làm script hoàn toàn idempotent: kiểm tra DB/table/login tồn tại trước khi tạo nếu chạy lẻ `common.sql`.
- [ ] **P1-06** Chạy thử trên 3 site + Central, xác nhận schema giống nhau.
- [x] **P1-07** Viết `database/linked-servers.sql` (hỗ trợ cả Central và Site).
- [x] **P1-08** Sửa `sp_DongBoSanPham` dùng đúng tên linked server và đồng bộ đủ cột + `LastModified`.

**Xong khi:** chạy script 2 lần liên tiếp không lỗi; 4 database cùng cấu trúc; có dữ liệu demo; `SELECT` được từng bảng.

### Giai đoạn 2 – Phân tán (trọng tâm môn học)

- [ ] **P2-01** Bật SQL Server Agent, tạo snapshot folder `\\CENTRAL\REPLDATA`, quyền Read/Write.
- [ ] **P2-02** Configure Distribution trên Central.
- [ ] **P2-03** Tạo Publication: article bảng chung không filter; article bảng riêng filter theo `MaChiNhanh`.
- [ ] **P2-04** Add Join dẫn xuất: `TonKho` → `HoaDon` → `ChiTietHoaDon`; `NhanVien` theo `MaChiNhanh`.
- [ ] **P2-05** Tạo Subscription cho 3 site, database đích cùng tên.
- [ ] **P2-06** Tạo Linked Server 4 chiều + `sp_addlinkedsrvlogin`.
- [ ] **P2-07** Viết `usp_TonKhoToanHeThong` (`UNION ALL` 3 site).
- [ ] **P2-08** Viết `usp_TongHopDoanhThu`.
- [ ] **P2-09** Viết `usp_TimHoaDon`, `usp_TimKhachHang` theo chuỗi `IF EXISTS`.
- [ ] **P2-10** Viết `usp_UpdateKhachHang`, `usp_DeleteKhachHang`, `usp_ChuyenChiNhanh` (nếu cần).
- [ ] **P2-11** Trigger đồng bộ site → Central + bảng `SyncQueue`.
- [ ] **P2-12** Trigger kiểm soát update/delete ở Central.
- [ ] **P2-13** Quy tắc giải quyết xung đột + chống trùng.
- [ ] **P2-14** Kiểm thử: mất Central, site vẫn bán; nối lại, dữ liệu đồng bộ.

**Xong khi:** 10 tiêu chí demo ở mục 2.4 chạy được trên hệ thống thật.

### Giai đoạn 3 – Backend bổ sung

- [ ] **P3-01** Tách cấu hình theo vai trò máy: Central / site (profile hoặc biến môi trường) — 🟡 đã có `application-local.properties` nhưng chưa có profile riêng.
- [ ] **P3-02** API tồn kho toàn chuỗi cho Admin (`GET /api/inventory/all` hoặc gọi 3 site).
- [ ] **P3-03** API trạng thái site: ping từng linked server, trả online/offline/latency.
- [ ] **P3-04** Tổng hợp Central qua linked server hoặc dữ liệu replication.
- [ ] **P3-05** Idempotency cho checkout: thêm `requestId`, lưu và trả lại kết quả cũ nếu trùng.
- [ ] **P3-06** Làm giàu `InventoryResponse` (tên SP, loại, thương hiệu, giá) hoặc adapter ở FE.
- [ ] **P3-07** Mã hóa đơn theo quy ước site (nếu P0 chốt).
- [ ] **P3-08** Bổ sung test: inventory, sales, product, customer, auth.
- [ ] **P3-09** Thống nhất alias mã chi nhánh ở **tất cả** service (`StatisticsService`, `DashboardService`) thay vì chỉ `BranchService`/`InventoryService`.
- [ ] **P3-10** Tạo `application.properties` mặc định (không chứa secret) hoặc hướng dẫn rõ profile `local`, để clone mới start được.

**Xong khi:** API trả đúng dữ liệu từ DB thật; test pass; không còn endpoint nào dùng mock ở BE.

### Giai đoạn 4 – Nối frontend với backend

Thứ tự làm từng service, mỗi service xong thì chạy lint + build + thử trang:

- [x] **P4-01** `branchService`
- [x] **P4-02** `employeeService`
- [x] **P4-03** `accountService`
- [x] **P4-04** `productService`
- [x] **P4-05** `customerService`
- [x] **P4-06** `inventoryService`
- [x] **P4-07** `invoiceService` + POS
- [x] **P4-08** `statisticsService` (thêm adapter shape BE → FE)
- [x] **P4-09** `dashboardService`
- [ ] **P4-10** `siteService` (chờ P3-03)
- [ ] **P4-11** Xóa dần `mockData.js` / `mockStore.js` khi không còn dùng; giữ lại nếu cần chế độ demo offline.

**Xong khi:** tắt backend → trang báo lỗi rõ ràng; bật backend → số liệu lấy từ SQL Server; bán hàng → `HoaDon`/`TonKho` thay đổi thật.

### Giai đoạn 5 – Tài liệu & demo

- [ ] **P5-01** `docs/kich-ban.md`
- [ ] **P5-02** `docs/erd.md` (7.2, 7.3, 7.4)
- [ ] **P5-03** `docs/phan-manh.md` (7.5 + 3 quy tắc)
- [ ] **P5-04** `docs/dinh-vi.md` (3.5, W/E/D/R/H/L)
- [ ] **P5-05** `docs/nhan-ban-dong-bo.md` (3.6, 3.7)
- [ ] **P5-06** `docs/phan-tich-chuc-nang.md` (2.2, 3, 4, 5)
- [ ] **P5-07** `docs/demo-checklist.md` (10 tiêu chí + ảnh chụp)
- [ ] **P5-08** Cập nhật `README.md`, `docs/architecture.md` (~~`docs/setup.md` đã xong 2026-10-29~~)
- [ ] **P5-09** Ảnh chụp: ping, linked server, publication, bán hàng, tồn trước/sau, truy vấn phân tán

**Xong khi:** một người ngoài nhóm đọc tài liệu có thể dựng lại hệ thống và chạy demo.

---

## 8. Kịch bản làm việc theo phiên

> Mỗi phiên nên ngắn, có đầu ra rõ ràng. Mở phiên mới → đọc file này → chọn đúng phiên → làm → cập nhật file.

| Phiên | Mục tiêu | Đầu vào | Đầu ra / xong khi |
| --- | --- | --- | --- |
| **S0 – Khởi động** | Nắm trạng thái | File này, `git status` | Biết mục 3, 7; chọn việc tiếp theo |
| **S1 – Chốt kiến trúc** | P0-01 → P0-06 | Mục 4.2, 10 | Quyết định ghi vào mục 10 |
| **S2 – Schema** | P1-01 → P1-03 | P0 đã chốt | 4 file DB + seed chạy được |
| **S3 – Database hoàn chỉnh** | P1-04 → P1-06 | S2 | Chạy 2 lần không lỗi, 4 DB giống nhau |
| **S4 – Distribution** | P2-01 → P2-05 | S3 | Publication + subscription hoạt động |
| **S5 – Linked Server & SP** | P2-06 → P2-10 | S4 | `SELECT` xuyên site, SP chạy mọi nơi |
| **S6 – Trigger & đồng bộ** | P2-11 → P2-13 | S5 | Dữ liệu site → Central → site khác |
| **S7 – Kiểm thử phân tán** | P2-14, mục 9.3 | S6 | 10 tiêu chí demo pass |
| **S8 – Backend bổ sung** | P3-01 → P3-10 | S3 | API thật, test pass |
| **S9 – FE integration 1** | P4-01 → P4-05 | S8 | ✅ đã xong (5 service đầu gọi API thật) |
| **S10 – FE integration 2** | P4-06 → P4-11 | S9 | 🟡 4/5 service còn lại đã xong; còn `siteService` (P4-10) |
| **S11 – Tài liệu & demo** | P5-01 → P5-09 | S7, S10 | Bộ tài liệu + demo hoàn chỉnh |

> **Tình trạng 2026-10-29:** S9 xong; S10 xong 9/10 service. Việc còn lại trước khi làm phân tán:
> chốt P0-07 (mã chi nhánh) + P0-08 (cấu hình backend), seed dữ liệu (P1-03), rồi mới sang S4–S7.
> S8 (P3-01, P3-09, P3-10) nên làm ngay sau P0 để tránh sửa lại hai lần.

### 8.1. Mẫu prompt cho phiên AI mới

```text
Đọc docs/PROJECT_STATUS.md trước khi làm bất cứ việc gì.

Trạng thái hiện tại: <commit>, nhánh <branch>.
Việc cần làm trong phiên này: <ID trong mục 7, ví dụ P1-02>.
Phạm vi: chỉ sửa <file/thư mục>. Không sửa schema ngoài phạm vi.
Ràng buộc:
- Không commit thẳng vào develop.
- Không đổi tên bảng/cột/linked server nếu chưa có quyết định trong mục 10.
- Frontend không gọi fetch trực tiếp; backend giữ ddl-auto=none.
- Sau khi xong: chạy kiểm tra ở mục 9.1, cập nhật mục 6/7/11 của file này.
```

---

## 9. Kiểm chứng & checklist demo

### 9.1. Lệnh kiểm tra thường dùng

```powershell
# Frontend
cd frontend
npm run lint
npm run build

# Backend (chạy test; exit code 1 do warning Mockito ghi stderr là bình thường)
cd backend
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd test

# Đếm test thật (đọc surefire, không tin exit code)
Get-ChildItem target/surefire-reports -Filter *.txt |
  ForEach-Object { (Select-String -Path $_.FullName -Pattern 'Tests run').Line }

# Chạy backend ở máy local (profile local)
$env:SPRING_PROFILES_ACTIVE = 'local'
.\mvnw.cmd spring-boot:run

# Git
git status
git log --oneline -10
git fetch origin
```

### 9.2. Kiểm chứng dữ liệu

| Việc | Cách kiểm |
| --- | --- |
| Schema 3 site giống nhau | So sánh danh sách bảng/cột giữa 3 DB |
| Seed đúng kịch bản | `SELECT * FROM TonKho WHERE MaSanPham='SP001' AND MaChiNhanh='CN02'` → 10 |
| Phân mảnh đúng | Mỗi site chỉ có `TonKho`/`HoaDon` của `MaChiNhanh` mình |
| Linked Server | `EXEC sp_testlinkedserver N'SITE_Q1'` |
| Truy vấn phân tán | `SELECT * FROM SITE_Q1.Fashion_Q1.dbo.TonKho UNION ALL ...` |
| SP trong suốt | Chạy cùng một SP trên 3 site, kết quả giống nhau |
| Bán hàng | Bán 2 SP001 tại Q7 → tồn 10 → 8, có 1 `HoaDon`, 2 `ChiTietHoaDon` |
| Chống tồn âm | Bán vượt tồn → lỗi, không tạo hóa đơn |
| Chống trùng | Gửi lại cùng `requestId` → không tạo thêm hóa đơn |
| Mất Central | Ngắt link → site vẫn bán; nối lại → đồng bộ |

### 9.3. Checklist demo 10 tiêu chí

- [ ] 1. 4 máy ping được nhau
- [ ] 2. Central `sp_testlinkedserver` 3 site OK
- [ ] 3. `TonKho` mỗi site chỉ chứa chi nhánh mình
- [ ] 4. `HoaDon` mỗi site chỉ chứa chi nhánh mình
- [ ] 5. `ChiTietHoaDon` nằm cùng site với hóa đơn cha
- [ ] 6. Tạo `KH001` tại Q7 → thấy ở Central, Q1, Thủ Đức
- [ ] 7. Bán hàng tại Q7 thành công
- [ ] 8. Tồn SP001 tại Q7 giảm đúng số lượng
- [ ] 9. Central chạy `usp_TonKhoToanHeThong` thấy đủ 3 site
- [ ] 10. Ngắt Central, Q7 vẫn bán và cập nhật tồn; nối lại đồng bộ

---

## 10. Quyết định đã chốt & câu hỏi còn mở

### 10.1. Đã chốt (đang áp dụng trong code)

| Ngày | Quyết định | Lý do |
| --- | --- | --- |
| 2026-10-22 | Frontend dùng lớp mock `mockData.js` + service | Backend chưa có API; đổi sang API chỉ sửa thân hàm |
| 2026-10-22 | Dùng `useAsyncData` suy luận `loading` từ request key | Tránh luật `react-hooks/set-state-in-effect` |
| 2026-10-22 | Dữ liệu mock dùng PRNG tất định | Reload không đổi số |
| 2026-10-23 | Tồn kho dùng `Inventory` entity + `SERIALIZABLE` | Chống bán vượt tồn |
| 2026-10-25 | Auth gọi API thật, bỏ mock auth | Backend đã có JWT |
| 2026-10-25 | Role lưu chữ thường ở FE | `RoleGuard` so sánh trực tiếp |
| 2026-10-29 | Tạo `docs/PROJECT_STATUS.md` làm nguồn sự thật | Tránh phân tán thông tin giữa nhiều file |
| 2026-10-29 | Frontend gọi API thật qua `api/client.js` (Bearer + refresh + `getAllPages`) | Bỏ dần mock; giữ shape dữ liệu nhờ hàm `to*` trong từng service |
| 2026-10-29 | Bootstrap tài khoản chạy ở mọi profile | Clone mới vẫn có tài khoản demo (`staff.q1`, `manager.q1`, `admin` / `123456`) |
| 2026-10-29 | `application.properties` bị gitignore, track `application-local.properties` | Tránh commit mật khẩu; **nhưng** phải bật profile `local` mới đọc được (xem Q9) |
| 2026-10-30 | 3 site dùng chung CSDL `Fashion_Store`, Central `Fashion_Central` | Tuân thủ quy tắc trong suốt (transparency rule a: tên subscriber giống nhau) |
| 2026-10-30 | Mã chi nhánh `CN1` / `CN2` / `CN3` (Quận 1 / Quận 7 / Thủ Đức) | Khớp bộ lọc PDF `MACN='CN1'`; FE + BE đã đồng bộ |
| 2026-10-30 | `TaiKhoan` replicate read-only xuống site, lọc theo `NhanVien` | Cho phép đăng nhập offline tại site |

### 10.2. Đề xuất, **chưa chốt** (cần nhóm quyết)

| Mã | Vấn đề | Đề xuất | Ảnh hưởng nếu đổi |
| --- | --- | --- | --- |
| Q2 | Định danh `KhachHang` | `KH` + mã site + số thứ tự (ví dụ `KH01...`) hoặc GUID | Đổi kiểu cột, sửa `GeneratedId`, sửa seed |
| Q3 | Định danh `HoaDon` | `HD` + mã site + timestamp/số thứ tự | Sửa `SalesService`, bỏ UUID |
| Q5 | Cơ chế đồng bộ bảng riêng | Trigger + `SyncQueue` | Cần bảng mới, job retry |
| Q6 | Có cho tạo khách hàng tại site không | Có, đồng bộ 2 chiều | Cần giải quyết xung đột |
| Q7 | Demo trên 4 máy thật hay 3 instance | 4 máy theo README | Cần Radmin VPN, IP tĩnh |
| Q8 | Chiều đồng bộ bảng riêng (`TonKho`, `HoaDon`, `ChiTietHoaDon`) | Site là nguồn ghi + trigger/queue đẩy lên Central (mô hình B, `docs/setup.md` 6.5) | Nếu chọn A (Central ghi rồi đẩy xuống) thì không đạt tiêu chí “mất Central vẫn bán” |
| Q9 | Cách cấu hình backend cho từng máy | Giữ `application-local.properties` + `SPRING_PROFILES_ACTIVE=local`; hoặc tạo thêm `application.properties` mặc định không secret và 3 profile `central`/`q1`/`q7`/`td` | Ảnh hưởng `HUONG_DAN_CHAY_BACKEND.md`, `backend/README.md`, `docs/setup.md`, CI |

---

## 11. Lịch sử cập nhật

| Ngày | Commit | Nội dung |
| --- | --- | --- |
| 2026-10-29 | `e2b0e3f` | Đối chiếu lại toàn bộ tài liệu với code mới: FE gọi API thật (10/11 service), `api/client.js`, `Fashion_Central.sql`, bootstrap tài khoản mọi profile, alias mã chi nhánh; cập nhật dashboard, D01–D16, B01–B21, F01–F14, backlog P0-07/P0-08, P1-07/P1-08, P3-09/P3-10, Q9–Q10 |
| 2026-10-29 | chưa commit | Viết lại `docs/setup.md` thành runbook 4 máy: môi trường (Agent, 1433, mixed auth, snapshot), distribution, publication/subscription, linked server, trigger + `SyncQueue`, kiểm tra nghiệm thu, troubleshooting, rollback, checklist theo máy. Xuất thêm `docs/setup.docx` (19 trang) để đọc/in |
| 2026-10-29 | `5b08a57` | Tạo file này: quét lại toàn bộ repo, đối chiếu mẫu giảng viên, chấm tiến độ, lập backlog P0–P5 |
| 2026-10-25 | `ed912ee` | Auth gọi API thật; backend có dashboard/thống kê/chi nhánh/tài khoản |
| 2026-10-23 | `2982227` | Gộp nhánh `feature/inventory-statistics` (7 trang + 4 service mock) |
| 2026-10-22 | `3c03027` | Thêm `docs/gap-analysis.md` |

---

### Ghi chú cuối

- File này **không thay thế** `docs/gap-analysis.md`; nó là bản cập nhật trạng thái và kế hoạch.
- Khi hoàn thành một giai đoạn, hãy cập nhật lại **mục 3, 6, 7, 11** để phiên sau không phải đọc lại toàn bộ code.
- Nếu phát hiện mâu thuẫn giữa file này và code, **code là sự thật**, nhưng phải sửa file này ngay.