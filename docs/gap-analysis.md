# Đối chiếu với mẫu đồ án của giảng viên và kế hoạch khắc phục

Tài liệu này ghi lại: mẫu giảng viên (GV) yêu cầu gì, dự án hiện lệch ở đâu, và thứ tự
làm để khớp. Nội dung lấy từ file mẫu
`[6][4]TONG HOP TOM TAT CAI DAT CSDL PT - QUAN LY THUC THI GIAO DICH- SV.pdf`.

Phạm vi hiện tại của nhóm: **làm trước phần database, backend, frontend**. Phần phân tán
(replication, linked server, stored procedure phân tán) làm sau, nhưng các quyết định dưới
đây cần chốt sớm vì sửa lại schema sau này rất tốn công.

---

## 1. Mẫu giảng viên yêu cầu gì

| # | Nhóm | Yêu cầu trong mẫu |
| --- | --- | --- |
| 1 | Điều kiện môi trường | SQL Server **Agent phải chạy**, tối thiểu 3 server, **không dùng bản Express**. Tắt Firewall, thêm Inbound Rule mở cổng dịch vụ, bật TCP/IP trong SQL Server Configuration Manager, đổi Security sang **SQL Server and Windows Authentication Mode**, phân quyền account dùng để liên kết máy |
| 2 | Phân tán dữ liệu | (b) **Định nghĩa publication**: publication là container chứa các article (table, view, stored procedure, UDF); phải chỉ rõ **Publisher**, **Distributor**, **Subscriber**. (c) **Định nghĩa subscription**: database nhận publication, chỉ rõ Subscriber. Các hình thức phân mảnh: ngang, dọc, hỗn hợp. Ví dụ đẩy dữ liệu: `KHOA` theo `MAKH` là phân mảnh ngang nguyên thủy, `LOP` là phân mảnh ngang dẫn xuất |
| 3 | Quy tắc phân mảnh | Phải thỏa **tính đầy đủ, tính tái thiết, tính tách biệt** |
| 4 | Linked Server | `sp_addlinkedserver` (provider SQLOLEDB, có UID/PWD) kèm `sp_addlinkedsrvlogin`, kiểm tra bằng `SELECT * FROM TOSERVER2.QL_VATTU.DBO.NHANVIEN`, có thể tạo bằng giao diện trong SSMS |
| 5 | Quy tắc "trong suốt" | **(a) Tên CSDL ở các Subscriber phải giống nhau**; **(b) Tên Linked Server phải giống nhau**. Để một stored procedure chạy được ở mọi server phân mảnh mà không phải chỉ lại đường dẫn |
| 6 | SP truy vấn phân tán | `SELECT ... UNION` gom dữ liệu từ các server phân mảnh (ví dụ `SP_DSLOP`) |
| 7 | SP tìm kiếm / sửa / xoá xuyên server | Chuỗi `IF EXISTS (bản cục bộ) ... ELSE IF EXISTS (link1) ... ELSE IF EXISTS (link2) ... ELSE raiserror` (ví dụ `sp_TimSV`, `Update_Hosv`, `Delete_Masv`, `ChuyenLop`) |
| 8 | Trigger | Trigger ở **mỗi site** để đồng bộ dữ liệu, và trigger **kiểm soát** update/delete ở máy chủ (không cho xoá bản ghi đang được tham chiếu) |
| 9 | Tài liệu thiết kế | 7.1 Phân tích thực thể (bảng STT / Tên thuộc tính / Kiểu / Ràng buộc / Khóa / Mô tả); 7.2 Lược đồ liên kết thực thể; 7.3 Lược đồ quan hệ liên kết; 7.4 Quan hệ giữa các bảng; 7.5 Thiết kế phân mảnh ngang và nhân bản (bảng **chung** chỉ thêm/sửa/xoá ở máy chủ rồi nhân bản xuống site, bảng **riêng** do từng site quản lý); **3.5 Sơ đồ định vị** với cột "Trụ sở chính / Chi nhánh" và ký hiệu **W** (tạo mới và ghi), **E** (sửa), **D** (xoá), **R** (đọc), **H** (tần suất cao), **L** (tần suất thấp); 3.6 Thiết kế nhân bản, đồng bộ hoá; 3.7 Thiết kế vật lí các bảng tại các trạm; 3.8 Cài đặt CSDL, tạo liên kết giữa các bảng |
| 10 | Kịch bản và phân tích chức năng | Mục 1 Kịch bản; 2.2 phân tích chức năng **tại đại lý tổng và từng đại lý**; 3 chức năng ở máy trạm; 4 chức năng ở máy chủ; 5 phân quyền theo nhóm đối tượng; 6 thiết kế mạng tổng quan |
| 11 | Chức năng nghiệp vụ và giao diện | Quản lý khách hàng, nhân viên, hóa đơn (đặt vé), tìm kiếm, thống kê, báo cáo, phân quyền, in hóa đơn. Giao diện: đăng nhập, tìm kiếm khách hàng, tìm kiếm chuyến bay, chọn loại vé, thông tin vé |

---

## 2. Bảng đối chiếu: dự án lệch ở đâu

| # | Nhóm GV | Trạng thái dự án | Độ lệch |
| --- | --- | --- | --- |
| 1 | Điều kiện môi trường | README mục 21-23 đã có TCP/IP, firewall, cổng 1433. **Thiếu**: SQL Server Agent, chọn edition, mixed auth mode, cấp quyền account liên kết máy | ~50% |
| 2 | Publication / Subscription | Chưa có script nào | 100% |
| 3 | Phân mảnh ngang + ngang dẫn xuất | README mục 6 mô tả đúng hướng (`TonKho`, `HoaDon` theo `MaChiNhanh`; `ChiTietHoaDon` dẫn xuất theo `HoaDon`) nhưng **chưa vẽ sơ đồ**, chưa tách bảng chung / bảng riêng | ~70% |
| 4 | Quy tắc đầy đủ / tái thiết / tách biệt | Chưa nhắc ở đâu | 100% |
| 5 | Linked Server | Chỉ có **ví dụ trong README**, chưa có script trong `database/` | 100% |
| 6 | Quy tắc trong suốt | **Vi phạm**: tên CSDL khác nhau (`Fashion_Q1` / `Fashion_Q7` / `Fashion_ThuDuc`), tên linked server khác nhau (`SITE_Q1` / `SITE_Q7` / `SITE_THUDUC`) | 100% |
| 7 | SP Union trong suốt | Chưa có stored procedure nào | 100% |
| 8 | SP tìm kiếm / sửa / xoá xuyên server | Chưa có | 100% |
| 9 | Trigger | Chưa có | 100% |
| 10 | Tài liệu thiết kế | Có mục tiêu nghiệp vụ, kiến trúc, phân quyền. **Thiếu**: ERD, lược đồ liên kết thực thể, quan hệ giữa các bảng, **sơ đồ định vị W/E/D/R/H/L**, sơ đồ phân mảnh vẽ tay, thiết kế vật lí tại các trạm, file kịch bản, phân tích chức năng theo vị trí | ~55% |
| 11a | Chức năng nghiệp vụ (backend) | Đã có: auth và phân quyền, nhân viên, tài khoản, chi nhánh, sản phẩm, khách hàng. **Thiếu**: tồn kho, bán hàng, chi tiết hóa đơn, thống kê, trạng thái site, và các API danh sách (GET) cho nhân viên / tài khoản / chi nhánh | ~55% |
| 11b | Giao diện (frontend) | Xem mục 5 bên dưới | ~65% |

**Tổng: khoảng 38% hoàn thành nếu tính cả phần phân tán (vì phân tán = 0%). Riêng phần
database + backend + frontend: khoảng 60%.**

---

## 3. Các điểm lệch cần sửa sớm

### A. Phải quyết định trước khi làm tiếp (sửa sau rất tốn)

| # | Vấn đề | Bằng chứng | Tác động |
| --- | --- | --- | --- |
| A1 | **Tên CSDL khác nhau ở các Subscriber** | `database/Fashion_Q1.sql`, `Fashion_Q7.sql`, `Fashion_TD.sql` | Vi phạm quy tắc "trong suốt" của GV: một stored procedure phải viết khác nhau cho từng site |
| A2 | **Tên Linked Server khác nhau** | README mục 24 | Cùng lý do. Cần chốt quy ước: tên link đặt theo **đích** (`SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`, `CENTRAL`) và stored procedure luôn tham chiếu đủ các đích |
| A3 | **`KhachHang.MaKhachHang` không có định danh toàn hệ thống** | `Fashion_Q1.sql` (khóa `varchar(10)`) | Mô hình copy **hai chiều** (site → Central → site). Hai site cùng tạo `KH001` sẽ đụng khóa chính, replication dừng. Cần khóa toàn cục: GUID hoặc mã có tiền tố site |
| A4 | **`KhachHang` có ràng buộc `UNIQUE(SoDienThoai)`** | `Fashion_Q1.sql` | Hai site cùng số điện thoại, khác mã khách → lỗi unique khi gộp. Cần quy tắc giải quyết xung đột |
| A5 | **Thiếu cột version / timestamp** trên bảng đồng bộ hai chiều | schema hiện tại | Không phân biệt được bản ghi mới hay cũ nên không giải quyết được xung đột |
| A6 | **`HoaDon.MaHoaDon` chưa có quy tắc sinh mã toàn hệ thống** | `Fashion_Q1.sql`; `backend/.../service/GeneratedId.java` sinh mã không có tiền tố site | Central gộp báo cáo từ ba site sẽ ra mã hóa đơn trùng |
| A7 | **Chưa tách rõ bảng chung và bảng riêng** | ba file schema giống nhau 100%, không có bảng riêng cho Central | Không lập được publication / subscription đúng, không vẽ được sơ đồ định vị |
| A8 | **`Fashion_TD.sql` tạo tên database `Fashion_TD`** | `database/Fashion_TD.sql` | README quy định `Fashion_ThuDuc` (mục 3 và mục 24) |
| A9 | **`Fashion_Q7.sql` và `Fashion_TD.sql` thiếu `distributed_user`** | chỉ `Fashion_Q1.sql` có `CREATE USER` + `db_datareader` / `db_datawriter` | Linked server tới hai site này sẽ lỗi phân quyền |
| A10 | **`TaiKhoan.MaTaiKhoan` là `INT IDENTITY`** | `Fashion_Q1.sql` | Ổn vì bảng local của từng site. Nếu Central phải quản lý tài khoản toàn chuỗi thì mã này không dùng được |
| A11 | **Backend thiếu entity `TonKho` và `ChiTietHoaDon`** | `backend/src/main/java/.../entity/` | Không làm được nghiệp vụ bán hàng và cập nhật tồn |
| A12 | **`GeneratedId` quét toàn bảng để sinh mã** | `backend/.../service/GeneratedId.java` | Mỗi site có bộ mã riêng nên mã trùng toàn hệ thống, đồng thời O(n) mỗi lần tạo |

### B. Có thể để sau, nhưng phải biết trước

| # | Vấn đề | Ghi chú |
| --- | --- | --- |
| B1 | Không có dữ liệu mẫu (không có câu `INSERT` nào) | README mục 30 nói "đã có database và dữ liệu sơ bộ" nhưng thực tế trong repo không có dữ liệu. Cần bộ dữ liệu đúng kịch bản demo: `SP001` tồn bằng 10 ở Quận 7, `KH001`, ba chi nhánh, nhân viên và tài khoản |
| B2 | `database/` không có `central.sql` và `common.sql` | `docs/setup.md` mục 3-4 đang hướng dẫn chạy hai file không tồn tại |
| B3 | File `.sql` lưu trong git dạng **UTF-16 LE BOM**, chưa có `.gitattributes` | Diff và merge rất khó đọc |
| B4 | Script không idempotent | Hardcode đường dẫn `MSSQL17.MSSQLSERVER`, `CREATE USER ... FOR LOGIN` giả định login đã có sẵn, `CREATE DATABASE` không kiểm tra tồn tại |
| B5 | Chưa có hướng dẫn SQL Server Agent, edition, mixed auth | `docs/setup.md` thiếu điều kiện tiên quyết để phân tán |
| B6 | Chưa có shared folder cho snapshot (`\\<CENTRAL>\REPLDATA`, Everyone Read/Write) | Mẫu GV mục II "Chuẩn bị" yêu cầu |
| B7 | Chưa có bảng log / queue đồng bộ ở Central | README mục 10 yêu cầu "lưu được thay đổi khi mất mạng". Nếu dùng trigger (mẫu GV mục 4) thì bắt buộc có |
| B8 | Chưa có test cho backend | `backend/src/test` chưa tồn tại. GV yêu cầu bằng chứng "không tồn âm", "không tạo trùng hóa đơn" |
| B9 | `docs/architecture.md` mô tả sai thực tế file SQL | Doc nói các file SQL "chỉ kiểm tra kết nối" trong khi thực tế là script tạo database |

### C. Phần đang tốt, giữ nguyên

- `TonKho` và `HoaDon` có cột `MaChiNhanh`: đúng tinh thần mẫu GV, dùng làm điều kiện
  `Add Filter` cho phân mảnh ngang nguyên thủy.
- `ChiTietHoaDon` không có `MaChiNhanh`, khoá ngoại `MaHoaDon`: đúng phân mảnh ngang dẫn xuất.
- Ba file schema giống hệt nhau: điều kiện cần để tạo publication dạng filter kèm join dẫn xuất.
- Backend: JPA map đúng tên bảng và cột SQL Server, `ddl-auto=none`, `IDENTITY` đúng cho `TaiKhoan`,
  có `@Lock(PESSIMISTIC_WRITE)` kèm `Isolation.SERIALIZABLE` khi sinh mã.

---

## 4. Kế hoạch theo giai đoạn

### Giai đoạn 0 - Chốt kiến trúc (chặn mọi việc khác)

| Câu hỏi | Lựa chọn | Gợi ý |
| --- | --- | --- |
| Tên CSDL trên bốn máy | Đổi cả bốn về **tên giống nhau** (ví dụ `Fashion_Store`) | Tuân thủ quy tắc GV mục 6a, một stored procedure viết một lần chạy được bốn nơi. Đổi giá: phải tạo lại database trên bốn máy |
| Tên Linked Server | Giữ theo đích: `SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`, `CENTRAL` | Đúng vì stored procedure tham chiếu "đích" chứ không phải "vị trí" |
| Bảng chung / bảng riêng | **Chung** (Central thêm sửa xoá, nhân bản xuống site): `SanPham`, `ChiNhanh` · **Riêng** (site tự quản lý, đồng bộ lên Central): `KhachHang`, `NhanVien`, `HoaDon`, `ChiTietHoaDon`, `TonKho` · **Chỉ Central**: `TaiKhoan` | Khớp với sơ đồ định vị mẫu GV |
| Cơ chế đồng bộ hai chiều | Transactional Replication cho bảng chung **+** trigger kèm bảng queue ở Central cho bảng riêng | Không dùng hai cơ chế cho cùng một luồng dữ liệu |

### Giai đoạn 1 - Database (nền cho cả hai phần)

1. Chốt và sửa schema theo nhóm A: định danh toàn cục cho `KhachHang` và `HoaDon`, cột
   `LastModified`, quy tắc sinh mã hóa đơn, sửa tên `Fashion_ThuDuc`, bổ sung
   `distributed_user` cho Quận 7 và Thủ Đức.
2. Dựng lại bộ script trong `database/` cho khớp `docs/setup.md`:

```text
database/
├── common.sql          # schema dùng chung, stored procedure, định nghĩa bảng
├── central.sql         # Fashion_Central và linked server tới ba site
├── q1.sql
├── q7.sql
├── thuduc.sql
├── replication/
│   ├── 01_configure_distributor.sql
│   ├── 02_create_publication.sql    # có Add Filter theo MaChiNhanh và Add Join dẫn xuất
│   ├── 03_create_subscription.sql
│   └── 04_triggers_sync.sql
└── seed.sql            # dữ liệu mẫu theo kịch bản demo
```

3. Viết các stored procedure trong suốt (mục 6, 7, 8 của mẫu GV), đặt trong `common.sql`
   để replicate xuống mọi site:
   - `usp_TonKhoToanHeThong` - `UNION ALL` ba site
   - `usp_TimHoaDon`, `usp_TimKhachHang` - chuỗi `IF EXISTS` local rồi mới đến linked server
   - `usp_TongHopDoanhThu` - báo cáo cho GV mục 4.1.3
   - `usp_BanHang` - transaction, khoá tồn chống âm, idempotency chống tạo trùng đơn
4. Chuyển file `.sql` sang UTF-8, thêm `.gitattributes`.
5. Bổ sung `docs/setup.md`: SQL Server Agent phải chạy, chọn edition không phải Express,
   mixed auth mode, shared folder snapshot, cấp quyền account cho linked server.

### Giai đoạn 2 - Backend

1. Thêm entity `TonKho` và `ChiTietHoaDon` kèm repository và specification.
2. Thêm API tồn kho: danh sách, sắp hết hàng, nhập kho, tồn theo chi nhánh.
3. Thêm API bán hàng gọi stored procedure `usp_BanHang`.
4. Thêm API thống kê và báo cáo (doanh thu theo khoảng ngày, top sản phẩm, doanh số nhân viên).
5. Thêm API trạng thái site (ping từng linked server).
6. Thêm API tổng hợp phân tán ở Central.
7. Bổ sung `branchId` vào `LoginResponse` và `AccountResponse` (cột `NhanVien.MaChiNhanh` đã có sẵn).
8. Thêm các API GET còn thiếu: `/employees`, `/accounts`, `/branches`, `/api/invoices`.
9. Sửa `GeneratedId`: sinh mã có tiền tố site, không quét toàn bảng.
10. Thêm test cho nghiệp vụ bán hàng.
11. Tách profile cấu hình theo vai trò máy: Central đọc linked server, site đọc database local.

### Giai đoạn 3 - Frontend

Xem mục 5.

### Giai đoạn 4 - Phân tán

Bám đúng trình tự mẫu GV mục I và II:

1. Bật SQL Server Agent, tạo snapshot folder, `Configure Distribution`.
2. `Create Publication` ở Central: article bảng chung không filter; article bảng riêng **có filter
   theo `MaChiNhanh`** kèm `Add Join to Extend the Selected Filter` cho đúng join path
   (`TonKho` → `HoaDon` → `ChiTietHoaDon`, `NhanVien` theo `MaChiNhanh`).
3. `New Subscription` cho ba site, database đích phải **cùng tên** (phụ thuộc Giai đoạn 0).
4. Linked server bốn chiều kèm `sp_addlinkedsrvlogin`.
5. Trigger đồng bộ site → Central kèm bảng queue `SyncQueue` ở Central.
6. Trigger kiểm soát ở máy chủ: chặn xoá `SanPham` đang có trong `TonKho` hoặc `ChiTietHoaDon`.
7. Chạy bộ tiêu chí kiểm tra trong `docs/setup.md` mục "Tiêu chí kiểm tra khi đã triển khai nghiệp vụ".

### Giai đoạn 5 - Tài liệu

| File cần tạo | Phủ mục GV |
| --- | --- |
| `docs/erd.md` | 7.2 / 7.3 / 7.4 |
| `docs/phan-manh.md` | 7.5, quy tắc đầy đủ / tái thiết / tách biệt, sơ đồ phân mảnh ngang nguyên thủy và ngang dẫn xuất |
| `docs/dinh-vi.md` | 3.5 - bảng "Trụ sở chính / Chi nhánh" với ký hiệu W/E/D/R/H/L và sơ đồ định vị chung / riêng |
| `docs/nhan-ban-dong-bo.md` | 3.6 và 3.7 |
| `docs/kinh-ban.md` | Mục 1 Kịch bản |
| `docs/phan-tich-chuc-nang.md` | Mục 2.2 chức năng theo vị trí, mục 5 phân quyền |
| `docs/demo-checklist.md` | Mục 34 và 35 - mười tiêu chí demo GV chấm |

---

## 5. Trạng thái frontend hiện tại

### 5.1 Các trang đã có giao diện và chạy được với dữ liệu giả

Phần việc của Nguyễn Quốc Đạt đã build xong toàn bộ giao diện. Backend chưa có API tồn kho
và thống kê nên các service trong `frontend/src/services/` trả dữ liệu giả theo đúng quy ước
dự án (`frontend/README.md`, mục "Gọi dữ liệu"): khi backend có API chỉ sửa phần thân hàm
trong service, trang giao diện không phải sửa.

| Trang | Route trong `nav.js` | Tệp |
| --- | --- | --- |
| Tồn kho (chi nhánh của tôi) | `/staff/inventory`, `/manager/inventory` | `frontend/src/pages/inventory/InventoryPage.jsx` |
| Tồn kho toàn chuỗi | `/admin/inventory` (tab "Toàn chuỗi") | cùng tệp trên |
| Tồn kho theo chi nhánh | `/admin/inventory` (tab "Theo chi nhánh") | cùng tệp trên |
| Sắp hết hàng | `/manager/low-stock` | `frontend/src/pages/inventory/LowStockPage.jsx` |
| Dashboard Admin | `/admin/dashboard` | `frontend/src/pages/dashboard/AdminDashboardPage.jsx` |
| Thống kê | `/manager/statistics`, `/admin/statistics` | `frontend/src/pages/statistics/StatisticsPage.jsx` |
| So sánh doanh thu | `/admin/revenue` | `frontend/src/pages/statistics/RevenueComparisonPage.jsx` |
| Sản phẩm bán chạy | `/admin/top-products` | `frontend/src/pages/statistics/TopProductsPage.jsx` |
| Doanh số nhân viên | `/admin/employee-sales` | `frontend/src/pages/statistics/EmployeeSalesPage.jsx` |

Các service dùng chung:

| Tệp | Trách nhiệm |
| --- | --- |
| `frontend/src/services/mockData.js` | Dữ liệu giả: 12 sản phẩm, 9 nhân viên, 8 khách hàng, 36 dòng tồn kho, lịch sử 90 ngày hóa đơn kèm chi tiết hóa đơn. Dùng bộ sinh số ổn định nên reload không nhảy số |
| `frontend/src/services/inventoryService.js` | `listInventory`, `listLowStock`, `getInventorySummary`, `adjustStock` |
| `frontend/src/services/statisticsService.js` | `getRevenueSummary`, `getRevenueSeries`, `getRevenueByBranch`, `getRevenueMatrix`, `getTopProducts`, `getEmployeeSales` |
| `frontend/src/services/dashboardService.js` | `getAdminDashboard` |

### 5.2 Các trang còn thiếu giao diện (việc của thành viên khác)

| Trang | Route | Phụ trách |
| --- | --- | --- |
| Bán hàng (POS) | `/pos` | Nguyễn Bảo |
| Sản phẩm | `/staff/products`, `/manager/products`, `/admin/products` | Nguyễn Bảo |
| Khách hàng | `/staff/customers`, `/manager/customers`, `/admin/customers` | Nguyễn Bảo |
| Hóa đơn | `/staff/invoices`, `/manager/invoices`, `/admin/invoices` | Nguyễn Bảo |
| Dashboard Manager | `/manager/dashboard` | Nguyễn Bảo |
| Hồ sơ, 403, 404 | `/profile` | Trần Vĩnh Khang |
| Nhân viên | `/manager/employees`, `/admin/employees` | Trần Vĩnh Khang |
| Tài khoản | `/manager/accounts`, `/admin/accounts` | Trần Vĩnh Khang |
| Chi nhánh | `/admin/branches` | Trần Vĩnh Khang |
| Trạng thái Site | `/admin/sites` | Trần Vĩnh Khang |

### 5.3 Ghi chú kỹ thuật cho frontend

- **Contract API**: mọi API trả bọc `{ code, message, result }`. Danh sách trả
  `result = { content, pageNumber, pageSize, totalElements, totalPages, last }`.
  Component `Pagination` đánh số trang từ 1, backend `page` bắt đầu từ 0, phải trừ 1 khi gọi.
- **CORS**: Vite chạy cổng 5173, backend mặc định `CORS_ALLOWED_ORIGINS=http://localhost:3000`.
  Khi chạy backend cần đặt biến môi trường `CORS_ALLOWED_ORIGINS=http://localhost:5173`.
- **Vai trò**: backend trả `STAFF` / `MANAGER` / `ADMIN` (chữ hoa), frontend dùng
  `staff` / `manager` / `admin` (chữ thường). `RoleGuard` so sánh trực tiếp nên phải hạ chữ
  thường khi lưu người dùng.
- **`branchId` chưa có trong response đăng nhập**: `LoginResponse` không trả `branchId` trong
  khi quy ước dữ liệu yêu cầu staff và manager chỉ thấy dữ liệu chi nhánh của mình.
  Cần bổ sung ở backend, không cần sửa schema vì cột `NhanVien.MaChiNhanh` đã có sẵn.
- **Lint**: bản ESLint dùng `eslint-plugin-react-hooks` bản mới có luật
  `react-hooks/set-state-in-effect` chặn `setState` đồng bộ trong effect. Các trang dùng
  pattern suy luận trạng thái tải từ khoá request, chỉ gọi `setState` trong callback của promise.
- **Thanh cuộn của menu bên**: thanh cuộn mặc định của Windows vẽ nền xám sáng trên nền tối
  của sidebar nên rất lóe. Đã thêm class `.sidebar-scroll` trong `frontend/src/index.css`
  (ẩn track, thumb mảnh 6px hòa màu `--color-card`) và gắn vào `<nav>` của `Sidebar.jsx`.
  Class này dùng lại được cho mọi vùng nền tối sau này.
- **Dữ liệu giả có tính ổn định**: `mockData.js` dùng bộ sinh số giả tất định nên reload trang
  không làm đổi số liệu. Tồn kho giả được giữ trong bộ nhớ nên thao tác "nhập kho" có hiệu lực
  trong suốt phiên làm việc. Khi chuyển sang API thật, chỉ thay phần thân hàm trong service.
