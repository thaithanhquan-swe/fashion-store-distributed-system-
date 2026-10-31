# Fashion Store Distributed System

Đồ án **Cơ sở dữ liệu phân tán – Quản lý chuỗi cửa hàng thời trang**.

## Bộ khung trong repository

Đã tạo cấu trúc `database/`, `backend/`, `frontend/` và `docs/` theo mục 12.

- **Database:** 4 script tạo DB + 8 bảng (`Fashion_Central`, `Fashion_Q1`, `Fashion_Q7`, `Fashion_TD`).
  Chưa có dữ liệu mẫu, chưa có replication / linked server / trigger.
- **Backend:** Spring Boot REST API — 12 controller, 50 endpoint, 73 test pass.
- **Frontend:** React + Vite — 30 trang, 10/11 service gọi API thật (chỉ `siteService` còn mock).
- **Phân tán:** chưa triển khai (0%).

Tài liệu:

- [Trạng thái & kế hoạch](docs/PROJECT_STATUS.md) — **nguồn sự thật duy nhất**
- [Thiết kế kiến trúc](docs/architecture.md)
- [Hướng dẫn triển khai 4 máy](docs/setup.md)
- [Đối chiếu mẫu giảng viên](docs/gap-analysis.md)
- [Thiết kế backend](backend/README.md)
- [Thiết kế frontend](frontend/README.md)

Các phần phía dưới mô tả kiến trúc mục tiêu và môi trường database của nhóm.

Hệ thống mô phỏng 3 cửa hàng:

- Store A – Quận 1
- Store B – Quận 7
- Store C – Thủ Đức

Hệ thống được triển khai trên **4 máy tính**:

- Máy 1: Central Server
- Máy 2: Store A – Quận 1
- Máy 3: Store B – Quận 7
- Máy 4: Store C – Thủ Đức

Các máy kết nối với nhau qua **Radmin VPN** và **SQL Server Linked Server**.

---

# 1. Mục tiêu đề tài

Hệ thống tập trung vào các nội dung của cơ sở dữ liệu phân tán:

- Phân bố dữ liệu trên nhiều máy.
- Phân mảnh dữ liệu theo chi nhánh.
- Replication dữ liệu dùng chung.
- Truy vấn dữ liệu giữa các Site.
- Bán hàng tại từng chi nhánh.
- Cập nhật tồn kho sau khi bán.
- Đồng bộ dữ liệu dùng chung.
- Central có thể truy vấn dữ liệu toàn hệ thống.

Nghiệp vụ chính:

```text
Bán hàng
    ↓
Cập nhật tồn kho
    ↓
Đồng bộ dữ liệu cần thiết
```

---

# 2. Kiến trúc hệ thống

```text
                         MÁY 1
                   CENTRAL SERVER
                  Fashion_Central
                         │
                         │
                    Radmin VPN
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼

        MÁY 2          MÁY 3          MÁY 4

      STORE A Q1      STORE B Q7     STORE C THỦ ĐỨC

      Fashion_Q1      Fashion_Q7     Fashion_ThuDuc

         CN01            CN02              CN03
```

Mô hình kết nối:

```text
Q1 ───────┐
          │
Q7 ───────┼──── CENTRAL
          │
THỦ ĐỨC ──┘
```

Các Site **không bắt buộc kết nối trực tiếp với nhau**.

Central đóng vai trò trung tâm kết nối.

---

# 3. Phân chia 4 máy

| Máy   | Vai trò           | Database          | Mã chi nhánh |
| ----- | ----------------- | ----------------- | ------------ |
| Máy 1 | Central Server    | `Fashion_Central` | -            |
| Máy 2 | Store A – Quận 1  | `Fashion_Q1`      | `CN01`       |
| Máy 3 | Store B – Quận 7  | `Fashion_Q7`      | `CN02`       |
| Máy 4 | Store C – Thủ Đức | `Fashion_ThuDuc`  | `CN03`       |

---

# 4. Các bảng chính

Theo yêu cầu đề tài, hệ thống tập trung vào 6 bảng:

```text
SanPham
ChiNhanh
TonKho
KhachHang
HoaDon
ChiTietHoaDon
```

---

## 4.1. SanPham

Lưu thông tin sản phẩm.

Ví dụ:

```text
MaSanPham
TenSanPham
LoaiSanPham
GiaBan
TrangThai
```

---

## 4.2. ChiNhanh

Lưu thông tin các cửa hàng.

Ví dụ:

```text
MaChiNhanh
TenChiNhanh
DiaChi
SoDienThoai
TrangThai
```

Quy ước:

```text
CN01 → Store A – Quận 1
CN02 → Store B – Quận 7
CN03 → Store C – Thủ Đức
```

---

## 4.3. TonKho

Lưu số lượng sản phẩm tại từng chi nhánh.

Ví dụ:

```text
MaChiNhanh
MaSanPham
SoLuongTon
```

Ví dụ:

```text
CN01 | SP001 | 10
CN02 | SP001 | 5
CN03 | SP001 | 8
```

Mỗi chi nhánh có tồn kho riêng.

---

## 4.4. KhachHang

Lưu thông tin khách hàng.

Ví dụ:

```text
MaKhachHang
HoTen
SoDienThoai
Email
DiaChi
```

`KhachHang` là dữ liệu dùng chung và có thể được replication giữa các Site.

---

## 4.5. HoaDon

Lưu giao dịch bán hàng.

Ví dụ:

```text
MaHoaDon
MaChiNhanh
MaKhachHang
NgayLap
TongTien
```

Hóa đơn được phân mảnh theo chi nhánh.

---

## 4.6. ChiTietHoaDon

Lưu sản phẩm thuộc từng hóa đơn.

Ví dụ:

```text
MaHoaDon
MaSanPham
SoLuong
DonGia
ThanhTien
```

`ChiTietHoaDon` được phân mảnh theo `HoaDon`.

---

# 5. Phân bố dữ liệu

## Store A – Quận 1

Database:

```text
Fashion_Q1
```

Chứa dữ liệu thuộc:

```text
MaChiNhanh = 'CN01'
```

Ví dụ:

```text
TonKho CN01
HoaDon CN01
ChiTietHoaDon của các hóa đơn CN01
```

---

## Store B – Quận 7

Database:

```text
Fashion_Q7
```

Chứa dữ liệu thuộc:

```text
MaChiNhanh = 'CN02'
```

---

## Store C – Thủ Đức

Database:

```text
Fashion_ThuDuc
```

Chứa dữ liệu thuộc:

```text
MaChiNhanh = 'CN03'
```

---

# 6. Phân mảnh dữ liệu

## TonKho

`TonKho` được phân mảnh ngang theo `MaChiNhanh`.

```text
TonKho_Q1
=
TonKho WHERE MaChiNhanh = 'CN01'
```

```text
TonKho_Q7
=
TonKho WHERE MaChiNhanh = 'CN02'
```

```text
TonKho_ThuDuc
=
TonKho WHERE MaChiNhanh = 'CN03'
```

Như vậy:

```text
Fashion_Q1
→ chỉ quản lý tồn kho Q1

Fashion_Q7
→ chỉ quản lý tồn kho Q7

Fashion_ThuDuc
→ chỉ quản lý tồn kho Thủ Đức
```

---

## HoaDon

`HoaDon` cũng được phân mảnh ngang theo `MaChiNhanh`.

```text
CN01 → Fashion_Q1
CN02 → Fashion_Q7
CN03 → Fashion_ThuDuc
```

---

## ChiTietHoaDon

`ChiTietHoaDon` không cần có `MaChiNhanh`.

Nó được phân mảnh dẫn xuất dựa trên `HoaDon`.

Ví dụ:

```text
HoaDon HD001 thuộc CN02
        ↓
ChiTietHoaDon của HD001
        ↓
lưu tại Fashion_Q7
```

---

# 7. Replication dữ liệu

Dữ liệu phù hợp nhất để replication là:

```text
KhachHang
```

Ví dụ khách hàng được tạo tại Quận 7:

```text
Fashion_Q7
     ↓
Tạo KH001
     ↓
Central
     ↓
Q1 + Thủ Đức
```

Sau khi đồng bộ:

```text
Fashion_Q1      → có KH001
Fashion_Q7      → có KH001
Fashion_ThuDuc  → có KH001
Fashion_Central → có KH001
```

Nhờ đó khách đã mua tại Q7 vẫn có thể được nhận diện khi mua tại Q1 hoặc Thủ Đức.

---

# 8. Dữ liệu nào không replication giữa các Site?

Không replication toàn bộ:

```text
TonKho
HoaDon
ChiTietHoaDon
```

Ví dụ Q7 bán sản phẩm:

```text
TonKho Q7:
10 → 8
```

Q1 và Thủ Đức không thay đổi.

Central muốn xem tồn kho toàn hệ thống thì truy vấn từng Site.

---

# 9. Central Server làm gì?

Database:

```text
Fashion_Central
```

Central có nhiệm vụ:

- Kết nối tới Q1.
- Kết nối tới Q7.
- Kết nối tới Thủ Đức.
- Truy vấn dữ liệu toàn hệ thống.
- Tổng hợp báo cáo.
- Hỗ trợ đồng bộ dữ liệu dùng chung.
- Làm trung tâm cho replication khách hàng.

Central **không thay thế database cục bộ của các Site**.

---

# 10. Nếu Central mất kết nối thì sao?

Các Site vẫn có database riêng nên vẫn có thể hoạt động cục bộ.

Ví dụ Q7 mất kết nối Central:

```text
Fashion_Q7 vẫn hoạt động
        ↓
Vẫn xem tồn kho Q7
        ↓
Vẫn bán hàng
        ↓
Vẫn tạo hóa đơn
        ↓
Vẫn cập nhật tồn kho
```

Chỉ có phần:

```text
Đồng bộ với Central
```

tạm thời chưa thực hiện được.

Khi kết nối lại, dữ liệu cần đồng bộ sẽ được xử lý tiếp.

---

# 11. Nghiệp vụ bán hàng

Ví dụ bán hàng tại Quận 7:

```text
Nhân viên chọn sản phẩm
        ↓
Kiểm tra TonKho_Q7
        ↓
Đủ số lượng?
    /          \
 Không          Có
   │             │
   ▼             ▼
Báo hết hàng   Tạo HoaDon
                  ↓
          Tạo ChiTietHoaDon
                  ↓
           Trừ TonKho_Q7
                  ↓
          Hoàn thành bán hàng
```

Ví dụ:

```text
SP001 tại Q7

Trước khi bán:
10 sản phẩm

Khách mua:
2 sản phẩm

Sau khi bán:
8 sản phẩm
```

---

# 12. Cấu trúc Repository

Project sử dụng cấu trúc đơn giản:

```text
fashion-store-distributed-system/
│
├── README.md
│
├── database/
│   ├── Fashion_Central.sql
│   ├── Fashion_Q1.sql
│   ├── Fashion_Q7.sql
│   └── Fashion_TD.sql
│
├── backend/
├── frontend/
└── docs/
```

---

# 13. Ý nghĩa các file SQL

> Cập nhật 2026-10-29: tên file thực tế là `Fashion_*.sql`. Cả 4 file đều **chỉ tạo DB + 8 bảng**,
> chưa có dữ liệu mẫu (`INSERT`), chưa có linked server, chưa có replication.

## `database/Fashion_Central.sql`

Chạy trên **Máy 1 – Central**. Tạo:

```text
- Database Fashion_Central
- 8 bảng: ChiNhanh, ChiTietHoaDon, HoaDon, KhachHang,
          NhanVien, SanPham, TaiKhoan, TonKho
- CREATE USER distributed_user (db_datareader, db_datawriter)
- sp_DongBoSanPham (ví dụ, còn lỗi tên linked server + chỉ đồng bộ TenSanPham)
```

**Chưa có:** linked server tới 3 site, truy vấn tổng hợp, publication.

---

## `database/Fashion_Q1.sql`

Chạy trên **Máy 2 – Store A Quận 1**. Tạo:

```text
- Database Fashion_Q1
- 8 bảng giống Central
- CREATE USER distributed_user
```

---

## `database/Fashion_Q7.sql`

Chạy trên **Máy 3 – Store B Quận 7**. Tạo `Fashion_Q7` + 8 bảng.
**Thiếu `distributed_user`** → phải chạy thêm mục 2.7 của `docs/setup.md`.

---

## `database/Fashion_TD.sql`

Chạy trên **Máy 4 – Store C Thủ Đức**. Tạo `Fashion_TD` + 8 bảng.
**Tên DB sai** so với quy ước `Fashion_ThuDuc`; **thiếu `distributed_user`**.

---

## Chưa có

```text
database/common.sql        Schema/SP dùng chung
database/seed.sql          Dữ liệu mẫu theo kịch bản demo
database/linked-server.sql Script sp_addlinkedserver cho Central
database/replication/      Publication + subscription + trigger
```

Xem `docs/PROJECT_STATUS.md` mục 7 (P1-01 → P1-08) để biết thứ tự làm.

---

# 14. Cấu trúc Backend

```text
backend/
```

Backend dùng để xây dựng API cho ứng dụng.

Các chức năng chính:

```text
Xem sản phẩm
Xem tồn kho
Quản lý khách hàng
Bán hàng
Xem hóa đơn
```

Backend không cần quá phức tạp vì trọng tâm của đồ án là **Cơ sở dữ liệu phân tán**.

---

# 15. Cấu trúc Frontend

```text
frontend/
```

Frontend chỉ cần các màn hình cơ bản:

```text
Sản phẩm
Tồn kho
Khách hàng
Bán hàng
Hóa đơn
```

---

# 16. Thư mục docs

```text
docs/
```

Chứa:

```text
ERD
Sơ đồ kiến trúc 4 máy
Sơ đồ phân mảnh
Sơ đồ replication
Ảnh Linked Server
Ảnh ping giữa các máy
Ảnh test bán hàng
Ảnh tồn kho trước/sau
Ảnh truy vấn phân tán
```

---

# 17. Phần mềm cần cài

Mỗi máy cần:

```text
SQL Server
SQL Server Management Studio
Microsoft OLE DB Driver for SQL Server
Radmin VPN
Git
```

Máy làm frontend/backend cài thêm công nghệ tương ứng.

---

# 18. Clone Repository

```bash
git clone https://github.com/<username>/fashion-store-distributed-system.git
```

Sau đó:

```bash
cd fashion-store-distributed-system
```

---

# 19. Kết nối Radmin VPN

Cả 4 máy phải tham gia cùng một mạng Radmin VPN.

Ví dụ:

| Máy   | Vai trò | Radmin IP      |
| ----- | ------- | -------------- |
| Máy 1 | Central | `<IP_CENTRAL>` |
| Máy 2 | Q1      | `<IP_Q1>`      |
| Máy 3 | Q7      | `<IP_Q7>`      |
| Máy 4 | Thủ Đức | `<IP_THUDUC>`  |

Ví dụ:

```text
Central   → 26.xx.xx.10
Q1        → 26.xx.xx.11
Q7        → 26.xx.xx.12
Thủ Đức   → 26.xx.xx.13
```

IP thực tế sẽ được cập nhật sau.

---

# 20. Kiểm tra kết nối giữa các máy

Ví dụ từ Q7 kiểm tra Central:

```powershell
ping <IP_CENTRAL>
```

Nếu ping thành công thì tiếp tục kiểm tra SQL Server.

---

# 21. Cấu hình TCP/IP SQL Server

Trên cả 4 máy:

```text
SQL Server Configuration Manager
```

Đi tới:

```text
SQL Server Network Configuration
→ Protocols for MSSQLSERVER
→ TCP/IP
```

Chọn:

```text
Enabled
```

Sau đó:

```text
TCP/IP
→ Properties
→ IP Addresses
→ IPAll
```

Thiết lập:

```text
TCP Dynamic Ports: để trống

TCP Port:
1433
```

Restart SQL Server sau khi cấu hình.

---

# 22. Mở Firewall Port 1433

Trên Windows:

```text
Windows Defender Firewall
→ Advanced Settings
→ Inbound Rules
→ New Rule
→ Port
→ TCP
→ 1433
→ Allow the connection
```

---

# 23. Kiểm tra port

Từ máy khác:

```powershell
Test-NetConnection <IP_SERVER> -Port 1433
```

Kết quả cần có:

```text
TcpTestSucceeded : True
```

---

# 24. Quy ước Linked Server

Toàn bộ project sử dụng các tên:

```text
CENTRAL
SITE_Q1
SITE_Q7
SITE_THUDUC
```

Không sử dụng:

```text
MAY1
MAY2
MAY3
MAY4
```

để tránh khó hiểu khi đọc SQL.

---

# 25. Linked Server tại Central

Central tạo 3 Linked Server:

```text
SITE_Q1
SITE_Q7
SITE_THUDUC
```

Mô hình:

```text
Fashion_Central
     │
     ├──── SITE_Q1
     ├──── SITE_Q7
     └──── SITE_THUDUC
```

Ví dụ tạo `SITE_Q7`:

```sql
USE master;
GO

EXEC master.dbo.sp_addlinkedserver
    @server     = N'SITE_Q7',
    @srvproduct = N'',
    @provider   = N'MSOLEDBSQL',
    @provstr    = N'Encrypt=Optional;TrustServerCertificate=Yes',
    @datasrc    = N'<IP_Q7>,1433';
GO
```

Thiết lập login:

```sql
EXEC master.dbo.sp_addlinkedsrvlogin
    @rmtsrvname = N'SITE_Q7',
    @useself = N'False',
    @locallogin = NULL,
    @rmtuser = N'<SQL_USER>',
    @rmtpassword = N'<SQL_PASSWORD>';
GO
```

---

# 26. Linked Server tại các Site

Mỗi Site chỉ cần tạo Linked Server:

```text
CENTRAL
```

Ví dụ Q7:

```text
Fashion_Q7
     │
     └──── CENTRAL
```

Ví dụ:

```sql
USE master;
GO

EXEC master.dbo.sp_addlinkedserver
    @server     = N'CENTRAL',
    @srvproduct = N'',
    @provider   = N'MSOLEDBSQL',
    @provstr    = N'Encrypt=Optional;TrustServerCertificate=Yes',
    @datasrc    = N'<IP_CENTRAL>,1433';
GO
```

---

# 27. Test Linked Server

Tại Central:

```sql
EXEC sp_testlinkedserver N'SITE_Q1';
EXEC sp_testlinkedserver N'SITE_Q7';
EXEC sp_testlinkedserver N'SITE_THUDUC';
```

Ví dụ truy vấn Q7:

```sql
SELECT *
FROM SITE_Q7.Fashion_Q7.dbo.TonKho;
```

---

# 28. Central truy vấn dữ liệu toàn hệ thống

Ví dụ xem tồn kho cả 3 chi nhánh:

```sql
SELECT 'Q1' AS Site, *
FROM SITE_Q1.Fashion_Q1.dbo.TonKho

UNION ALL

SELECT 'Q7' AS Site, *
FROM SITE_Q7.Fashion_Q7.dbo.TonKho

UNION ALL

SELECT 'THUDUC' AS Site, *
FROM SITE_THUDUC.Fashion_ThuDuc.dbo.TonKho;
```

Đây là truy vấn phân tán.

Không cần copy `TonKho` của 3 chi nhánh về chung một bảng.

---

# 29. Thứ tự Setup Project

## Bước 1

Clone repository.

```text
Git Clone
```

## Bước 2

Cài:

```text
SQL Server
SSMS
Radmin VPN
Git
```

## Bước 3

4 máy kết nối cùng mạng Radmin VPN.

## Bước 4

Bật SQL Server TCP/IP.

## Bước 5

Mở port:

```text
1433
```

## Bước 6

Restore database tương ứng.

```text
Máy 1 → Fashion_Central
Máy 2 → Fashion_Q1
Máy 3 → Fashion_Q7
Máy 4 → Fashion_ThuDuc
```

## Bước 7

Kiểm tra database và dữ liệu.

## Bước 8

Chạy file SQL tương ứng:

```text
Máy 1 → database/Fashion_Central.sql

Máy 2 → database/Fashion_Q1.sql

Máy 3 → database/Fashion_Q7.sql

Máy 4 → database/Fashion_TD.sql   (sẽ đổi thành Fashion_ThuDuc)
```

## Bước 9

Chạy:

```text
database/common.sql
```

nếu cần các Stored Procedure dùng chung. **File này chưa tồn tại** — xem `docs/PROJECT_STATUS.md` P1-01.

## Bước 10

Test Linked Server.

## Bước 11

Test nghiệp vụ bán hàng.

## Bước 12

Test replication khách hàng.

## Bước 13

Test khi ngắt kết nối Central.

---

# 30. Trạng thái database hiện tại

Trong repo đã có **script tạo schema** cho 4 database (mỗi file 8 bảng):

```text
Fashion_Central.sql   -> Fashion_Central   (+ distributed_user, sp_DongBoSanPham)
Fashion_Q1.sql        -> Fashion_Q1        (+ distributed_user)
Fashion_Q7.sql        -> Fashion_Q7        (thiếu distributed_user)
Fashion_TD.sql        -> Fashion_TD        (thiếu distributed_user, tên sai quy ước)
```

**Chưa có dữ liệu mẫu:** cả 4 file đều **0 câu `INSERT`**. Database sau khi tạo sẽ trống;
backend tự tạo tài khoản demo và chi nhánh `Local Demo Branch` (`CN001`).

**Chưa có:** linked server, publication/subscription, trigger, `SyncQueue`, `seed.sql`.

Do đó các thành viên **không tự ý tạo database mới hoặc thay đổi cấu trúc bảng**.

Nếu cần thay đổi phải thống nhất với nhóm trước. Trạng thái chi tiết: `docs/PROJECT_STATUS.md`.

---

# 31. Nguyên tắc làm việc

Không tự ý thay đổi:

```text
Tên database
Tên bảng
Tên column
Primary Key
Foreign Key
Mã chi nhánh
Tên Linked Server
```

> **Quy tắc trong suốt (transparency):** tên CSDL ở các Subscriber phải **giống nhau** và tên
> Linked Server phải **thống nhất theo quy ước** (`SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`, `CENTRAL`).
> Nhờ đó một stored procedure viết một lần chạy được ở mọi site mà không phải sửa đường dẫn.
> Hiện 3 site còn dùng 3 tên khác nhau (`Fashion_Q1`, `Fashion_Q7`, `Fashion_TD`) — xem
> `docs/setup.md` mục 0.2 để chốt trước khi triển khai.

Nếu cần sửa:

```text
Thống nhất với nhóm
       ↓
Sửa SQL
       ↓
Test
       ↓
Commit lên Git
       ↓
Thành viên khác Pull
```

---

# 32. Không commit thông tin nhạy cảm

Không đưa lên Git:

```text
SQL Server password
sa password
Radmin VPN password
Private key
.env
```

Trong file SQL nên để:

```text
<IP_CENTRAL>
<IP_Q1>
<IP_Q7>
<IP_THUDUC>

<SQL_USER>
<SQL_PASSWORD>
```

Sau khi clone, từng thành viên tự điền thông tin máy mình.

---

# 33. Thành viên phụ trách

| Thành viên   | Máy   | Phụ trách         |
| ------------ | ----- | ----------------- |
| Thành viên 1 | Máy 1 | Central           |
| Thành viên 2 | Máy 2 | Store A – Quận 1  |
| Thành viên 3 | Máy 3 | Store B – Quận 7  |
| Thành viên 4 | Máy 4 | Store C – Thủ Đức |

---

# 34. Nội dung cần demo

Project cần demo được tối thiểu:

```text
1. 4 máy kết nối với nhau

2. Central truy cập được 3 Site

3. Tồn kho phân mảnh theo chi nhánh

4. Hóa đơn phân mảnh theo chi nhánh

5. ChiTietHoaDon đi theo HoaDon

6. KhachHang replication giữa các Site

7. Bán hàng tại một Site

8. Sau bán hàng tồn kho tại Site đó giảm

9. Central xem được dữ liệu toàn hệ thống

10. Khi mất Central, Site vẫn thực hiện nghiệp vụ cục bộ
```

---

# 35. Luồng demo đề xuất

Ví dụ demo tại Quận 7:

```text
Kiểm tra SP001 tồn = 10
        ↓
Tạo đơn mua 2 SP001
        ↓
Tạo HoaDon
        ↓
Tạo ChiTietHoaDon
        ↓
TonKho SP001 = 8
        ↓
Central truy vấn Q7
        ↓
Thấy hóa đơn mới
```

Sau đó demo khách hàng:

```text
Tạo KH001 tại Q7
        ↓
Đồng bộ lên Central
        ↓
Q1 / Thủ Đức nhận KH001
```

Sau đó có thể ngắt Central:

```text
Ngắt kết nối Central
        ↓
Q7 vẫn bán hàng
        ↓
TonKho Q7 vẫn cập nhật
        ↓
Kết nối Central lại
        ↓
Tiếp tục đồng bộ dữ liệu cần thiết
```

---

# 36. Repository

```text
fashion-store-distributed-system
```

Đề tài:

```text
Đề tài 4 – Quản lý chuỗi cửa hàng thời trang
```

Môn học:

```text
Cơ sở dữ liệu phân tán
```
