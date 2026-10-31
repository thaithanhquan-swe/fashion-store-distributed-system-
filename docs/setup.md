# Hướng dẫn triển khai hệ thống trên 4 máy

> **Trạng thái:** bản runbook kỹ thuật, cập nhật **2026-10-30**.
> Đây là hướng dẫn thay thế nội dung cũ của `docs/setup.md`.
> Đọc kèm `docs/PROJECT_STATUS.md` (trạng thái dự án) và `docs/gap-analysis.md` (phân tích lệch mẫu giảng viên).
>
> **Các quyết định kiến trúc đã chốt** (2026-10-30): tên CSDL, mã chi nhánh, cơ chế đồng bộ
> `KhachHang`, tài khoản tại site. Xem `docs/PROJECT_STATUS.md` mục 10.1.

---

## 0. Phạm vi và giả định

Hướng dẫn này triển khai:

- **4 máy Windows** trong cùng mạng **Radmin VPN**:
  - Máy 1 – Central.
  - Máy 2 – Store A, Quận 1 (`CN1`).
  - Máy 3 – Store B, Quận 7 (`CN2`).
  - Máy 4 – Store C, Thủ Đức (`CN3`).
- **SQL Server Developer hoặc Standard** (không dùng Express), **SSMS**, **Microsoft OLE DB Driver for SQL Server**, **SQL Server Agent**.
- Backend Spring Boot chạy trên từng máy, trỏ database cục bộ.
- Frontend Vite gọi backend của máy tương ứng.

### 0.1. Bảng máy và thông tin cần điền

| Máy | Vai trò | Database | Mã CN | IP (điền) | Backend |
| --- | --- | --- | --- | --- | --- |
| 1 | Central | `Fashion_Central` | — | `<IP_CENTRAL>` | Cổng 8080 |
| 2 | Store A – Quận 1 | `Fashion_Store` | `CN1` | `<IP_Q1>` | Cổng 8080 |
| 3 | Store B – Quận 7 | `Fashion_Store` | `CN2` | `<IP_Q7>` | Cổng 8080 |
| 4 | Store C – Thủ Đức | `Fashion_Store` | `CN3` | `<IP_THUDUC>` | Cổng 8080 |

### 0.2. Quy tắc trong suốt (đã áp dụng)

| Phần | Quy ước đã chốt | Lý do |
| --- | --- | --- |
| **(a) Tên CSDL** | 3 site dùng chung `Fashion_Store`; Central là `Fashion_Central` | Một procedure viết một lần, chạy được ở mọi site |
| **(b) Tên Linked Server** | `SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`, `CENTRAL` | Câu lệnh 4 phần `SITE_Q1.Fashion_Store.dbo.SanPham` dùng được ở mọi nơi |

Đã chốt ngày 2026-10-30 (ghi tại `docs/PROJECT_STATUS.md` mục 10.1):

- 3 site: `Fashion_Store` (giống nhau); Central: `Fashion_Central`.
- Mã chi nhánh: `CN1` (Quận 1), `CN2` (Quận 7), `CN3` (Thủ Đức) — khớp bộ lọc `MACN='CN1'` trong PDF.
- `TaiKhoan` được replicate read-only xuống site, lọc theo `NhanVien`, để đăng nhập offline.

### 0.3. Mã chi nhánh (đã áp dụng)

Toàn bộ code và script dùng `CN1` / `CN2` / `CN3`: `frontend/src/config/branches.js`,
`BranchService`, `InventoryService`, `DashboardService`, `seed.sql` và các script trong `database/`.

### 0.4. ⚠️ CHỜ CHỐT – cơ chế đồng bộ `KhachHang`

`KhachHang` được tạo tại site nhưng phải nhìn thấy ở Central và các site khác.
Transactional replication chỉ chạy **một chiều** (Publisher → Subscriber), nên không đủ để đẩy khách hàng mới từ site lên Central.

Ba phương án:

| Phương án | Cách làm | Phù hợp khi |
| --- | --- | --- |
| **Merge Replication** | Central và 3 site đều là publisher/subscriber | Cần đồng bộ hai chiều, chấp nhận cấu hình phức tạp |
| **Trigger + `SyncQueue`** | Site ghi thay đổi vào queue ở Central; job Central áp dụng rồi phát lại | Cần kiểm soát xung đột, lưu được khi mất mạng |
| **Chỉ tạo khách tại Central** | Site chỉ đọc khách hàng | Đơn giản nhất nhưng không đúng nghiệp vụ bán hàng tại site |

Khuyến nghị cho đồ án: **Trigger + `SyncQueue`** (phù hợp mẫu giảng viên mục 4 và yêu cầu "lưu thay đổi khi mất mạng").
Chốt phương án trước khi làm mục 8.

---

## 1. Kiến trúc và thứ tự triển khai

```text
                         MÁY 1 – CENTRAL
                   Fashion_Central + Distributor
                              │
                         Radmin VPN
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
   MÁY 2 – Q1           MÁY 3 – Q7         MÁY 4 – THỦ ĐỨC
   Fashion_Q1           Fashion_Q7         Fashion_ThuDuc
   CN01                 CN02               CN03
```

### 1.1. Thứ tự bắt buộc

1. Chuẩn bị môi trường trên **cả 4 máy** (mục 2).
2. Tạo database + schema trên **Central trước**, sau đó 3 site (mục 3, 4, 5).
3. Tạo **Linked Server** (mục 7) — làm trước replication để kiểm tra kết nối.
4. Cấu hình **Distribution + Publication** trên Central (mục 6).
5. Tạo **Subscription** trên từng site (mục 6).
6. Cài **Trigger + job đồng bộ** (mục 8).
7. Chạy **kiểm tra nghiệm thu** (mục 9).

> Không làm ngược: nếu tạo subscription trước khi có schema giống nhau, snapshot sẽ lỗi.

---

## 2. Chuẩn bị chung trên cả 4 máy

### 2.1. Cài đặt phần mềm

Mỗi máy cài:

- SQL Server **Developer/Standard** (không dùng Express).
- SQL Server Management Studio (SSMS).
- Microsoft OLE DB Driver for SQL Server (`MSOLEDBSQL`).
- Radmin VPN.
- Git.
- Java 21 + Maven wrapper (nếu chạy backend).
- Node.js 20.19+ (nếu chạy frontend).

Kiểm tra edition:

```sql
SELECT
    @@SERVERNAME AS ServerName,
    SERVERPROPERTY('Edition') AS Edition,
    SERVERPROPERTY('ProductVersion') AS Version;
```

Kết quả phải là Developer/Standard/Enterprise, **không phải Express**.

### 2.2. Bật TCP/IP cổng 1433

Trên cả 4 máy:

1. Mở **SQL Server Configuration Manager**.
2. `SQL Server Network Configuration` → `Protocols for MSSQLSERVER` → `TCP/IP` → **Enabled**.
3. `TCP/IP` → Properties → `IP Addresses` → `IPAll`:
   - `TCP Dynamic Ports`: để trống.
   - `TCP Port`: `1433`.
4. Khởi động lại service SQL Server.

### 2.3. Mở firewall

Chạy PowerShell **Administrator** trên cả 4 máy:

```powershell
New-NetFirewallRule `
  -DisplayName "SQL Server 1433" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 1433 `
  -Action Allow
```

Kiểm tra từ máy khác:

```powershell
Test-NetConnection <IP_CENTRAL> -Port 1433
```

Kết quả cần có `TcpTestSucceeded : True`.

### 2.4. Bật Mixed Authentication và tài khoản `sa`

Trên cả 4 máy:

1. SSMS → chuột phải server → **Properties** → **Security**.
2. Chọn **SQL Server and Windows Authentication mode**.
3. Khởi động lại SQL Server.
4. `Security` → `Logins` → `sa` → **Properties**:
   - `Status` → `Enabled`.
   - Đặt mật khẩu mạnh, **không lưu vào git**.
5. Kiểm tra:

```sql
SELECT SERVERPROPERTY('IsIntegratedSecurityOnly') AS ChiWindows;
-- 0 = Mixed Mode, 1 = chỉ Windows
```

> Không dùng mật khẩu mẫu trong tài liệu. Mỗi máy đặt mật khẩu riêng hoặc dùng biến môi trường khi chạy backend.

### 2.5. Bật SQL Server Agent

Replication cần Agent chạy.

1. **SQL Server Configuration Manager** → `SQL Server Services`.
2. `SQL Server Agent (MSSQLSERVER)` → chuột phải → **Start**.
3. Properties → `Service` → `Start Mode`: **Automatic**.

Kiểm tra:

```powershell
Get-Service SQLSERVERAGENT | Select-Object Name, Status, StartType
```

Hoặc trong SQL:

```sql
EXEC xp_servicecontrol N'QUERYSTATE', N'SQLSERVERAGENT';
-- Running
```

### 2.6. Tạo snapshot folder (chỉ máy Central)

1. Tạo thư mục:

```powershell
New-Item -ItemType Directory -Path D:\ReplData -Force
```

2. Share thư mục:

```powershell
New-SmbShare -Name "REPLDATA" -Path "D:\ReplData" -FullAccess "Everyone"
```

> Trong đồ án có thể dùng `Everyone` cho dễ demo. Nếu làm thật, chỉ cấp quyền cho account chạy SQL Server Agent và account của Distributor.
> Đường dẫn snapshot sẽ là `\\CENTRAL\REPLDATA` (thay `CENTRAL` bằng tên máy thật).

3. Kiểm tra từ máy site:

```powershell
Test-Path "\\<IP_CENTRAL>\REPLDATA"
```

Phải trả về `True`.

### 2.7. Tạo login `distributed_user` trên từng site

Trên **mỗi site** (Q1, Q7, Thủ Đức), chạy trong SSMS:

```sql
USE master;
GO

IF NOT EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = 'distributed_user')
BEGIN
    CREATE LOGIN distributed_user
    WITH PASSWORD = N'<DISTRIBUTED_PASSWORD>',
         CHECK_POLICY = ON;
END
GO

USE [Fashion_Q1];   -- đổi theo site: Fashion_Q7 / Fashion_ThuDuc
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'distributed_user')
BEGIN
    CREATE USER distributed_user FOR LOGIN distributed_user;
END
GO

ALTER ROLE db_datareader ADD MEMBER distributed_user;
ALTER ROLE db_datawriter ADD MEMBER distributed_user;
GO
```

Lặp lại cho Q7 và Thủ Đức. Trên Central cũng nên tạo login này nếu site cần ghi ngược lên Central.

### 2.8. Kiểm tra kết nối chéo

Từ Central:

```powershell
Test-NetConnection <IP_Q1> -Port 1433
Test-NetConnection <IP_Q7> -Port 1433
Test-NetConnection <IP_THUDUC> -Port 1433
```

Từ từng site:

```powershell
Test-NetConnection <IP_CENTRAL> -Port 1433
```

Tất cả phải `TcpTestSucceeded : True`.

---

## 3. Tạo database và schema

### 3.1. Bộ script và thứ tự chạy

> **Thứ tự bắt buộc: linked server → database → procedure phân tán.**
> Procedure dùng tên 4 phần (`SITE_Q1.Fashion_Store.dbo.SanPham`), nên SQL Server
> kiểm tra database đích **ngay khi tạo procedure**. Vì vậy procedure được tách
> sang script riêng, chạy sau khi cả 4 database đã tồn tại.

| # | Script | Máy chạy | Chức năng |
| --- | --- | --- | --- |
| 1 | `database/linked-servers.sql` | Central + 3 site | Tạo `SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`, `CENTRAL` |
| 2 | `database/Fashion_Central.sql` | Central | DB `Fashion_Central`, 4 chi nhánh, trigger, view |
| 3 | `database/sites/Fashion_Store.sql` | 3 site | DB `Fashion_Store`, chi nhánh, trigger |
| 4 | `database/central-procs.sql` | Central | 4 procedure phân tán |
| 5 | `database/sites/site-procs.sql` | 3 site | 3 procedure kéo/đẩy dữ liệu |
| 6 | `database/seed.sql` | Central | Dữ liệu mẫu (100 SP, 300 dòng tồn kho cho CN1/CN2/CN3, 15 KH, 10 NV, 7 HĐ) |
| 7 | `database/replication.sql` | Central | Distributor + publication + subscription |

`common.sql` được `:r` tự động bởi script 2 và 3, **không chạy riêng**.

Mọi lệnh `sqlcmd` phải chạy **trong thư mục `database/`** vì `:r` phân giải
đường dẫn theo thư mục hiện tại, và phải có `-f 65001` để không lỗi tiếng Việt.

### 3.2. Trên Central

Sửa khối `CONFIG` trong `database/linked-servers.sql`: `Role = "central"`, điền IP
3 máy site, mật khẩu `distributed_user`. Sau đó:

```powershell
cd database
sqlcmd -S localhost -E -i linked-servers.sql -b -f 65001
sqlcmd -S localhost -E -i Fashion_Central.sql -b -f 65001
```

Kiểm tra:

```sql
USE [Fashion_Central];
SELECT name FROM sys.tables ORDER BY name;            -- 8 bảng
SELECT name, data_source FROM sys.servers WHERE is_linked = 1;  -- SITE_Q1, SITE_Q7, SITE_THUDUC
```

### 3.3. Trên từng site

Trong `database/linked-servers.sql` đổi `Role = "site"` và `CentralHost` = IP máy
Central. Trong `database/sites/Fashion_Store.sql` **mở đúng một khối site**
(CN1 / CN2 / CN3). Sau đó trên **mỗi máy site**:

```powershell
cd database
sqlcmd -S localhost -E -i linked-servers.sql -b -f 65001
sqlcmd -S localhost -E -i sites\Fashion_Store.sql -b -f 65001
```

Kiểm tra:

```sql
USE [Fashion_Store];
SELECT name FROM sys.tables ORDER BY name;   -- đủ 8 bảng
SELECT * FROM dbo.ChiNhanh;                  -- đúng 1 dòng = chi nhánh của máy này
```

### 3.4. Procedure phân tán (sau khi cả 4 database đã có)

```powershell
# Trên Central
sqlcmd -S localhost -E -i central-procs.sql -b -f 65001

# Trên từng site (mở đúng SiteBranch trong sites\site-procs.sql)
sqlcmd -S localhost -E -i sites\site-procs.sql -b -f 65001
```

### 3.5. Nạp dữ liệu mẫu (Central)

```powershell
sqlcmd -S localhost -E -i seed.sql -b -f 65001
```

Tài khoản demo (mật khẩu `123456`): `staff.q1`, `manager.q1`, `staff.q7`,
`manager.q7`, `staff.td`, `manager.td`, `admin`.

### 3.6. Điều kiện để replication hoạt động

- Schema 3 site phải **giống nhau** (cùng bảng, cùng cột, cùng kiểu).
- Tên database đích phải **giống tên database nguồn** (quy tắc trong suốt).
- Không có khóa ngoại chặn snapshot (hoặc phải tạo theo đúng thứ tự).
- `distributed_user` có quyền đọc/ghi trên database đích.

### 3.7. Cấu hình backend trỏ database của từng máy

> ⚠️ **Bẫy hiện tại:** `backend/src/main/resources/application.properties` **bị gitignore và không
> có trong repo**. Chỉ có `application-local.properties`, mà Spring Boot **chỉ đọc file này khi
> bật profile `local`**. Clone mới chạy `mvnw spring-boot:run` sẽ lỗi vì thiếu datasource.

Chọn một trong hai cách:

**Cách A – copy file mẫu (nhanh, khuyến nghị cho demo):**

```powershell
cd backend\src\main\resources
Copy-Item application-local.properties application.properties
# Sửa spring.datasource.url cho đúng máy:
#   Central : databaseName=Fashion_Central
#   Q1      : databaseName=Fashion_Store
#   Q7      : databaseName=Fashion_Store
#   Thủ Đức : databaseName=Fashion_Store
```

File `application.properties` đã bị gitignore nên không sợ commit nhầm.

**Cách B – dùng profile `local` + biến môi trường (không tạo file mới):**

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://localhost:1433;databaseName=Fashion_Q1;encrypt=true;trustServerCertificate=true'
$env:SPRING_DATASOURCE_USERNAME = 'sa'
$env:SPRING_DATASOURCE_PASSWORD = '<mật khẩu sa của máy này>'
$env:JWT_SIGNER_KEY = '<chuỗi base64 >= 64 byte, giống nhau trên 4 máy>'
cd backend
.\mvnw.cmd spring-boot:run
```

**Kiểm tra backend đã nối đúng DB:**

```powershell
# Phải trả JSON có code 1000
curl http://localhost:8080/api/branches
# Đăng nhập thử
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{\"username\":\"admin\",\"password\":\"123456\"}'
```

Backend tự tạo tài khoản demo khi DB trống (`staff.q1`, `manager.q1`, `admin` / `123456`).
Nếu DB trống hoàn toàn, nó cũng tự tạo chi nhánh `Local Demo Branch` mã `CN001` — **đây là lý do
phải seed dữ liệu thật** (P1-03) trước khi demo.

---

## 4. Máy 1 – Central

### 4.1. Tạo database

Xem mục 3.1.

### 4.2. Cấu hình Distributor

Làm bằng SSMS theo mẫu giảng viên:

1. Kết nối SSMS vào máy Central.
2. Chuột phải **Replication** → **Configure Distribution**.
3. Chọn **`<ServerName>` will act as its own Distributor** (dùng local Distributor).
4. Snapshot folder: `\\CENTRAL\REPLDATA`.
5. Distribution database: giữ mặc định `distribution`.
6. Finish.

Kiểm tra:

```sql
SELECT name, is_distributor FROM sys.servers;
EXEC sp_helpdistributor;
```

### 4.3. Tạo Publication

Với mô hình 3 chi nhánh, nên tạo:

- **1 publication dùng chung**: `SanPham`, `ChiNhanh` (không filter).
- **3 publication theo chi nhánh**: `Pub_CN01`, `Pub_CN02`, `Pub_CN03`, mỗi publication chứa các bảng riêng đã lọc theo `MaChiNhanh`.

> ⚠️ **CHỜ CHỐT – chiều dữ liệu của bảng riêng.**
> Cách làm trong mục này giả định **Central là nơi ghi** `TonKho`, `HoaDon`, `ChiTietHoaDon`, `NhanVien` rồi đẩy xuống site.
> Nếu nhóm chọn mô hình **site tự ghi** (đúng yêu cầu “mất Central site vẫn bán được”) thì **không** publish các bảng này từ Central.
> Xem bảng so sánh ở mục 6.5 trước khi tạo publication.

Các bảng riêng cần lọc:

| Bảng | Điều kiện lọc |
| --- | --- |
| `NhanVien` | `MaChiNhanh = 'CN01'` |
| `TonKho` | `MaChiNhanh = 'CN01'` |
| `HoaDon` | `MaChiNhanh = 'CN01'` |
| `ChiTietHoaDon` | Join dẫn xuất theo `HoaDon` |

Các bước wizard:

1. Chuột phải **Local Publications** → **New Publication**.
2. Chọn database `Fashion_Central` (hoặc tên đã chốt).
3. Chọn **Transactional publication**.
4. Chọn articles:
   - Bỏ chọn `sysdiagrams`.
   - Chọn bảng cần replicate.
5. Với bảng riêng: **Add Filter** → chọn cột `MaChiNhanh` → điều kiện `= 'CN01'`.
6. Với `ChiTietHoaDon`: **Add Join to Extend the Selected Filter** → join `ChiTietHoaDon.MaHoaDon = HoaDon.MaHoaDon`.
7. Đặt tên publication (`Pub_CN01`).
8. Chọn **Create a snapshot immediately** và **Start the Snapshot Agent**.
9. Finish.

Lặp lại cho `CN02`, `CN03` (đổi điều kiện filter).

> Nếu muốn dùng T-SQL thay wizard, xem Phụ lục A. Nên dùng wizard cho lần đầu để tránh sai option.

### 4.4. Kiểm tra Snapshot Agent

Trong SSMS:

- `Replication` → `Local Publications` → chuột phải publication → **View Snapshot Agent Status**.
- Hoặc **Replication Monitor** → `Publishers` → chọn Central → `Publications`.

Trạng thái phải là **Succeeded** hoặc đang chạy không lỗi.

Kiểm tra trong SQL:

```sql
USE distribution;
SELECT publisher_db, publication, status, snapshot_ready
FROM dbo.MSpublications;
```

### 4.5. Tạo Linked Server tới 3 site

Chạy trên Central (mục 7.1).

---

## 5. Máy 2, 3, 4 – Site

Làm lần lượt trên từng site.

### 5.1. Tạo database và schema

Xem mục 3.2.

### 5.2. Tạo `distributed_user`

Xem mục 2.7.

### 5.3. Tạo Linked Server `CENTRAL`

Chạy trên từng site (mục 7.2).

### 5.4. Tạo Subscription

Trên Central:

1. `Replication` → `Local Publications` → chuột phải `Pub_CN01` → **New Subscriptions**.
2. Chọn **Push subscription** (hoặc Pull nếu muốn site tự kéo).
3. **Add SQL Server Subscriber** → nhập tên/IP máy Q1, user `distributed_user`.
4. Chọn database đích: `Fashion_Q1` (phải trùng tên database nguồn).
5. Chọn **Run the snapshot agent immediately**.
6. Finish.

Lặp lại cho `Pub_CN02` → Q7, `Pub_CN03` → Thủ Đức.

Kiểm tra trên Central:

```sql
USE distribution;
SELECT s.name AS subscriber, a.publication, a.status
FROM dbo.MSsubscriptions a
JOIN dbo.MSsubscriber_info s ON s.subscriber_id = a.subscriber_id;
```

### 5.5. Kiểm tra dữ liệu đã về site

Trên Q1:

```sql
USE [Fashion_Q1];
SELECT TOP 10 * FROM dbo.TonKho WHERE MaChiNhanh = 'CN01';
SELECT TOP 10 * FROM dbo.HoaDon WHERE MaChiNhanh = 'CN01';
```

Trên Q7 / Thủ Đức tương tự với `CN02` / `CN03`.

---

## 6. Cấu hình Replication chi tiết

### 6.1. Mô hình đề xuất

```text
Central (Publisher + Distributor)
   │
   ├── Pub_Common  ──►  Q1, Q7, Thủ Đức   (SanPham, ChiNhanh)
   ├── Pub_CN01    ──►  Q1                 (NhanVien, TonKho, HoaDon, ChiTietHoaDon lọc CN01)
   ├── Pub_CN02    ──►  Q7                 (lọc CN02)
   └── Pub_CN03    ──►  Thủ Đức            (lọc CN03)
```

### 6.2. Loại replication

| Loại | Dùng cho | Ghi chú |
| --- | --- | --- |
| **Transactional** | Bảng chung Central → site; bảng riêng Central → đúng site | Phù hợp dữ liệu ít sửa, cần nhất quán |
| **Merge** | `KhachHang` nếu chọn đồng bộ hai chiều | Cần resolver xung đột |
| **Trigger + queue** | `KhachHang`, `NhanVien` nếu cần đẩy từ site lên Central | Linh hoạt, lưu được khi mất mạng |

### 6.3. Lưu ý về `KhachHang`

Nếu chỉ dùng transactional replication một chiều:

- Khách tạo tại Q7 **sẽ không tự lên Central**.
- Central không thấy khách mới → báo cáo thiếu.
- Q1/Thủ Đức không nhận được khách của Q7.

Vì vậy phải chọn một trong ba phương án ở mục 0.4.
**Không được** cấu hình cả transactional replication và trigger cho cùng một luồng dữ liệu — sẽ gây trùng.

### 6.4. Thứ tự tạo article

Để tránh lỗi khóa ngoại:

1. `ChiNhanh`
2. `SanPham`
3. `NhanVien`
4. `KhachHang`
5. `TonKho`
6. `HoaDon`
7. `ChiTietHoaDon`
8. `TaiKhoan` (nếu replicate)

### 6.5. ⚠️ CHỜ CHỐT – chiều đồng bộ bảng riêng

Bảng riêng (`TonKho`, `HoaDon`, `ChiTietHoaDon`, `NhanVien`) có hai cách triển khai **khác nhau hoàn toàn**. Phải chọn một trước khi tạo publication.

| Mô hình | Ghi ở đâu | Đồng bộ thế nào | Mất Central site còn bán? |
| --- | --- | --- | --- |
| **A. Central là nguồn ghi** | Central | Transactional replication, filter `MaChiNhanh`, đẩy xuống site | ❌ Không |
| **B. Site là nguồn ghi** (khuyến nghị) | Site | Trigger + `SyncQueue` đẩy lên Central; Central báo cáo bằng linked server | ✅ Có |

**Nếu chọn A:**

- Làm đúng mục 4.3 và 5.4 như đang viết: `Pub_CN01/02/03` chứa bảng riêng đã lọc.
- Site chỉ đọc, không ghi trực tiếp các bảng đó.
- Không đáp ứng tiêu chí demo số 10 (mất Central vẫn bán).

**Nếu chọn B:**

- `Pub_Common` chỉ chứa bảng dùng chung: `SanPham`, `ChiNhanh` (và `KhachHang` nếu chọn merge replication).
- **Không** tạo `Pub_CN01/02/03` cho `TonKho`, `HoaDon`, `ChiTietHoaDon`, `NhanVien`.
- Site ghi cục bộ; trigger đẩy thay đổi lên Central (mục 8.2).
- Central tổng hợp bằng `UNION ALL` qua linked server (mục 9.4) hoặc bảng tổng hợp nhận từ queue.
- Các bước 5.4 (subscription) chỉ áp dụng cho `Pub_Common`.

> Ghi rõ lựa chọn vào `docs/PROJECT_STATUS.md` mục 10 trước khi chạy.
> Không trộn hai mô hình cho cùng một bảng — sẽ tạo dữ liệu trùng hoặc ghi đè lẫn nhau.

---

## 7. Linked Server

### 7.1. Trên Central → 3 site

Chạy trên Central:

```sql
USE master;
GO

-- Q1
IF EXISTS (SELECT 1 FROM sys.servers WHERE name = N'SITE_Q1')
    EXEC sp_dropserver @server = N'SITE_Q1', @droplogins = 'droplogins';
GO
EXEC sp_addlinkedserver
    @server     = N'SITE_Q1',
    @srvproduct = N'',
    @provider   = N'MSOLEDBSQL',
    @datasrc    = N'<IP_Q1>,1433',
    @provstr    = N'Encrypt=Optional;TrustServerCertificate=Yes';
GO
EXEC sp_addlinkedsrvlogin
    @rmtsrvname  = N'SITE_Q1',
    @useself     = N'False',
    @locallogin  = NULL,
    @rmtuser     = N'distributed_user',
    @rmtpassword = N'<DISTRIBUTED_PASSWORD>';
GO

-- Lặp lại cho SITE_Q7, SITE_THUDUC
```

### 7.2. Trên từng site → Central

Chạy trên Q1/Q7/Thủ Đức:

```sql
USE master;
GO

IF EXISTS (SELECT 1 FROM sys.servers WHERE name = N'CENTRAL')
    EXEC sp_dropserver @server = N'CENTRAL', @droplogins = 'droplogins';
GO
EXEC sp_addlinkedserver
    @server     = N'CENTRAL',
    @srvproduct = N'',
    @provider   = N'MSOLEDBSQL',
    @datasrc    = N'<IP_CENTRAL>,1433',
    @provstr    = N'Encrypt=Optional;TrustServerCertificate=Yes';
GO
EXEC sp_addlinkedsrvlogin
    @rmtsrvname  = N'CENTRAL',
    @useself     = N'False',
    @locallogin  = NULL,
    @rmtuser     = N'distributed_user',
    @rmtpassword = N'<DISTRIBUTED_PASSWORD>';
GO
```

### 7.3. Kiểm tra

```sql
EXEC sp_testlinkedserver N'SITE_Q1';
EXEC sp_testlinkedserver N'SITE_Q7';
EXEC sp_testlinkedserver N'SITE_THUDUC';
```

Trên site:

```sql
EXEC sp_testlinkedserver N'CENTRAL';
```

Truy vấn thử:

```sql
SELECT TOP 5 * FROM SITE_Q1.[Fashion_Q1].dbo.TonKho;
SELECT TOP 5 * FROM CENTRAL.[Fashion_Central].dbo.KhachHang;
```

> Nếu chọn phương án đổi tên CSDL chung, thay `[Fashion_Q1]` bằng tên đã chốt.

---

## 8. Trigger và đồng bộ dữ liệu

> Phần này chỉ làm sau khi chốt phương án ở mục 0.4.

### 8.1. Bảng queue ở Central

```sql
USE [Fashion_Central];
GO

IF OBJECT_ID(N'dbo.SyncQueue', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SyncQueue (
        QueueId      BIGINT IDENTITY(1,1) PRIMARY KEY,
        SourceSite   VARCHAR(10)  NOT NULL,
        TableName    VARCHAR(50)  NOT NULL,
        RecordKey    VARCHAR(50)  NOT NULL,
        Operation    VARCHAR(10)  NOT NULL,   -- INSERT / UPDATE / DELETE
        Payload      NVARCHAR(MAX) NULL,
        CreatedAt    DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
        ProcessedAt  DATETIME2    NULL,
        RetryCount   INT          NOT NULL DEFAULT 0,
        LastError    NVARCHAR(4000) NULL
    );
END
GO
```

### 8.2. Trigger đẩy thay đổi từ site lên Central

Ví dụ trên Q1, bảng `KhachHang`:

```sql
USE [Fashion_Q1];
GO

CREATE OR ALTER TRIGGER dbo.trg_KhachHang_Sync
ON dbo.KhachHang
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO CENTRAL.[Fashion_Central].dbo.SyncQueue
        (SourceSite, TableName, RecordKey, Operation, Payload)
    SELECT
        'CN01',
        'KhachHang',
        i.MaKhachHang,
        CASE WHEN d.MaKhachHang IS NULL THEN 'INSERT' ELSE 'UPDATE' END,
        (SELECT i.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
    FROM inserted i
    LEFT JOIN deleted d ON d.MaKhachHang = i.MaKhachHang;
END
GO
```

Làm tương tự cho `NhanVien` nếu cần.

> Trigger chạy trong transaction của lệnh gốc. Nếu mất kết nối Central, lệnh bán hàng có thể bị chặn.
> Vì vậy cần cân nhắc: hoặc dùng queue cục bộ rồi job đẩy sau, hoặc chấp nhận ghi trực tiếp khi có mạng.

### 8.3. Job xử lý queue ở Central

Tạo SQL Server Agent Job chạy mỗi 1–5 phút:

1. Đọc các dòng `ProcessedAt IS NULL`.
2. Áp dụng vào bảng đích.
3. Đánh dấu `ProcessedAt`.
4. Nếu lỗi: tăng `RetryCount`, ghi `LastError`, thử lại sau.

### 8.4. Trigger kiểm soát xóa/sửa ở Central

Ví dụ chặn xóa sản phẩm đang được tham chiếu:

```sql
USE [Fashion_Central];
GO

CREATE OR ALTER TRIGGER dbo.trg_SanPham_DeleteGuard
ON dbo.SanPham
AFTER DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM deleted d
        JOIN dbo.TonKho t ON t.MaSanPham = d.MaSanPham
    )
    OR EXISTS (
        SELECT 1
        FROM deleted d
        JOIN dbo.ChiTietHoaDon c ON c.MaSanPham = d.MaSanPham
    )
    BEGIN
        RAISERROR(N'Không thể xóa sản phẩm đang được tham chiếu trong TonKho hoặc ChiTietHoaDon.', 16, 1);
        ROLLBACK TRANSACTION;
    END
END
GO
```

---

## 9. Kiểm tra nghiệm thu

### 9.1. Theo từng máy

| # | Kiểm tra | Lệnh | Kết quả mong đợi |
| --- | --- | --- | --- |
| 1 | SQL Server chạy | `SELECT @@SERVERNAME` | Đúng tên máy |
| 2 | Không phải Express | `SELECT SERVERPROPERTY('Edition')` | Developer/Standard/Enterprise |
| 3 | Mixed auth | `SELECT SERVERPROPERTY('IsIntegratedSecurityOnly')` | `0` |
| 4 | Agent chạy | `EXEC xp_servicecontrol N'QUERYSTATE', N'SQLSERVERAGENT'` | `Running` |
| 5 | Port 1433 | `Test-NetConnection <IP> -Port 1433` | `True` |
| 6 | Snapshot folder | `Test-Path \\<IP_CENTRAL>\REPLDATA` | `True` |
| 7 | `distributed_user` | `SELECT name FROM sys.database_principals WHERE name='distributed_user'` | Có 1 dòng |
| 8 | Linked Server | `EXEC sp_testlinkedserver N'...'` | Không lỗi |
| 9 | Publication | `EXEC sp_helppublication` | Có publication |
| 10 | Subscription | Replication Monitor | Status = Running/Succeeded |

### 9.2. Kiểm tra phân mảnh

Trên Q1:

```sql
SELECT DISTINCT MaChiNhanh FROM dbo.TonKho;
-- Chỉ CN01
SELECT DISTINCT MaChiNhanh FROM dbo.HoaDon;
-- Chỉ CN01
```

Trên Q7: chỉ `CN02`. Trên Thủ Đức: chỉ `CN03`.

### 9.3. Kiểm tra bán hàng và tồn kho

Kịch bản mẫu:

1. Trên Q7, kiểm tra `SP001` tồn = 10.
2. Bán 2 `SP001`.
3. Kiểm tra:
   - Có đúng 1 `HoaDon` mới.
   - Có 2 dòng `ChiTietHoaDon`.
   - `TonKho` còn 8.
   - Q1 và Thủ Đức không đổi.

### 9.4. Kiểm tra truy vấn phân tán

Trên Central:

```sql
SELECT 'Q1' AS Site, * FROM SITE_Q1.[Fashion_Q1].dbo.TonKho
UNION ALL
SELECT 'Q7', * FROM SITE_Q7.[Fashion_Q7].dbo.TonKho
UNION ALL
SELECT 'TD', * FROM SITE_THUDUC.[Fashion_ThuDuc].dbo.TonKho;
```

### 9.5. Kiểm tra mất Central

1. Tắt SQL Server hoặc ngắt mạng máy Central.
2. Trên Q7, bán hàng bình thường.
3. Kiểm tra tồn Q7 giảm, hóa đơn được tạo.
4. Bật lại Central.
5. Kiểm tra dữ liệu được đồng bộ (theo cơ chế đã chọn).

---

## 10. Troubleshooting

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
| --- | --- | --- |
| `Login failed for user 'distributed_user'` | Chưa tạo login/user hoặc sai mật khẩu | Chạy lại mục 2.7, kiểm tra `sys.sql_logins` |
| `Cannot open database "Fashion_Q1"` | Tên DB không khớp hoặc chưa tạo | Tạo đúng tên; kiểm tra `sys.databases` |
| `The server is not configured as a Distributor` | Chưa Configure Distribution | Chạy mục 4.2 |
| `The process could not connect to Distributor` | Agent chưa chạy, firewall, sai account | Bật Agent, mở port, kiểm tra login |
| `Snapshot folder is not accessible` | Chưa share hoặc thiếu quyền | Share `\\CENTRAL\REPLDATA`, cấp Read/Write |
| `The subscription is not initialized` | Snapshot chưa chạy xong | Start Snapshot Agent, xem Replication Monitor |
| `Cannot drop server ... because it is used` | Linked server đang được dùng | Xóa subscription/publication trước |
| `Replication schema mismatch` | Schema 3 site không giống nhau | So sánh bảng/cột, sửa schema |
| `Conflict` khi đồng bộ khách hàng | Hai site sửa cùng bản ghi | Áp dụng quy tắc `LastModified` / resolver |
| Backend báo `Cannot open database` | `application.properties` trỏ sai DB hoặc file không tồn tại | Làm mục 3.4: copy `application-local.properties` → `application.properties` rồi sửa `spring.datasource.url`, hoặc bật profile `local` + `SPRING_DATASOURCE_URL` |
| Backend start nhưng báo thiếu datasource / `Failed to configure a DataSource` | Không có `application.properties` và không bật profile `local` | Làm mục 3.4 |
| Frontend báo CORS | Backend chưa cho phép origin | Đặt `app.cors.allowed-origins=http://localhost:5173` |
| `JWT signer key must be at least 64 bytes` | Chưa cấu hình khóa | Đặt `JWT_SIGNER_KEY` dài ≥ 64 bytes, giống nhau trên 4 máy |
| Dữ liệu chi nhánh lọc sai / không thấy gì | Mã CN lệch (`CN001` vs `CN01`) | Chốt mục 0.3, sửa `config/branches.js` + alias ở backend |

---

## 11. Rollback / reset

> Chỉ làm khi demo lỗi và cần dựng lại. Thao tác này xóa dữ liệu.

Thứ tự an toàn:

1. Trên Central: xóa subscription.
2. Trên Central: xóa publication.
3. Trên Central: tắt publishing/distribution nếu cần.
4. Trên từng site: xóa database (nếu muốn làm lại).
5. Trên Central: xóa database.
6. Chạy lại từ mục 2.

Lệnh tham khảo:

```sql
-- Xóa subscription
EXEC sp_dropsubscription
    @publication = N'Pub_CN01',
    @subscriber = N'<SUBSCRIBER_SERVER>',
    @destination_db = N'Fashion_Q1';

-- Xóa publication
EXEC sp_droppublication @publication = N'Pub_CN01';
```

> Không xóa `distribution` database khi chưa chắc chắn; có thể ảnh hưởng các publication khác.

---

## 12. Checklist theo máy

### Máy 1 – Central

- [ ] SQL Server Developer/Standard, TCP/IP 1433, firewall
- [ ] Mixed auth, `sa` bật
- [ ] SQL Server Agent chạy tự động
- [ ] Snapshot folder `\\CENTRAL\REPLDATA` đã share
- [ ] Database `Fashion_Central` đã tạo
- [ ] `distributed_user` đã tạo
- [ ] Configure Distribution xong
- [ ] Publication `Pub_Common`, `Pub_CN01`, `Pub_CN02`, `Pub_CN03`
- [ ] Linked Server `SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`
- [ ] `sp_testlinkedserver` OK
- [ ] Snapshot Agent chạy thành công

### Máy 2 – Q1

- [ ] SQL Server Developer/Standard, TCP/IP 1433, firewall
- [ ] Mixed auth
- [ ] Database `Fashion_Q1` + 8 bảng
- [ ] `distributed_user` + quyền đọc/ghi
- [ ] Linked Server `CENTRAL`
- [ ] Subscription `Pub_CN01` / `Pub_Common`
- [ ] Dữ liệu chỉ có `CN01`
- [ ] Bán hàng thử và kiểm tra tồn

### Máy 3 – Q7

- [ ] SQL Server Developer/Standard, TCP/IP 1433, firewall
- [ ] Mixed auth
- [ ] Database `Fashion_Q7` + 8 bảng
- [ ] `distributed_user` + quyền đọc/ghi
- [ ] Linked Server `CENTRAL`
- [ ] Subscription `Pub_CN02` / `Pub_Common`
- [ ] Dữ liệu chỉ có `CN02`
- [ ] Bán hàng thử và kiểm tra tồn

### Máy 4 – Thủ Đức

- [ ] SQL Server Developer/Standard, TCP/IP 1433, firewall
- [ ] Mixed auth
- [ ] Database `Fashion_ThuDuc` (sửa từ `Fashion_TD`) + 8 bảng
- [ ] `distributed_user` + quyền đọc/ghi
- [ ] Linked Server `CENTRAL`
- [ ] Subscription `Pub_CN03` / `Pub_Common`
- [ ] Dữ liệu chỉ có `CN03`
- [ ] Bán hàng thử và kiểm tra tồn

---

## Phụ lục A – Script T-SQL tham khảo cho Publication

> Chỉ dùng khi đã hiểu rõ; wizard an toàn hơn cho lần đầu.

```sql
USE [Fashion_Central];
GO

-- Bật publishing cho database
EXEC sp_replicationdboption
    @dbname = N'Fashion_Central',
    @optname = N'publish',
    @value = N'true';
GO

-- Tạo publication
EXEC sp_addpublication
    @publication = N'Pub_CN01',
    @status = N'active',
    @allow_push = N'true',
    @allow_pull = N'true',
    @repl_freq = N'continuous',
    @independent_agent = N'true';
GO

-- Thêm article đã lọc
EXEC sp_addarticle
    @publication = N'Pub_CN01',
    @article = N'TonKho',
    @source_object = N'TonKho',
    @type = N'logbased',
    @schema_option = 0x000000000803509F,
    @filter_clause = N'MaChiNhanh = ''CN01''';
GO

-- Thêm article dẫn xuất (lọc theo HoaDon)
EXEC sp_addarticle
    @publication = N'Pub_CN01',
    @article = N'ChiTietHoaDon',
    @source_object = N'ChiTietHoaDon',
    @type = N'logbased',
    @schema_option = 0x000000000803509F;
GO

EXEC sp_addarticle
    @publication = N'Pub_CN01',
    @article = N'HoaDon',
    @source_object = N'HoaDon',
    @type = N'logbased',
    @schema_option = 0x000000000803509F,
    @filter_clause = N'MaChiNhanh = ''CN01''';
GO

-- Tạo snapshot agent
EXEC sp_addpublication_snapshot
    @publication = N'Pub_CN01',
    @frequency_type = 1;
GO
```

> Script trên chỉ là khung. Cần kiểm tra `@schema_option`, thứ tự article và join filter theo đúng schema thật.
> Với `ChiTietHoaDon`, phải dùng **join filter** trỏ tới `HoaDon`, không chỉ `@filter_clause`.

---

## Phụ lục B – Liên kết tài liệu

- `docs/PROJECT_STATUS.md` – trạng thái, backlog, thứ tự làm.
- `docs/gap-analysis.md` – phân tích lệch so với mẫu giảng viên.
- `docs/architecture.md` – kiến trúc tổng quan (đã cập nhật 2026-10-29).
- `README.md` – mô tả đề tài, mục 17–29 là bản cũ của hướng dẫn này.
- `backend/HUONG_DAN_CHAY_BACKEND.md` – chạy backend trên từng máy (⚠️ còn hướng dẫn cũ về `application.properties`, xem mục 3.4).

---

## Phụ lục C – Việc cần làm trước khi chạy thật

1. [x] Chốt tên CSDL: `Fashion_Store` cho 3 site, `Fashion_Central` cho Central (mục 0.2).
2. [x] Chốt **mã chi nhánh** `CN1`/`CN2`/`CN3` (mục 0.3) và đồng bộ `config/branches.js` + backend.
3. [x] Bỏ `UNIQUE(SoDienThoai)` trên `KhachHang`, thay bằng `IX_KhachHang_SoDienThoai` (D10).
4. [x] Thêm cột `LastModified` trên các bảng phục vụ đồng bộ / giải quyết xung đột (D11).
5. [x] Tạo `database/common.sql`, `Fashion_Central.sql`, `sites/Fashion_Store.sql`.
6. [x] Thêm `distributed_user` vào script khởi tạo Central và các site.
7. [x] Sửa `sp_DongBoSanPham` dùng đúng linked server `SITE_Q1`, `SITE_Q7`, `SITE_THUDUC` và đồng bộ đủ cột + `LastModified`.
8. [x] Tạo `database/linked-servers.sql`, `central-procs.sql`, `sites/site-procs.sql`, `replication.sql`.
9. [x] Tạo `seed.sql` mở rộng: 100 sản phẩm, 300 dòng tồn kho cho CN1/CN2/CN3, 15 KH, 10 NV, 7 HĐ.
10. [x] Chuyển file SQL sang UTF-8, thêm `.gitattributes`.
11. [ ] Chốt cơ chế đồng bộ `KhachHang`: ưu tiên Merge Replication (theo file PDF bài giảng giảng viên) hoặc Transactional + SyncQueue (mục 0.4).
12. [ ] Cấu hình backend từng máy theo mục 3.4 (copy `application-local.properties` hoặc dùng profile `local` + biến môi trường).
13. [ ] Triển khai thực tế trên 4 máy Radmin VPN và chạy nghiệm thu theo mục 9.
