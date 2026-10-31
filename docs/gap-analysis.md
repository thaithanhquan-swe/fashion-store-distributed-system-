# Đối chiếu với mẫu đồ án của giảng viên và kế hoạch khắc phục

Tài liệu này ghi lại: mẫu giảng viên (GV) yêu cầu gì, dự án hiện lệch ở đâu, và thứ tự
làm để khớp. Nội dung lấy từ file mẫu
`[6][4]TONG HOP TOM TAT CAI DAT CSDL PT - QUAN LY THUC THI GIAO DICH- SV.pdf`.

Phạm vi hiện tại của nhóm: **làm trước phần database, backend, frontend**. Phần phân tán
(replication, linked server, stored procedure phân tán) làm sau, nhưng các quyết định dưới
đây cần chốt sớm vì sửa lại schema sau này rất tốn công.

> ## ⚠️ CẬP NHẬT 2026-10-29 (đối chiếu commit `e2b0e3f`)
>
> File này ban đầu viết khi backend/frontend còn sơ khai. **Trạng thái hiện tại đã khác nhiều**;
> nguồn sự thật duy nhất là [`docs/PROJECT_STATUS.md`](PROJECT_STATUS.md). Đọc mục đó trước.
>
> **Đã xong từ lúc viết file này:**
> - Backend: 12 controller, 50 endpoint, 73 test pass; đã có entity `TonKho`, `ChiTietHoaDon`,
>   API tồn kho, bán hàng (`POST /api/sales/checkout`), hóa đơn, dashboard 3 role, thống kê 5 endpoint,
>   `branchId` trong `LoginResponse`, CORS 5173.
> - Frontend: đủ 30 file trang (27 route menu + 7 route chi tiết); **10/11 service gọi API thật**
>   qua `src/api/client.js` (Bearer + refresh token + gom phân trang); chỉ `siteService` còn mock.
> - Database: đã có `database/Fashion_Central.sql` (8 bảng + `distributed_user` + `sp_DongBoSanPham`).
> - Tài liệu: `docs/setup.md` đã viết lại thành runbook 4 máy; có `docs/PROJECT_STATUS.md`.
>
> **Vẫn còn nguyên (phần phân tán ~0%):** publication/subscription, linked server, trigger,
> `SyncQueue`, SP `UNION` xuyên site, seed dữ liệu, định danh toàn cục, UTF-8 cho file SQL.
>
> **Phát sinh mới sau khi pull:**
> - `application.properties` bị gitignore và **không còn trong repo**; chỉ có
>   `application-local.properties`, mà Spring Boot chỉ đọc khi bật profile `local`.
> - Mã chi nhánh lệch: FE hardcode `CN001/CN002/CN003` + "Store Quận 3"; đề bài và `AGENTS.md`
>   dùng `CN01/CN02/CN03` (Quận 1 / Quận 7 / Thủ Đức). Alias mới chỉ có ở `BranchService`/`InventoryService`.
> - `sp_DongBoSanPham` dùng tên 3 phần `Fashion_Q1.dbo.*` (cần linked server tên `Fashion_Q1`),
>   lệch quy ước `SITE_Q1`; chỉ đồng bộ cột `TenSanPham`.

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
| 11a | Chức năng nghiệp vụ (backend) | ✅ **Đã xong phần ứng dụng**: auth + phân quyền, nhân viên, tài khoản, chi nhánh, sản phẩm, khách hàng, tồn kho, bán hàng, chi tiết hóa đơn, hóa đơn, dashboard 3 role, thống kê. **Còn thiếu**: trạng thái site, API tổng hợp phân tán, idempotency checkout, cấu hình theo máy | ~90% |
| 11b | Giao diện (frontend) | ✅ 30 file trang, đủ 27 route menu + 7 route chi tiết; 10/11 service gọi API thật. Xem mục 5 bên dưới | ~95% |

**Cập nhật 2026-10-29: khoảng 50–55% nếu tính cả phần phân tán (phân tán vẫn ~0%).
Riêng database + backend + frontend: khoảng 85–88%.** Con số 38% / 60% bên dưới là mốc lịch sử
lúc mới viết file, giữ lại để đối chiếu.

---

## 3. Các điểm lệch cần sửa sớm

### A. Phải quyết định trước khi làm tiếp (sửa sau rất tốn)

| # | Vấn đề | Bằng chứng | Tác động / Trạng thái |
| --- | --- | --- | --- |
| A1 | ~~**Tên CSDL khác nhau ở các Subscriber**~~ | `Fashion_Store.sql` | ✅ **Đã chốt**: Cả 3 site dùng chung `Fashion_Store`, Central là `Fashion_Central`. |
| A2 | ~~**Tên Linked Server khác nhau**~~ | `database/linked-servers.sql` | ✅ **Đã chốt**: Tên link đặt theo đích (`SITE_Q1`, `SITE_Q7`, `SITE_THUDUC`, `CENTRAL`). |
| A3 | ~~**`KhachHang.MaKhachHang` không có định danh toàn hệ thống**~~ | `seed.sql` | ✅ **Đã chốt**: Tiền tố site (`Q1-KH...`, `Q7-KH...`, `TD-KH...`) chống đụng khóa chính. |
| A4 | ~~**`KhachHang` có ràng buộc `UNIQUE(SoDienThoai)`**~~ | `database/common.sql` | ✅ **Đã xử lý (D10)**: Bỏ constraint UNIQUE, đổi sang Non-Clustered Index `IX_KhachHang_SoDienThoai`. |
| A5 | ~~**Thiếu cột version / timestamp**~~ | `database/common.sql` | ✅ **Đã xử lý (D11)**: Thêm cột `LastModified DATETIME2(3) DEFAULT SYSUTCDATETIME()`, hỗ trợ Last-In-Wins. |
| A6 | **`HoaDon.MaHoaDon` quy tắc sinh mã toàn hệ thống** | `seed.sql` dùng `Q1-HD...` | 🟡 Seed đã chuẩn hóa tiền tố; backend `SalesService` cần áp dụng logic sinh mã theo site. |
| A7 | **Tách rõ bảng chung và bảng riêng** | `Fashion_Central.sql` vs `Fashion_Store.sql` | ✅ Đã tách bảng chung (`SanPham`, `ChiNhanh`) và bảng riêng (`TonKho`, `HoaDon`, `NhanVien`). |
| A8 | ~~**`Fashion_TD.sql` tạo tên database `Fashion_TD`**~~ | `sites/Fashion_Store.sql` | ✅ Đã thay bằng `Fashion_Store` cho cả 3 site. |
| A9 | ~~**Thiếu `distributed_user` ở site**~~ | `Fashion_Store.sql`, `Fashion_Central.sql` | ✅ Đã tích hợp khối tạo login/user `distributed_user` với quyền `db_datareader` + `db_datawriter`. |
| A10 | **`TaiKhoan.MaTaiKhoan` là `INT IDENTITY`** | `common.sql` | Ổn vì bảng local của từng site hoặc replicate read-only từ Central. |
| A11 | ~~**Backend thiếu entity `TonKho` và `ChiTietHoaDon`**~~ | ✅ **Đã xong**: `entity/TonKho.java`, `entity/ChiTietHoaDon.java` + repository + `SalesService` | — |
| A12 | **`GeneratedId` quét toàn bảng để sinh mã** | `backend/.../service/GeneratedId.java` | Cần cập nhật sinh mã theo tiền tố site. |

### B. Có thể để sau, nhưng phải biết trước

| # | Vấn đề | Ghi chú |
| --- | --- | --- |
| B1 | ~~Không có dữ liệu mẫu~~ | ✅ **Đã xong**: `database/seed.sql` đã có **100 sản phẩm**, **300 dòng tồn kho**, 15 KH, 10 NV, 10 tài khoản, 7 hóa đơn mẫu. |
| B2 | ~~`database/` không có `central.sql` và `common.sql`~~ | ✅ **Đã xong**: Đã có `Fashion_Central.sql`, `common.sql`, `linked-servers.sql`, `central-procs.sql`, `sites/site-procs.sql`, `replication.sql`. |
| B3 | ~~File `.sql` thiếu `.gitattributes`~~ | ✅ **Đã xong**: Đã tạo `.gitattributes` ép mã hóa UTF-8. Các script SQL mới đều là UTF-8. |
| B4 | Script không idempotent | 🟡 `Fashion_Central.sql` & `Fashion_Store.sql` đã có `DROP IF EXISTS`; `linked-servers.sql` có `sp_dropserver`. |
| B5 | ~~Chưa có hướng dẫn SQL Server Agent, edition, mixed auth~~ | ✅ **Đã xong** trong `docs/setup.md` (runbook 4 máy). |
| B6 | ~~Chưa có shared folder cho snapshot~~ | ✅ **Đã có hướng dẫn** `\\<CENTRAL>\REPLDATA`, Everyone Read/Write trong `docs/setup.md`. |
| B7 | Chưa có bảng log / queue đồng bộ ở Central | Có mẫu `SyncQueue` trong `docs/setup.md` mục 8.1. |
| B8 | ~~Chưa có test cho backend~~ | ✅ **Đã có 73 test / 11 lớp** (`mvnw.cmd test` pass). |
| B9 | ~~`docs/architecture.md` mô tả sai thực tế file SQL~~ | ✅ **Đã sửa**: mô tả đúng các file `database/`. |

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
├── Fashion_Central.sql # Fashion_Central + linked server tới ba site (đã có bảng, thiếu linked server)
├── Fashion_Q1.sql      # đã có
├── Fashion_Q7.sql      # đã có
├── Fashion_TD.sql      # đã có, cần đổi tên Fashion_ThuDuc
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

> **Cập nhật 2026-10-29:** mục 1, 2, 4, 7, 8, 10 đã xong (mục 3 làm bằng service transaction
> thay vì `usp_BanHang`; mục 5, 6, 9, 11 còn mở).

1. ✅ Thêm entity `TonKho` và `ChiTietHoaDon` kèm repository và specification.
2. ✅ Thêm API tồn kho: danh sách, sắp hết hàng, nhập kho, tồn theo chi nhánh.
3. 🟡 Thêm API bán hàng — hiện dùng `SalesService` (`POST /api/sales/checkout`) thay vì gọi
   stored procedure `usp_BanHang`; chưa có idempotency.
4. ✅ Thêm API thống kê và báo cáo (doanh thu theo khoảng ngày, top sản phẩm, doanh số nhân viên).
5. ❌ Thêm API trạng thái site (ping từng linked server).
6. ❌ Thêm API tổng hợp phân tán ở Central.
7. ✅ Bổ sung `branchId` vào `LoginResponse` (cột `NhanVien.MaChiNhanh` đã có sẵn).
8. ✅ Thêm các API GET còn thiếu: `/employees`, `/accounts`, `/branches`, `/api/invoices`.
9. ❌ Sửa `GeneratedId`: sinh mã có tiền tố site, không quét toàn bảng.
10. 🟡 Thêm test — 73 test / 11 lớp đã pass; còn thiếu test inventory, sales, product, customer, auth.
11. 🟡 Tách profile cấu hình theo vai trò máy — mới có `application-local.properties`.

### Giai đoạn 3 - Frontend

✅ **Cơ bản xong** (xem mục 5): đủ 30 trang, 10/11 service gọi API thật, lint + build sạch.
Còn lại: `siteService` (chờ API trạng thái site), rà trạng thái tải/lỗi/rỗng từng trang,
và chốt mã chi nhánh ở `config/branches.js`.

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

## 5. Trạng thái frontend hiện tại (cập nhật 2026-10-29)

### 5.1 Toàn bộ giao diện đã có và đang gọi API thật

30 file `.jsx` trong `frontend/src/pages/`, phủ **27 route menu** (`config/nav.js`) + **7 route chi tiết**
(`EXTRA_PAGES` trong `App.jsx`). Không còn trang "đang xây dựng".

**10/11 service gọi API thật** qua `frontend/src/api/client.js`:

| Service | Endpoint chính |
| --- | --- |
| `authService` | `POST /api/auth/login`, `/refresh_token`, `/change-password` |
| `accountService` | `/api/accounts` (+ role/status/reset-password) |
| `branchService` | `/api/branches`, ghép số liệu từ employee/inventory/statistics |
| `customerService` | `/api/customers` (+ lịch sử hóa đơn, bật/tắt hoạt động) |
| `dashboardService` | `/api/dashboard/admin`, `/manager`, `/staff` |
| `employeeService` | `/api/employees` |
| `inventoryService` | `/api/inventory` (+ nhập/điều chỉnh, sắp hết) |
| `invoiceService` | `/api/invoices` + `POST /api/sales/checkout` |
| `productService` | `/api/products` |
| `statisticsService` | `/api/statistics/*` (doanh thu, chuỗi ngày, chi nhánh, top SP, nhân viên) |
| ~~`siteService`~~ | ❌ **vẫn mock** — chờ API trạng thái site (P3-03) |

Hạ tầng dùng chung:
- `src/api/client.js`: gắn `Bearer`, tự refresh khi 401 một lần, phát `SESSION_EXPIRED_EVENT`,
  `getAllPages()` gom phân trang (BE 0-based → FE 1-based), `buildQuery()`.
- `src/context/AuthContext.jsx` nghe sự kiện hết phiên để đăng xuất.
- Mỗi service có hàm `to*` đổi shape backend → shape trang đang dùng, nên **không phải sửa trang**
  khi API đổi tên trường.

### 5.2 Trang còn phụ thuộc dữ liệu giả / chưa hoàn chỉnh

| Trang | Route | Tình trạng |
| --- | --- | --- |
| Trạng thái Site | `/admin/sites` | Dùng `siteService` mock; cần API ping linked server |
| Tồn kho toàn chuỗi (Admin) | `/admin/inventory` tab "Toàn chuỗi" | Ghép từ `/api/inventory` theo chi nhánh; chưa có endpoint tổng hợp thật |
| Chi tiết khách hàng | `/staff/customers/:id`, ... | Đã gọi API; cần kiểm tra lại trạng thái lỗi/rỗng sau khi có seed |

### 5.3 Ghi chú kỹ thuật cho frontend

- **Contract API**: mọi API trả bọc `{ code, message, result }`. Danh sách trả
  `result = { content, pageNumber, pageSize, totalElements, totalPages, last }`.
  `client.js` đã trừ 1 khi gọi và `Pagination` đánh số từ 1.
- **CORS**: `app.cors.allowed-origins=http://localhost:5173` trong `application-local.properties` —
  đã khớp Vite, không cần đặt biến môi trường như ghi chú cũ.
- **Vai trò**: backend trả `STAFF` / `MANAGER` / `ADMIN`; `authService` hạ chữ thường.
- **`branchId`**: ✅ `LoginResponse` đã trả `branchId`; `authService` lưu vào user (Admin = `null`).
- **Mã chi nhánh**: ⚠️ `config/branches.js` đang hardcode `CN001/CN002/CN003` và "Store Quận 3".
  Đề bài + `AGENTS.md` dùng `CN01/CN02/CN03` (Quận 1 / Quận 7 / Thủ Đức) → phải chốt (Q10 trong
  `PROJECT_STATUS.md`) trước khi seed.
- **Lint**: `eslint-plugin-react-hooks` bản mới có luật `react-hooks/set-state-in-effect`;
  `useAsyncData` suy luận `loading` từ khoá request, chỉ `setState` trong callback promise.
- **Thanh cuộn sidebar**: class `.sidebar-scroll` trong `index.css`, gắn vào `<nav>` của `Sidebar.jsx`.
- **Mock còn lại**: `mockData.js` + `mockStore.js` chỉ còn `siteService` dùng; giữ lại để demo offline.
  Khi xong P4-10 thì xoá dần (P4-11).
