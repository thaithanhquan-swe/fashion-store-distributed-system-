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

> **Instance SQL Server dùng chung cho cả 4 máy:** named instance **`FASHION`**, cổng TCP
> cố định **`14333`**. Kết nối nội bộ: `localhost\FASHION` (hoặc `localhost,14333`).
> Kết nối qua mạng: `<IP>,14333`. Chi tiết và lý do ở mục 2.2 và 2.2.1.

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

### 0.4. ✅ ĐÃ CHỐT – Replication (Merge) và đối chiếu yêu cầu tối thiểu của đề tài

Đã chốt phương án **Merge Replication** ngày 2026-10-31 (theo tài liệu giảng dạy của TS. Phan Thị Hà - HVCNBCVT trong file `[6][4]TONG HOP TOM TAT CAI DAT CSDL PT - QUAN LY THUC THI GIAO DICH- SV.pdf`).

**Lý do lựa chọn và ưu điểm:**
1. **Khớp 100% mẫu giảng viên:** Tài liệu bài giảng hướng dẫn trực quan bằng SSMS Wizard tạo `Merge publication`, thiết lập bộ lọc ngang nguyên thủy (`CHINHANH WHERE [MACN] = 'CN1'`) và dẫn xuất (`Add Join to Extend the Selected Filter` cho `NHANVIEN`, `TONKHO`, `HOADON`, `CHITIETHOADON`).
2. **Đồng bộ hai chiều / đa chiều cho `KhachHang`:** Bất kỳ chi nhánh nào tạo hoặc cập nhật thông tin khách hàng, dữ liệu đều tự động merge về Central và phát tán sang các chi nhánh khác mà không cần viết trigger thủ công.
3. **Hoạt động offline khi mất kết nối:** Máy trạm chi nhánh vẫn bán hàng và cập nhật tồn kho cục bộ khi đứt mạng Radmin VPN hoặc Central bảo trì; khi kết nối mạng phục hồi, Merge Agent tự động đồng bộ thay đổi về Central.
4. **Hòa giải xung đột linh hoạt (Conflict Resolution):** Sử dụng cơ chế phân quyền độ ưu tiên (Subscriber Priority 75.00, Publisher 100.00) kết hợp cột thời gian `LastModified` và procedure đối soát để đảm bảo dữ liệu không bị ghi đè sai lệch.

#### 0.4.1. Đề tài yêu cầu tối thiểu 5 hạng mục — không được bỏ hạng mục nào

Mỗi nhóm phải **chứng minh** tối thiểu: **1 phương pháp phân mảnh + 1 phương pháp replication
+ 1 distributed transaction + 1 tình huống concurrency + 1 distributed query**.

| # | Hạng mục bắt buộc | Đồ án dùng | Chứng minh ở đâu |
| --- | --- | --- | --- |
| 1 | **Phân mảnh** | Ngang **nguyên thủy** (`CHINHANH` lọc `MACN='CN1'`) + ngang **dẫn xuất** (join `NHANVIEN`, `TONKHO`, `HOADON`, `CHITIETHOADON`) | Mục 4.3, 6.4, 9.2 |
| 2 | **Replication** | **Merge publication** `Pub_Fashion_Store_Q1/Q7/ThuDuc` + publication dùng chung | Mục 4.3, 5.4, 6 |
| 3 | **Distributed transaction** | `BEGIN DISTRIBUTED TRANSACTION` qua linked server (2PC/MSDTC) | **Mục 8.5** |
| 4 | **Concurrency** | 2 phiên cùng sửa `TonKho` + conflict resolver của Merge | **Mục 8.6** |
| 5 | **Distributed query** | Truy vấn 4 phần `UNION ALL` qua 3 linked server | Mục 9.4 |

> [!IMPORTANT]
> **Merge Replication KHÔNG thay thế Distributed Transaction.**
> - **Replication** là đồng bộ **bất đồng bộ**: mỗi site ghi cục bộ, Merge Agent chạy sau và
>   hợp nhất thay đổi. Không có tính nguyên tử (atomic) giữa các site.
> - **Distributed transaction** là **một** transaction trải trên ≥ 2 server, commit/rollback
>   đồng thời theo giao thức 2 pha (2PC) qua MSDTC. Replication không cung cấp điều này.
>
> Vì vậy dùng Merge **không lệch đề tài** (đề bài không bắt buộc loại replication nào), nhưng
> **vẫn phải làm thêm mục 8.5** thì mới đủ "1 distributed transaction".

> [!NOTE]
> **Trạng thái code hiện tại:** `database/replication.sql` đang dựng **transactional** replication,
> chưa phải Merge. Nếu giữ quyết định Merge thì phải sửa script theo Merge publication
> (xem thêm mục 6.5 về chiều ghi của bảng riêng). Đừng trộn cả hai cho cùng một bảng.

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
   Fashion_Store        Fashion_Store      Fashion_Store
   CN1                  CN2                CN3
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

### 2.2. Bật TCP/IP và cấu hình Port — instance `FASHION`, cổng `14333`

> **Cấu hình thực tế của nhóm (đã chốt):** mỗi máy chạy **song song 2 instance**:
>
> | Instance | Tên service | Cổng TCP | Ghi chú |
> | --- | --- | --- | --- |
> | Instance mặc định | `MSSQLSERVER` | `1433` | Không dùng cho đồ án |
> | Named instance | `FASHION` | `14333` | **Instance dùng cho toàn bộ đồ án** |
>
> Hệ quả: **mọi** kết nối từ xa (SSMS, `sqlcmd`, linked server, JDBC) phải chỉ định
> `14333`, hoặc dùng tên instance kèm SQL Browser (mục 2.2.1). Đừng dùng `1433` —
> cổng đó đang thuộc instance mặc định.

Trên cả 4 máy:

1. Mở **SQL Server Configuration Manager**.
2. `SQL Server Network Configuration` → `Protocols for FASHION` → `TCP/IP` → **Enabled**.
3. Chuột phải `TCP/IP` → **Properties** → tab **IP Addresses** → cuộn xuống **IPAll**:
   - `TCP Dynamic Ports`: **xóa trắng hoàn toàn** (không để `0`).
   - `TCP Port`: `14333`.
4. Khởi động lại service **`SQL Server (FASHION)`**.
5. Kiểm tra cổng đã mở đúng:

```powershell
netstat -ano | findstr :14333
# Phải thấy dòng LISTENING của tiến trình sqlservr.exe
```

### 2.2.1. `<IP>\FASHION` khác gì `<IP>,14333` — và vì sao cần UDP 1434?

Đây là điểm hay nhầm nhất khi dùng named instance.

**Nguyên tắc:** named instance **không** có cổng cố định mà client tự biết. Khi bạn gõ
`<IP>\FASHION`, client phải **hỏi** dịch vụ **SQL Server Browser** trên máy đích:
"instance FASHION đang ở cổng nào?" — câu hỏi đó đi qua **UDP 1434**. Browser trả về
cổng TCP thật (ở đây là `14333`), rồi client mới mở kết nối TCP tới cổng đó.

Vì vậy:

| Cách gõ | Cần SQL Browser (UDP 1434)? | Dùng khi nào |
| --- | --- | --- |
| `<IP>,14333` | **Không** | **Khuyến nghị** cho mọi kết nối qua mạng: linked server, JDBC, SSMS từ máy khác. Nhanh, ổn định, không phụ thuộc service phụ. |
| `<IP>\FASHION` | **Có** (Browser đang chạy + firewall mở UDP 1434 inbound) | Khi muốn gõ theo tên cho dễ nhớ, hoặc cổng bị đổi động. |
| `localhost\FASHION` | **Không** | Chỉ khi client ở **cùng máy**. Kết nối nội bộ đi qua Shared Memory / Named Pipes, không qua TCP nên không cần Browser. |
| `localhost` hoặc `.` | Không | Trỏ tới **instance mặc định**, **không phải** `FASHION`. |

> [!IMPORTANT]
> - `localhost` chỉ có nghĩa "máy đang chạy client". Từ máy khác, `localhost\FASHION` sẽ
>   trỏ nhầm vào chính máy đó → phải dùng `<IP>\FASHION` hoặc `<IP>,14333`.
> - Trong SSMS trên **cùng máy**, `localhost\FASHION` **đúng** và tiện nhất — giống hệt
>   cách bạn đang dùng. Chỉ khi qua mạng mới phải thay `localhost` bằng IP.
> - Nếu bật Browser: service tên là **`SQL Server Browser`**, chạy chung cho **mọi**
>   instance trên máy, không phải `SQL Server Browser (FASHION)`.

### 2.3. Mở firewall

Chạy PowerShell **Administrator** trên cả 4 máy:

```powershell
# Bắt buộc: cổng TCP của instance FASHION
New-NetFirewallRule `
  -DisplayName "SQL Server FASHION 14333" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 14333 `
  -Action Allow
```

*(Nếu vẫn muốn dùng instance mặc định thì mở thêm `-LocalPort 1433`. Đừng mở 1433 rồi
trỏ nhầm vào đó — 1433 là instance mặc định, không phải `FASHION`.)*

**Chỉ khi muốn kết nối theo tên `<IP>\FASHION`** — mở thêm UDP 1434 cho SQL Server Browser:

```powershell
New-NetFirewallRule `
  -DisplayName "SQL Server Browser 1434" `
  -Direction Inbound `
  -Protocol UDP `
  -LocalPort 1434 `
  -Action Allow

# Browser phải đang chạy thì UDP 1434 mới có tác dụng
Set-Service 'SQLBrowser' -StartupType Automatic
Start-Service 'SQLBrowser'
Get-Service 'SQLBrowser' | Select-Object Name, Status, StartType
```

> Nếu dùng `<IP>,14333` (khuyến nghị) thì **không cần** UDP 1434 và **không cần** bật
> SQL Server Browser. Đây cũng là lý do các script linked server dùng `,14333`.

Kiểm tra từ máy khác:

```powershell
Test-NetConnection <IP_CENTRAL> -Port 14333
```

Kết quả cần có `TcpTestSucceeded : True`.

### 2.4. Bật Mixed Authentication và tài khoản `sa`

Trên cả 4 máy:

1. SSMS → chuột phải server (ví dụ `localhost\FASHION`) → **Properties** → **Security**.
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

Replication cần SQL Server Agent chạy để thực thi Snapshot Agent, Log Reader Agent, Merge Agent.

1. **SQL Server Configuration Manager** → `SQL Server Services`.
2. Tìm service Agent tương ứng với instance:
   - **Instance mặc định:** `SQL Server Agent (MSSQLSERVER)`
   - **Named Instance `FASHION`:** `SQL Server Agent (FASHION)`
3. Chuột phải → **Start**.
4. Chuột phải → Properties → tab `Service` → `Start Mode`: chọn **Automatic** (để tự chạy khi bật máy).

**Kiểm tra bằng PowerShell:**

```powershell
# Với instance mặc định (MSSQLSERVER):
Get-Service SQLSERVERAGENT | Select-Object Name, Status, StartType

# Với Named Instance (FASHION):
# Lưu ý: Bọc trong nháy đơn để PowerShell không hiểu $FASHION là biến
Get-Service 'SQLAgent$FASHION' | Select-Object Name, Status, StartType
Start-Service 'SQLAgent$FASHION'
Set-Service 'SQLAgent$FASHION' -StartupType Automatic
```

**Hoặc kiểm tra trong SSMS qua T-SQL:**

```sql
-- Với instance mặc định:
EXEC xp_servicecontrol N'QUERYSTATE', N'SQLSERVERAGENT';

-- Với Named Instance FASHION:
EXEC xp_servicecontrol N'QUERYSTATE', N'SQLAgent$FASHION';
-- Kết quả trả về: Current State = 'Running.'
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

USE [Fashion_Store];   -- giống nhau trên cả 3 site (quy tắc trong suốt)
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
Test-NetConnection <IP_Q1> -Port 14333
Test-NetConnection <IP_Q7> -Port 14333
Test-NetConnection <IP_THUDUC> -Port 14333
```

Từ từng site:

```powershell
Test-NetConnection <IP_CENTRAL> -Port 14333
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

> [!NOTE]
> **Quy ước tham số `-S <instance>` khi chạy `sqlcmd`** (áp dụng cho **mọi** lệnh trong tài liệu này):
> - Cấu hình của nhóm là named instance `FASHION`, cổng `14333`, nên dùng
>   **`-S localhost\FASHION`** (chạy trên chính máy đó) hoặc **`-S localhost,14333`**.
> - `-S localhost` **không dùng được**: nó trỏ vào instance mặc định `MSSQLSERVER`,
>   không phải `FASHION`.
> - Trên máy khác thì thay `localhost` bằng IP: `-S <IP>,14333` (không cần SQL Browser)
>   hoặc `-S <IP>\FASHION` (cần Browser + UDP 1434).
> - Ví dụ: `sqlcmd -S localhost\FASHION -E -i Fashion_Central.sql -b -f 65001`.

### 3.2. Trên Central

Sửa khối `CONFIG` trong `database/linked-servers.sql`: `Role = "central"`, điền IP
3 máy site (**kèm cổng, dạng `<IP>,14333`**), mật khẩu `distributed_user`. Sau đó:

```powershell
cd database
sqlcmd -S localhost\FASHION -E -i linked-servers.sql -b -f 65001
sqlcmd -S localhost\FASHION -E -i Fashion_Central.sql -b -f 65001
```

Kiểm tra:

```sql
USE [Fashion_Central];
SELECT name FROM sys.tables ORDER BY name;            -- 8 bảng
SELECT name, data_source FROM sys.servers WHERE is_linked = 1;  -- SITE_Q1, SITE_Q7, SITE_THUDUC
```

### 3.3. Trên từng site

Trong `database/linked-servers.sql` đổi `Role = "site"` và `CentralHost` = `<IP máy Central>,14333`.
Trong `database/sites/Fashion_Store.sql` **mở đúng một khối site**
(CN1 / CN2 / CN3). Sau đó trên **mỗi máy site**:

```powershell
cd database
sqlcmd -S localhost\FASHION -E -i linked-servers.sql -b -f 65001
sqlcmd -S localhost\FASHION -E -i sites\Fashion_Store.sql -b -f 65001
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
sqlcmd -S localhost\FASHION -E -i central-procs.sql -b -f 65001

# Trên từng site (mở đúng SiteBranch trong sites\site-procs.sql)
sqlcmd -S localhost\FASHION -E -i sites\site-procs.sql -b -f 65001
```

### 3.5. Nạp dữ liệu mẫu (Central)

```powershell
sqlcmd -S localhost\FASHION -E -i seed.sql -b -f 65001
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
#
# Lưu ý định dạng URL cho instance FASHION (cổng 14333):
# - Khuyến nghị: chỉ định cổng, không phụ thuộc SQL Browser
#   spring.datasource.url=jdbc:sqlserver://localhost:14333;databaseName=Fashion_Store;encrypt=true;trustServerCertificate=true
# - Hoặc dùng instanceName (driver tự hỏi SQL Browser, cần UDP 1434):
#   spring.datasource.url=jdbc:sqlserver://localhost;instanceName=FASHION;databaseName=Fashion_Store;encrypt=true;trustServerCertificate=true
# - LƯU Ý: databaseName phải là Fashion_Store (3 site) / Fashion_Central (Central).
#   File mẫu hiện ghi Fashion_Q1 — phải sửa, nếu không backend báo "Cannot open database".
```

File `application.properties` đã bị gitignore nên không sợ commit nhầm.

**Cách B – dùng profile `local` + biến môi trường (không tạo file mới):**

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://localhost:14333;databaseName=Fashion_Store;encrypt=true;trustServerCertificate=true'
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

> **Publisher là named instance `FASHION`.** `SERVERPROPERTY('ServerName')` trả về
> `HOST\FASHION`; vì push subscription chạy agent **trên chính Central** nên kết nối
> nội bộ vẫn OK, không cần SQL Browser. Nếu chuyển sang **pull subscription**, site phải
> kết nối ngược lại Publisher → cần `<IP_CENTRAL>,14333` hoặc Browser + UDP 1434.

Kiểm tra:

```sql
SELECT name, is_distributor FROM sys.servers;
EXEC sp_helpdistributor;
```

### 4.3. Tạo Publication

Với mô hình 3 chi nhánh, nên tạo:

- **1 publication dùng chung**: `SanPham`, `ChiNhanh` (không filter).
- **3 publication theo chi nhánh**: `Pub_Fashion_Store_Q1`, `Pub_Fashion_Store_Q7`,
  `Pub_Fashion_Store_ThuDuc` (đúng tên script `database/replication.sql` tạo ra),
  mỗi publication chứa các bảng riêng đã lọc theo `MaChiNhanh`.

> ✅ **Chốt B1 (Merge) – site tự ghi bảng riêng.** Các bảng `TonKho`, `HoaDon`, `ChiTietHoaDon`, `NhanVien`
> được publish từ Central tới mỗi site với filter `MaChiNhanh`. Site ghi cục bộ; Merge Agent đẩy lên Central
> và kéo thay đổi về. Central làm điểm tổng hợp, không phải nguồn ghi duy nhất.

Các bảng riêng cần lọc:

| Bảng | Điều kiện lọc |
| --- | --- |
| `NhanVien` | `MaChiNhanh = 'CN1'` |
| `TonKho` | `MaChiNhanh = 'CN1'` |
| `HoaDon` | `MaChiNhanh = 'CN1'` |
| `ChiTietHoaDon` | Join dẫn xuất theo `HoaDon` |

Các bước wizard:

1. Chuột phải **Local Publications** → **New Publication**.
2. Chọn database `Fashion_Central` (hoặc tên đã chốt).
3. Chọn **Merge publication** (đã chốt ở mục 0.4). Nếu chỉ cần một chiều Central → site
   thì có thể chọn Transactional, nhưng **không trộn cả hai cho cùng một bảng**.
4. Chọn articles:
   - Bỏ chọn `sysdiagrams`.
   - Chọn bảng cần replicate.
5. Với bảng riêng: **Add Filter** → chọn cột `MaChiNhanh` → điều kiện `= 'CN1'`.
6. Với `ChiTietHoaDon`: **Add Join to Extend the Selected Filter** → join `ChiTietHoaDon.MaHoaDon = HoaDon.MaHoaDon`.
7. Đặt tên publication (`Pub_Fashion_Store_Q1`).
8. Chọn **Create a snapshot immediately** và **Start the Snapshot Agent**.
9. Finish.

Lặp lại cho `CN2`, `CN3` (đổi điều kiện filter và tên publication).

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

1. `Replication` → `Local Publications` → chuột phải `Pub_Fashion_Store_Q1` → **New Subscriptions**.
2. Chọn **Push subscription** (hoặc Pull nếu muốn site tự kéo).
3. **Add SQL Server Subscriber** → nhập `<IP_Q1>,14333` (hoặc `<IP_Q1>\FASHION` nếu đã bật Browser), user `distributed_user`.
4. Chọn database đích: `Fashion_Store` (phải trùng tên database nguồn).
5. Chọn **Run the snapshot agent immediately**.
6. Finish.

Lặp lại cho `Pub_Fashion_Store_Q7` → Q7, `Pub_Fashion_Store_ThuDuc` → Thủ Đức.

> **Bắt buộc với named instance:** tên Subscriber phải kèm cổng — `<IP_Q1>,14333`
> (không phải `<IP_Q1>` trơn). Nếu SSMS/Wizard không cho nhập cổng, thêm subscriber
> trong `Replication → Subscribers` với server name `<IP_Q1>,14333` trước, rồi mới
> tạo subscription. Script `database/replication.sql` đã cấu hình sẵn dạng này.

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
USE [Fashion_Store];
SELECT TOP 10 * FROM dbo.TonKho WHERE MaChiNhanh = 'CN1';
SELECT TOP 10 * FROM dbo.HoaDon WHERE MaChiNhanh = 'CN1';
```

Trên Q7 / Thủ Đức tương tự với `CN2` / `CN3`.

---

## 6. Cấu hình Replication chi tiết

### 6.1. Mô hình đề xuất

```text
Central (Publisher + Distributor)
   │
   ├── Pub_Common            ──►  Q1, Q7, Thủ Đức   (SanPham, ChiNhanh)
   ├── Pub_Fashion_Store_Q1  ──►  Q1                 (NhanVien, TonKho, HoaDon, ChiTietHoaDon lọc CN1)
   ├── Pub_Fashion_Store_Q7  ──►  Q7                 (lọc CN2)
   └── Pub_Fashion_Store_ThuDuc ──► Thủ Đức          (lọc CN3)
```

### 6.2. Loại replication

| Loại | Dùng cho | Ghi chú |
| --- | --- | --- |
| **Merge** ✅ đã chọn | Bảng cần đồng bộ hai chiều / bán offline (`KhachHang`, bảng riêng nếu chọn mô hình B) | Khớp mẫu giảng viên; cần resolver xung đột |
| **Transactional** | Chỉ khi cần một chiều Central → site, dữ liệu ít sửa | **Không trộn** với Merge cho cùng một bảng |
| **Trigger + queue** | Bổ sung khi cần đẩy thay đổi lên Central mà không dùng Merge | Linh hoạt, lưu được khi mất mạng |

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

### 6.5. Chọn mô hình ghi cho bảng riêng

Bảng riêng (`TonKho`, `HoaDon`, `ChiTietHoaDon`, `NhanVien`) được deploy theo **B1 – Merge publication** (đã chốt mục 0.4).
Mỗi bảng được publish với filter `MaChiNhanh` → mỗi site chỉ nhận dữ liệu của chính mình.

#### 6.5.1. Merge / Transactional / Trigger + outbox khác nhau ở đâu

Ba cơ chế này **không cùng một trục so sánh**. Cần tách thành 2 câu hỏi:

- **Ai bắt thay đổi?** → engine của SQL Server (đọc log / đọc metadata merge) hay **ứng dụng tự viết** (trigger).
- **Đi mấy chiều?** → một chiều (Central → site) hay hai chiều.

| | **Merge** | **Transactional** | **Trigger + outbox** |
| --- | --- | --- | --- |
| Ai bắt thay đổi | Merge Agent đọc **metadata** (`rowguid`, `MSmerge_*`, generation) | Log Reader Agent đọc **transaction log** | **Trigger** do nhóm tự viết |
| Chiều | **2 chiều** | **1 chiều** (Publisher → Subscriber) | Tự định nghĩa (thường site → Central) |
| Đơn vị truyền | Ảnh dòng (row image) sau khi sửa | Từng lệnh `INSERT/UPDATE/DELETE` theo đúng thứ tự log | Tuỳ mình: ảnh dòng **hoặc độ lệch (delta)** |
| Xung đột | DBMS phát hiện, resolver theo priority | **Không có** (subscriber chỉ đọc) | Tự viết luật |
| Offline | Có — agent chạy bù khi có mạng | Có, nhưng site không ghi được | Có — nhưng phải tự retry |
| Đụng schema | Thêm cột hệ thống vào bảng article | Cần snapshot lại khi đổi schema | Không đụng, nhưng tự lo khi schema đổi |
| Công sức | Thấp (wizard), vận hành trung bình | Thấp | **Cao** (tự làm idempotent, thứ tự, retry, dead-letter) |
| Kiểm soát nghiệp vụ | Thấp | Thấp | **Cao** (chèn được logic riêng) |

> **Điểm khác biệt quan trọng nhất — ảnh dòng (state) vs độ lệch (delta):**
> Merge/Transactional gửi **trạng thái cuối của dòng**. Nếu Q1 và Q7 cùng bán 2 và 3 sản phẩm
> `SP001` khi offline, Merge chọn **một** ảnh dòng thắng; bản ghi còn lại chỉ nằm trong bảng
> conflict, **không tự cộng dồn** → phải xử lý tay, nếu không thì mất doanh số.
> Trigger + outbox có thể gửi **độ lệch** (`-2`, `-3`) nên cộng dồn đúng. Đây là lý do kỹ thuật
> để chọn B2 cho bảng mà **nhiều site cùng ghi một dòng**.
>
> Với `TonKho` đã lọc `MaChiNhanh` thì mỗi dòng chỉ có **một** site ghi → B1 (Merge) an toàn.

#### 6.5.2. Chọn mô hình ghi

| Mô hình | Ghi ở đâu | Đồng bộ thế nào | Mất Central site còn bán? | Đủ hạng mục "replication"? |
| --- | --- | --- | --- | --- |
| **A. Central là nguồn ghi** | Central | Transactional replication, filter `MaChiNhanh`, đẩy xuống site | ❌ Không | ✅ Có |
| **B. Site là nguồn ghi** ✅ **đã chọn (B1)** | Site | **B1:** Merge publication có filter `MaChiNhanh` | ✅ Có | ✅ Có |

> **Hiểu đúng mô hình B:** "site là nguồn ghi" **không** có nghĩa là bỏ replication cho bảng
> riêng. Nếu dùng **Merge**, bảng riêng vẫn được publish (có filter) và merge **hai chiều** —
> vừa phân mảnh, vừa replication, vừa bán offline. Chỉ khi dùng **Transactional** (một chiều)
> thì mới buộc phải bỏ publish và chuyển sang trigger/queue.

**Nếu chọn A:**

- Làm đúng mục 4.3 và 5.4 như đang viết: `Pub_Fashion_Store_Q1/Q7/ThuDuc` chứa bảng riêng đã lọc.
- Site chỉ đọc, không ghi trực tiếp các bảng đó.
- Không đáp ứng tiêu chí demo số 10 (mất Central vẫn bán).

**✅ Đã chọn B1 – Merge publication cho bảng riêng (khớp PDF giảng viên):**

- Vẫn tạo `Pub_Fashion_Store_Q1/Q7/ThuDuc` chứa `NhanVien`, `TonKho`, `HoaDon`, `ChiTietHoaDon`,
  lọc `MaChiNhanh = 'CN1'` và join filter cho `ChiTietHoaDon`.
- Site ghi cục bộ; Merge Agent đẩy lên Central và kéo thay đổi về.
- Được cả ba: phân mảnh (filter), replication (merge), offline (agent chạy bù khi có mạng).
- **Không** cần trigger cho các bảng này.

**B2 – Trigger + outbox cục bộ (tùy chọn, chỉ làm ví dụ mở rộng — KHÔNG dùng cùng lúc với B1 cho cùng bảng):**

- `Pub_Common` chỉ chứa bảng dùng chung: `SanPham`, `ChiNhanh`; `KhachHang` dùng Merge.
  **Không** tạo `Pub_Fashion_Store_Q1/Q7/ThuDuc` cho bảng riêng.
- Trigger ghi vào **`SyncOutbox` cục bộ tại site**, job định kỳ đẩy lên Central (mục 8.1–8.3).
- ⚠️ **Không** ghi trực tiếp `CENTRAL.[Fashion_Central]...` trong trigger: khi mất
  Central, insert qua linked server sẽ fail và **rollback luôn giao dịch bán hàng** → mất khả
  năng bán offline.
- Phải tự xử lý: idempotent (`MERGE`), thứ tự bản ghi, retry, dead-letter.
- Central tổng hợp bằng `UNION ALL` qua linked server (mục 9.4) hoặc bảng tổng hợp nhận từ queue.

**Điều kiện để mô hình B1 (đã chọn) đúng yêu cầu đề bài:**

1. Có **replication thật**: Merge publication `Pub_Fashion_Store_Q1/Q7/ThuDuc` chứa bảng riêng đã lọc.
2. Chứng minh **phân mảnh**: filter `MaChiNhanh`, truy vấn kiểm tra mỗi site chỉ có dữ liệu của mình (mục 9.2).
3. **Distributed transaction** (8.5), **concurrency** (8.6), **distributed query** (9.4) làm độc lập.
4. Central **nhận được** dữ liệu bảng riêng qua Merge Agent (hai chiều).
5. Không trộn B1 (Merge) và B2 (Trigger + outbox) cho cùng một bảng — sẽ tạo dữ liệu trùng hoặc ghi đè lẫn nhau.

> Ghi rõ lựa chọn vào `docs/PROJECT_STATUS.md` mục 10 trước khi chạy.

---

## 7. Linked Server

> [!NOTE]
> **Tham số `@datasrc` đối với instance `FASHION` (cổng `14333`):**
> - **Khuyến nghị:** `@datasrc = N'<IP>,14333'` — chỉ định thẳng cổng, **không** phụ thuộc
>   SQL Server Browser, không cần mở UDP 1434. Đây là cách các script trong `database/`
>   đang dùng (biến `Q1Host`/`Q7Host`/`TDHost`/`CentralHost` nhận giá trị `<IP>,14333`).
> - Nếu muốn dùng tên instance: `@datasrc = N'<IP>\FASHION'` — **bắt buộc** máy đích bật
>   service `SQL Server Browser` và mở **UDP 1434** inbound (mục 2.3), nếu không sẽ lỗi
>   `SQL Server Network Interfaces: Error Locating Server/Instance Specified`.
> - **Không dùng** `<IP>` trơn: cổng 1433 thuộc instance mặc định, không phải `FASHION`.

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
    @datasrc    = N'<IP_Q1>,14333',
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
    @datasrc    = N'<IP_CENTRAL>,14333',
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
SELECT TOP 5 * FROM SITE_Q1.[Fashion_Store].dbo.TonKho;
SELECT TOP 5 * FROM CENTRAL.[Fashion_Central].dbo.KhachHang;
```

> Tên database 4 phần phải khớp quy tắc trong suốt ở mục 0.2: 3 site dùng
> `[Fashion_Store]`, Central dùng `[Fashion_Central]`.

---

## 8. Trigger và đồng bộ dữ liệu

> Phần này chỉ làm sau khi chốt phương án ở mục 0.4.

### 8.1. Bảng outbox cục bộ tại mỗi site

> Đặt queue **tại site**, không đặt ở Central. Trigger chỉ ghi local nên không bao giờ bị
> chặn khi mất mạng; job sẽ đẩy lên Central sau.

```sql
USE [Fashion_Store];
GO

IF OBJECT_ID(N'dbo.SyncOutbox', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SyncOutbox (
        QueueId     BIGINT IDENTITY(1,1) PRIMARY KEY,
        TableName   VARCHAR(50)   NOT NULL,
        RecordKey   VARCHAR(50)   NOT NULL,
        Operation   VARCHAR(10)   NOT NULL,   -- INSERT / UPDATE / DELETE
        Payload     NVARCHAR(MAX) NULL,
        CreatedAt   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
        PushedAt    DATETIME2     NULL,
        RetryCount  INT           NOT NULL DEFAULT 0,
        LastError   NVARCHAR(4000) NULL
    );

    CREATE INDEX IX_SyncOutbox_Unpushed
        ON dbo.SyncOutbox (QueueId)
        WHERE PushedAt IS NULL;
END
GO
```

### 8.2. Trigger ghi outbox cục bộ (không gọi linked server)

Ví dụ bảng `TonKho` trên Q1 (làm tương tự cho `HoaDon`, `ChiTietHoaDon`, `NhanVien`):

```sql
USE [Fashion_Store];
GO

CREATE OR ALTER TRIGGER dbo.trg_TonKho_Sync
ON dbo.TonKho
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    -- INSERT / UPDATE: dựa vào "deleted" để phân biệt
    INSERT INTO dbo.SyncOutbox (TableName, RecordKey, Operation, Payload)
    SELECT
        'TonKho',
        CONCAT(i.MaChiNhanh, '|', i.MaSanPham),
        CASE WHEN d.MaSanPham IS NULL THEN 'INSERT' ELSE 'UPDATE' END,
        (SELECT i.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
    FROM inserted i
    LEFT JOIN deleted d
        ON d.MaChiNhanh = i.MaChiNhanh
       AND d.MaSanPham  = i.MaSanPham;

    -- DELETE: không có dòng tương ứng trong "inserted"
    INSERT INTO dbo.SyncOutbox (TableName, RecordKey, Operation, Payload)
    SELECT
        'TonKho',
        CONCAT(d.MaChiNhanh, '|', d.MaSanPham),
        'DELETE',
        NULL
    FROM deleted d
    WHERE NOT EXISTS (
        SELECT 1
        FROM inserted i
        WHERE i.MaChiNhanh = d.MaChiNhanh
          AND i.MaSanPham  = d.MaSanPham
    );
END
GO
```

> Trigger **không** được `INSERT INTO CENTRAL...`. Nếu ghi trực tiếp qua linked server, khi
> Central chết thì câu bán hàng rollback theo → site không bán được. Ghi local là điều kiện
> để mô hình B thực sự offline.

### 8.3. Job đẩy outbox lên Central (chạy tại site)

Tạo SQL Server Agent Job trên **từng site**, chạy mỗi 1–5 phút:

1. Đọc các dòng `PushedAt IS NULL` theo thứ tự `QueueId` (giữ đúng thứ tự phát sinh).
2. `MERGE` vào bảng tương ứng ở Central qua linked server (idempotent theo khóa chính).
3. Thành công: đánh dấu `PushedAt`.
4. Lỗi: tăng `RetryCount`, ghi `LastError`, dừng batch để lần sau thử lại (tránh vòng lặp vô hạn).

Khung xử lý:

```sql
USE [Fashion_Store];
GO
SET NOCOUNT ON;

DECLARE @QueueId BIGINT, @TableName varchar(50), @Key varchar(50),
        @Op varchar(10), @Payload nvarchar(max);

WHILE 1 = 1
BEGIN
    SELECT TOP (1)
        @QueueId = QueueId, @TableName = TableName, @Key = RecordKey,
        @Op = Operation, @Payload = Payload
    FROM dbo.SyncOutbox
    WHERE PushedAt IS NULL
    ORDER BY QueueId;

    IF @@ROWCOUNT = 0 BREAK;

    BEGIN TRY
        IF @TableName = 'TonKho'
        BEGIN
            IF @Op = 'DELETE'
                DELETE FROM CENTRAL.[Fashion_Central].dbo.TonKho
                WHERE CONCAT(MaChiNhanh, '|', MaSanPham) = @Key;
            ELSE
                MERGE CENTRAL.[Fashion_Central].dbo.TonKho AS t
                USING (
                    SELECT * FROM OPENJSON(@Payload) WITH (
                        MaChiNhanh   varchar(10) '$.MaChiNhanh',
                        MaSanPham    varchar(10) '$.MaSanPham',
                        SoLuongTon   int         '$.SoLuongTon',
                        MucTonToiThieu int       '$.MucTonToiThieu'
                    )
                ) AS s
                ON t.MaChiNhanh = s.MaChiNhanh AND t.MaSanPham = s.MaSanPham
                WHEN MATCHED THEN
                    UPDATE SET SoLuongTon     = s.SoLuongTon,
                               MucTonToiThieu = s.MucTonToiThieu,
                               NgayCapNhat    = GETDATE()
                WHEN NOT MATCHED THEN
                    INSERT (MaChiNhanh, MaSanPham, SoLuongTon, MucTonToiThieu)
                    VALUES (s.MaChiNhanh, s.MaSanPham, s.SoLuongTon, s.MucTonToiThieu);
        END
        -- Thêm nhánh cho HoaDon, ChiTietHoaDon, NhanVien

        UPDATE dbo.SyncOutbox
        SET PushedAt = SYSUTCDATETIME(), LastError = NULL
        WHERE QueueId = @QueueId;
    END TRY
    BEGIN CATCH
        UPDATE dbo.SyncOutbox
        SET RetryCount = RetryCount + 1,
            LastError  = ERROR_MESSAGE()
        WHERE QueueId = @QueueId;
        BREAK;   -- để job lần sau thử lại
    END CATCH
END
```

> Thứ tự quan trọng: `HoaDon` phải được đẩy trước `ChiTietHoaDon` (khóa ngoại). Vì `QueueId`
> tăng theo thứ tự phát sinh, chỉ cần app luôn insert header trước detail.

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

### 8.5. Distributed transaction (2PC/MSDTC) — bắt buộc theo đề tài

> Đây là hạng mục **distributed transaction** mà Merge Replication **không** thay thế được
> (xem mục 0.4.1). Demo: chuyển tồn kho giữa 2 site, phải **atomic** — hoặc cả hai site cùng
> cập nhật, hoặc không site nào đổi.

#### 8.5.1. Bật MSDTC trên cả 4 máy

1. `dcomcnfg` → **Component Services** → **Computers** → chuột phải **My Computer** → **Properties**.
2. Tab **MSDTC** → **Security Configuration**:
   - Tick **Network DTC Access**.
   - Tick **Allow Inbound** và **Allow Outbound**.
   - Chọn **No Authentication Required** (chỉ cho demo nội bộ VPN).
   - Tick **Enable XA Transactions** nếu dùng provider khác.
3. OK → restart service **Distributed Transaction Coordinator** (`msdtc`).
4. Firewall: cho phép ứng dụng **Distributed Transaction Coordinator** (TCP 135 + dải RPC động),
   hoặc tạm mở RPC cho dải IP Radmin VPN.

Kiểm tra linked server có bật promotion:

```sql
EXEC sp_serveroption N'SITE_Q1',     N'remote proc transaction promotion', N'true';
EXEC sp_serveroption N'SITE_Q7',     N'remote proc transaction promotion', N'true';
EXEC sp_serveroption N'SITE_THUDUC', N'remote proc transaction promotion', N'true';
```

#### 8.5.2. Procedure chuyển kho phân tán (chạy trên Central)

```sql
USE [Fashion_Central];
GO

CREATE OR ALTER PROCEDURE dbo.sp_ChuyenKho_PhanTan
    @MaSanPham varchar(10),
    @SoLuongChuyen int
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;                 -- lỗi ở đâu thì rollback toàn bộ

    IF @SoLuongChuyen <= 0
        THROW 50001, N'Số lượng phải lớn hơn 0.', 1;

    BEGIN DISTRIBUTED TRANSACTION;

        -- Q1 (CN1) trừ kho
        UPDATE SITE_Q1.[Fashion_Store].dbo.TonKho
        SET SoLuongTon = SoLuongTon - @SoLuongChuyen,
            NgayCapNhat = GETDATE()
        WHERE MaChiNhanh = 'CN1'
          AND MaSanPham  = @MaSanPham
          AND SoLuongTon >= @SoLuongChuyen;

        IF @@ROWCOUNT = 0
        BEGIN
            ROLLBACK TRANSACTION;
            THROW 50002, N'Q1 không đủ tồn kho để chuyển.', 1;
        END

        -- Q7 (CN2) cộng kho
        UPDATE SITE_Q7.[Fashion_Store].dbo.TonKho
        SET SoLuongTon = SoLuongTon + @SoLuongChuyen,
            NgayCapNhat = GETDATE()
        WHERE MaChiNhanh = 'CN2'
          AND MaSanPham  = @MaSanPham;

        IF @@ROWCOUNT = 0
        BEGIN
            ROLLBACK TRANSACTION;
            THROW 50003, N'Q7 chưa có dòng tồn kho cho sản phẩm này.', 1;
        END

    COMMIT TRANSACTION;
END
GO
```

#### 8.5.3. Kiểm chứng atomicity

```sql
-- 1) Trước khi chuyển
SELECT 'Q1' AS Site, MaChiNhanh, MaSanPham, SoLuongTon
FROM SITE_Q1.[Fashion_Store].dbo.TonKho WHERE MaSanPham = 'SP001';
SELECT 'Q7' AS Site, MaChiNhanh, MaSanPham, SoLuongTon
FROM SITE_Q7.[Fashion_Store].dbo.TonKho WHERE MaSanPham = 'SP001';

-- 2) Chuyển 5 sản phẩm Q1 -> Q7 (phải commit cả hai)
EXEC dbo.sp_ChuyenKho_PhanTan @MaSanPham = 'SP001', @SoLuongChuyen = 5;

-- 3) Test rollback: mở distributed transaction, sửa cả 2 site rồi ROLLBACK
BEGIN DISTRIBUTED TRANSACTION;
    UPDATE SITE_Q1.[Fashion_Store].dbo.TonKho
    SET SoLuongTon = SoLuongTon - 1
    WHERE MaChiNhanh = 'CN1' AND MaSanPham = 'SP001';

    UPDATE SITE_Q7.[Fashion_Store].dbo.TonKho
    SET SoLuongTon = SoLuongTon + 1
    WHERE MaChiNhanh = 'CN2' AND MaSanPham = 'SP001';
ROLLBACK TRANSACTION;

-- 4) Kiểm tra lại: cả Q1 và Q7 phải giữ nguyên như sau bước 2
```

> Lỗi thường gặp: `MSDTC on server ... is unavailable` hoặc
> `unable to begin a distributed transaction` → MSDTC chưa bật Network DTC Access hoặc firewall chặn RPC.

### 8.6. Tình huống concurrency — bắt buộc theo đề tài

Demo 1 — **blocking / tranh chấp ghi** trên cùng một dòng `TonKho` (mở 2 cửa sổ SSMS trên Q1):

```sql
-- Cửa sổ A: giữ transaction 15 giây, chưa commit
BEGIN TRANSACTION;
    UPDATE dbo.TonKho WITH (ROWLOCK)
    SET SoLuongTon = SoLuongTon - 1
    WHERE MaChiNhanh = 'CN1' AND MaSanPham = 'SP001';
    WAITFOR DELAY '00:00:15';
ROLLBACK TRANSACTION;

-- Cửa sổ B: chạy trong lúc A đang giữ lock -> bị block
UPDATE dbo.TonKho
SET SoLuongTon = SoLuongTon - 1
WHERE MaChiNhanh = 'CN1' AND MaSanPham = 'SP001';
```

Xem ai đang chặn ai:

```sql
SELECT session_id, blocking_session_id, wait_type, wait_time, wait_resource
FROM sys.dm_exec_requests
WHERE blocking_session_id <> 0;
```

Demo 2 — **tránh lost update** bằng `UPDLOCK` + kiểm tra tồn trong cùng transaction:

```sql
BEGIN TRANSACTION;
    UPDATE dbo.TonKho WITH (UPDLOCK, ROWLOCK)
    SET SoLuongTon = SoLuongTon - 1,
        NgayCapNhat = GETDATE()
    WHERE MaChiNhanh = 'CN1'
      AND MaSanPham  = 'SP001'
      AND SoLuongTon >= 1;

    IF @@ROWCOUNT = 0
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50010, N'Hết hàng, không thể bán.', 1;
    END
COMMIT TRANSACTION;
```

Demo 3 — **xung đột đồng bộ (Merge conflict)**: sửa cùng một `KhachHang` ở Q1 và Q7 khi
đang offline, sau đó để Merge Agent chạy → xem **Replication Monitor → Conflicts**.
Cơ chế phân xử: priority Subscriber 75.00 < Publisher 100.00, kèm cột `LastModified`.

---

## 9. Kiểm tra nghiệm thu

### 9.1. Theo từng máy

| # | Kiểm tra | Lệnh | Kết quả mong đợi |
| --- | --- | --- | --- |
| 1 | SQL Server chạy | `SELECT @@SERVERNAME` | `<TÊN_MÁY>\FASHION` |
| 2 | Không phải Express | `SELECT SERVERPROPERTY('Edition')` | Developer/Standard/Enterprise |
| 3 | Mixed auth | `SELECT SERVERPROPERTY('IsIntegratedSecurityOnly')` | `0` |
| 4 | Agent chạy | `EXEC xp_servicecontrol N'QUERYSTATE', N'SQLAgent$FASHION'` | `Running` |
| 5 | Port 14333 | `Test-NetConnection <IP> -Port 14333` | `True` |
| 6 | Snapshot folder | `Test-Path \\<IP_CENTRAL>\REPLDATA` | `True` |
| 7 | `distributed_user` | `SELECT name FROM sys.database_principals WHERE name='distributed_user'` | Có 1 dòng |
| 8 | Linked Server | `EXEC sp_testlinkedserver N'...'` | Không lỗi |
| 9 | Publication | `EXEC sp_helppublication` | Có publication |
| 10 | Subscription | Replication Monitor | Status = Running/Succeeded |
| 11 | Distributed transaction | `EXEC dbo.sp_ChuyenKho_PhanTan ...` + test ROLLBACK (mục 8.5.3) | Hai site cùng đổi hoặc cùng không đổi |
| 12 | Concurrency | 2 cửa sổ SSMS sửa cùng dòng `TonKho` (mục 8.6) | Thấy `blocking_session_id`, không mất update |

### 9.2. Kiểm tra phân mảnh

Trên Q1:

```sql
SELECT DISTINCT MaChiNhanh FROM dbo.TonKho;
-- Chỉ CN1
SELECT DISTINCT MaChiNhanh FROM dbo.HoaDon;
-- Chỉ CN1
```

Trên Q7: chỉ `CN2`. Trên Thủ Đức: chỉ `CN3`.

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
SELECT 'Q1' AS Site, * FROM SITE_Q1.[Fashion_Store].dbo.TonKho
UNION ALL
SELECT 'Q7', * FROM SITE_Q7.[Fashion_Store].dbo.TonKho
UNION ALL
SELECT 'TD', * FROM SITE_THUDUC.[Fashion_Store].dbo.TonKho;
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
| `Cannot open database "Fashion_Store"` | Tên DB không khớp hoặc chưa tạo | Dùng đúng `Fashion_Store` (site) / `Fashion_Central` (Central); kiểm tra `sys.databases` |
| `Login failed` / kết nối được instance mặc định nhưng **không thấy** `Fashion_Store` | Đang trỏ `localhost` hoặc `<IP>` (cổng 1433 = instance mặc định) | Đổi sang `localhost\FASHION` / `<IP>,14333` |
| `TCP Provider: No connection could be made ... port 1433` | Đang dùng 1433 cho instance `FASHION` | Instance `FASHION` ở `14333`: sửa connection string, firewall, linked server `@datasrc` |
| `Error Locating Server/Instance Specified` khi dùng `<IP>\FASHION` | SQL Server Browser tắt hoặc chưa mở UDP 1434 | Bật service `SQLBrowser` + mở UDP 1434 (mục 2.3), hoặc chuyển sang `<IP>,14333` |
| Linked server chạy được nhưng `sp_testlinkedserver` lỗi | `@datasrc` thiếu cổng `14333` | Sửa `@datasrc = N'<IP>,14333'` |
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
| Dữ liệu chi nhánh lọc sai / không thấy gì | Mã CN lệch (`CN001` vs `CN1`) | Chốt mục 0.3, sửa `config/branches.js` + alias ở backend |

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
    @publication = N'Pub_Fashion_Store_Q1',
    @subscriber = N'<SUBSCRIBER_SERVER>',
    @destination_db = N'Fashion_Store';

-- Xóa publication
EXEC sp_droppublication @publication = N'Pub_Fashion_Store_Q1';
```

> Không xóa `distribution` database khi chưa chắc chắn; có thể ảnh hưởng các publication khác.

---

## 12. Checklist theo máy

### Máy 1 – Central

- [ ] SQL Server Developer/Standard, instance `FASHION`, TCP/IP `14333`, firewall
- [ ] Mixed auth, `sa` bật
- [ ] SQL Server Agent `SQLAgent$FASHION` chạy tự động
- [ ] Snapshot folder `\\CENTRAL\REPLDATA` đã share
- [ ] Database `Fashion_Central` đã tạo
- [ ] `distributed_user` đã tạo
- [ ] Configure Distribution xong
- [ ] Publication `Pub_Fashion_Store_Q1`, `Pub_Fashion_Store_Q7`, `Pub_Fashion_Store_ThuDuc`
- [ ] Linked Server `SITE_Q1`, `SITE_Q7`, `SITE_THUDUC` (`@datasrc` có `,14333`)
- [ ] `sp_testlinkedserver` OK
- [ ] Snapshot Agent chạy thành công
- [ ] MSDTC bật Network DTC Access trên cả 4 máy (mục 8.5.1)
- [ ] `sp_ChuyenKho_PhanTan` chạy đúng + test ROLLBACK (mục 8.5.3)
- [ ] Demo concurrency 2 cửa sổ SSMS (mục 8.6)

### Máy 2 – Q1

- [ ] SQL Server Developer/Standard, instance `FASHION`, TCP/IP `14333`, firewall
- [ ] Mixed auth
- [ ] Database `Fashion_Store` + 8 bảng
- [ ] `distributed_user` + quyền đọc/ghi
- [ ] Linked Server `CENTRAL` (`@datasrc` có `,14333`)
- [ ] Subscription `Pub_Fashion_Store_Q1`
- [ ] Dữ liệu chỉ có `CN1`
- [ ] Bán hàng thử và kiểm tra tồn

### Máy 3 – Q7

- [ ] SQL Server Developer/Standard, instance `FASHION`, TCP/IP `14333`, firewall
- [ ] Mixed auth
- [ ] Database `Fashion_Store` + 8 bảng
- [ ] `distributed_user` + quyền đọc/ghi
- [ ] Linked Server `CENTRAL` (`@datasrc` có `,14333`)
- [ ] Subscription `Pub_Fashion_Store_Q7`
- [ ] Dữ liệu chỉ có `CN2`
- [ ] Bán hàng thử và kiểm tra tồn

### Máy 4 – Thủ Đức

- [ ] SQL Server Developer/Standard, instance `FASHION`, TCP/IP `14333`, firewall
- [ ] Mixed auth
- [ ] Database `Fashion_Store` + 8 bảng
- [ ] `distributed_user` + quyền đọc/ghi
- [ ] Linked Server `CENTRAL` (`@datasrc` có `,14333`)
- [ ] Subscription `Pub_Fashion_Store_ThuDuc`
- [ ] Dữ liệu chỉ có `CN3`
- [ ] Bán hàng thử và kiểm tra tồn

---

## Phụ lục A – Script T-SQL tham khảo cho Publication

> Chỉ dùng khi đã hiểu rõ; wizard an toàn hơn cho lần đầu.
> Script dưới đây là khung **MERGE** (đã chốt mục 0.4). Xem chi tiết đầy đủ trong `database/replication.sql`.

```sql
USE [Fashion_Central];
GO

-- Bật merge publishing cho database
EXEC sp_replicationdboption
    @dbname = N'Fashion_Central',
    @optname = N'merge publish',
    @value = N'true';
GO

-- Tạo merge publication cho Q1 (CN1)
EXEC sp_addmergepublication
    @publication = N'Pub_Fashion_Store_Q1',
    @description = N'Replication to Quận 1 (CN1), filtered by MaChiNhanh',
    @status = N'active',
    @allow_push = N'true',
    @allow_pull = N'true',
    @repl_freq = N'continuous',
    @independent_agent = N'true',
    @retention = 14,
    @history_retention = 48;
GO

-- Thêm article đã lọc: NhanVien (chỉ CN1)
EXEC sp_addmergearticle
    @publication = N'Pub_Fashion_Store_Q1',
    @article = N'NhanVien',
    @source_object = N'NhanVien',
    @type = N'logbased',
    @schema_option = 0x000000000803509F,
    @pre_creation_cmd = N'none',
    @subset_filterclause = N'MaChiNhanh = ''CN1''';
GO

-- Thêm article đã lọc: TonKho (chỉ CN1)
EXEC sp_addmergearticle
    @publication = N'Pub_Fashion_Store_Q1',
    @article = N'TonKho',
    @source_object = N'TonKho',
    @type = N'logbased',
    @schema_option = 0x000000000803509F,
    @pre_creation_cmd = N'none',
    @subset_filterclause = N'MaChiNhanh = ''CN1''';
GO

-- Thêm article đã lọc: HoaDon (chỉ CN1)
EXEC sp_addmergearticle
    @publication = N'Pub_Fashion_Store_Q1',
    @article = N'HoaDon',
    @source_object = N'HoaDon',
    @type = N'logbased',
    @schema_option = 0x000000000803509F,
    @pre_creation_cmd = N'none',
    @subset_filterclause = N'MaChiNhanh = ''CN1''';
GO

-- Thêm article dẫn xuất: ChiTietHoaDon (join filter qua HoaDon)
EXEC sp_addmergearticle
    @publication = N'Pub_Fashion_Store_Q1',
    @article = N'ChiTietHoaDon',
    @source_object = N'ChiTietHoaDon',
    @type = N'logbased',
    @schema_option = 0x000000000803509F,
    @pre_creation_cmd = N'none';
GO

-- Join filter: ChiTietHoaDon -> HoaDon
EXEC sp_addmergejoinfilter
    @publication = N'Pub_Fashion_Store_Q1',
    @article = N'ChiTietHoaDon',
    @join_article = N'HoaDon',
    @join_filterclause = N'ChiTietHoaDon.[MaHoaDon] = HoaDon.[MaHoaDon]',
    @join_unique_key = 1,
    @type = 1;
GO

-- Tạo snapshot agent
EXEC sp_addpublication_snapshot
    @publication = N'Pub_Fashion_Store_Q1',
    @frequency_type = 4,   -- daily
    @frequency_interval = 1,
    @frequency_subday = 8, -- hours
    @frequency_subday_interval = 1;
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
11. [x] Chốt cơ chế đồng bộ `KhachHang`: **Merge Replication** (theo file PDF bài giảng giảng viên, mục 0.4).
12. [ ] Cấu hình backend từng máy theo mục 3.4 (copy `application-local.properties` hoặc dùng profile `local` + biến môi trường).
13. [ ] Triển khai thực tế trên 4 máy Radmin VPN và chạy nghiệm thu theo mục 9.
