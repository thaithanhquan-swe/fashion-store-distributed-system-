-- =====================================================================
-- FASHION_CENTRAL — CENTRAL DATABASE
-- Central node: full master data, replication publisher, and
-- cross-branch aggregation views.
-- PREREQUISITE: run `linked-servers.sql` (role = central) first.
-- Distributed procedures live in `central-procs.sql` and must run after
-- the 3 site databases exist.
-- Run on the Central machine, from the `database/` folder:
--     sqlcmd -S <instance> -E -i Fashion_Central.sql -b -f 65001
-- =====================================================================

USE [master]
GO

-- Password of the shared replication login (MUST match the 3 sites).
:setvar DistributedPassword "Fashion@123456"

-- -------------------------------------------------
-- 1. CREATE DATABASE Fashion_Central (portable: default file locations)
-- -------------------------------------------------
IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'Fashion_Central')
BEGIN
    ALTER DATABASE [Fashion_Central] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [Fashion_Central];
END
GO

CREATE DATABASE [Fashion_Central];
GO

-- FULL recovery supports Merge replication (snapshot + change tracking).
ALTER DATABASE [Fashion_Central] SET RECOVERY FULL;
GO

USE [Fashion_Central];
GO

-- -------------------------------------------------
-- 2. REPLICATION LOGIN/USER (identical on all 4 machines)
-- -------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'distributed_user')
BEGIN
    CREATE LOGIN [distributed_user] WITH PASSWORD = N'$(DistributedPassword)', CHECK_POLICY = OFF;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'distributed_user')
BEGIN
    CREATE USER [distributed_user] FOR LOGIN [distributed_user] WITH DEFAULT_SCHEMA=[dbo];
END
GO

ALTER ROLE [db_datareader] ADD MEMBER [distributed_user];
ALTER ROLE [db_datawriter] ADD MEMBER [distributed_user];
GO

-- -------------------------------------------------
-- 3. RUN COMMON DDL (8 tables, FKs, indexes, SP/trigger templates)
--    NOTE: sqlcmd resolves :r relative to the CURRENT DIRECTORY, so run
--    this script from the `database/` folder (see the header above).
-- -------------------------------------------------
:r common.sql

-- -------------------------------------------------
-- 4. CENTRAL MASTER DATA: All 4 branches
-- -------------------------------------------------
INSERT INTO [dbo].[ChiNhanh] ([MaChiNhanh], [TenChiNhanh], [DiaChi], [SoDienThoai], [TrangThai]) VALUES
(N'CN1', N'Store Quận 1',     N'123 Đường A, Quận 1, TP.HCM',     N'028-38xx-xxxx', 1),
(N'CN2', N'Store Quận 7',     N'456 Đường B, Quận 7, TP.HCM',     N'028-54xx-xxxx', 1),
(N'CN3', N'Store Thủ Đức',    N'789 Đường C, Thủ Đức, TP.HCM',    N'028-71xx-xxxx', 1),
(N'CN0', N'Trụ sở chính',     N'000 Đường D, Quận 1, TP.HCM',     N'028-39xx-xxxx', 1);
GO

-- -------------------------------------------------
-- 5. LINKED SERVERS
--    Created by `database/linked-servers.sql`, which must run BEFORE this
--    script because the distributed procedures reference those names.
--    Transparency rule (b): names are fixed —
--      Central    -> SITE_Q1, SITE_Q7, SITE_THUDUC
--      each site  -> CENTRAL
--    Verify with:
--      SELECT name, data_source FROM sys.servers WHERE is_linked = 1;
-- -------------------------------------------------

-- -------------------------------------------------
-- 6. CENTRAL TRIGGERS: Guard DELETE on replicated tables
--    Prevents direct deletes on Central for tables that are replicated from sites.
-- -------------------------------------------------
CREATE TRIGGER [dbo].[trg_SanPham_GuardDelete]
ON [dbo].[SanPham]
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF SYSTEM_USER = 'distributed_user'
    BEGIN
        DELETE t
        FROM [dbo].[SanPham] t
        JOIN deleted d ON t.MaSanPham = d.MaSanPham;
        RETURN;
    END;
    RAISERROR (N'Không được xóa SanPham trực tiếp trên Central. Dữ liệu được đồng bộ từ các site.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO

CREATE TRIGGER [dbo].[trg_KhachHang_GuardDelete]
ON [dbo].[KhachHang]
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF SYSTEM_USER = 'distributed_user'
    BEGIN
        DELETE t
        FROM [dbo].[KhachHang] t
        JOIN deleted d ON t.MaKhachHang = d.MaKhachHang;
        RETURN;
    END;
    RAISERROR (N'Không được xóa KhachHang trực tiếp trên Central. Dữ liệu được đồng bộ 2 chiều.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO

CREATE TRIGGER [dbo].[trg_NhanVien_GuardDelete]
ON [dbo].[NhanVien]
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF SYSTEM_USER = 'distributed_user'
    BEGIN
        DELETE t
        FROM [dbo].[NhanVien] t
        JOIN deleted d ON t.MaNhanVien = d.MaNhanVien;
        RETURN;
    END;
    RAISERROR (N'Không được xóa NhanVien trực tiếp trên Central. Dữ liệu được đồng bộ từ các site.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO

CREATE TRIGGER [dbo].[trg_TaiKhoan_GuardDelete]
ON [dbo].[TaiKhoan]
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF SYSTEM_USER = 'distributed_user'
    BEGIN
        DELETE t
        FROM [dbo].[TaiKhoan] t
        JOIN deleted d ON t.MaTaiKhoan = d.MaTaiKhoan;
        RETURN;
    END;
    RAISERROR (N'Không được xóa TaiKhoan trực tiếp trên Central. Dữ liệu được đồng bộ từ các site.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO

-- -------------------------------------------------
-- 7. REPLICATION SETUP — see `database/replication.sql`
--    Chốt B1 (Merge Replication). Chạy script `database/replication.sql`
--    trên Central sau khi linked servers và database đã sẵn sàng.
--    Không cần chạy các lệnh dưới đây (đã tổng hợp trong replication.sql).
-- -------------------------------------------------
/*
-- STEP 1: Configure Distributor (run once on Central) — xem replication.sql
-- STEP 2: Create Publications — xem replication.sql (sp_addmergepublication)
--   Pub_Common: SanPham, ChiNhanh, KhachHang (không lọc, push tat ca site)
--   Pub_Fashion_Store_Q1/Q7/ThuDuc: NhanVien, TaiKhoan, TonKho, HoaDon,
--     ChiTietHoaDon (loc MaChiNhanh, join filter ChiTietHoaDon -> HoaDon,
--     TaiKhoan -> NhanVien)
-- STEP 3: Create Subscriptions — xem replication.sql (sp_addmergesubscription +
--   sp_addmergepushsubscription_agent)
-- STEP 4: Start snapshot agents — xem replication.sql (sp_startpublication_snapshot)
-- STEP 5: Share snapshot folder cho tat ca machine
*/

-- -------------------------------------------------
-- 8. AGGREGATION VIEWS (Central-only)
-- -------------------------------------------------

-- v_TonKho_ToanBo: Inventory across all branches
CREATE OR ALTER VIEW [dbo].[v_TonKho_ToanBo]
AS
SELECT
    tk.MaChiNhanh,
    cn.TenChiNhanh,
    tk.MaSanPham,
    sp.TenSanPham,
    tk.SoLuongTon,
    tk.MucTonToiThieu,
    tk.NgayCapNhat,
    CASE
        WHEN tk.SoLuongTon = 0 THEN N'HẾT HÀNG'
        WHEN tk.SoLuongTon <= tk.MucTonToiThieu THEN N'SẮP HẾT'
        ELSE N'BÌNH THƯỜNG'
    END AS TrangThaiTon
FROM [dbo].[TonKho] tk
JOIN [dbo].[ChiNhanh] cn ON tk.MaChiNhanh = cn.MaChiNhanh
JOIN [dbo].[SanPham] sp ON tk.MaSanPham = sp.MaSanPham;
GO

-- v_HoaDon_ToanBo: All invoices with branch names
CREATE OR ALTER VIEW [dbo].[v_HoaDon_ToanBo]
AS
SELECT
    h.MaHoaDon,
    h.MaChiNhanh,
    cn.TenChiNhanh,
    h.MaKhachHang,
    kh.HoTen AS TenKhachHang,
    h.MaNhanVien,
    nv.HoTen AS TenNhanVien,
    h.NgayLap,
    h.TongTien,
    h.PhuongThucTT,
    h.TrangThai
FROM [dbo].[HoaDon] h
JOIN [dbo].[ChiNhanh] cn ON h.MaChiNhanh = cn.MaChiNhanh
LEFT JOIN [dbo].[KhachHang] kh ON h.MaKhachHang = kh.MaKhachHang
JOIN [dbo].[NhanVien] nv ON h.MaNhanVien = nv.MaNhanVien;
GO

-- v_DoanhThu_TheoChiNhanh: Revenue per branch
CREATE OR ALTER VIEW [dbo].[v_DoanhThu_TheoChiNhanh]
AS
SELECT
    cn.MaChiNhanh,
    cn.TenChiNhanh,
    COUNT(h.MaHoaDon) AS SoHoaDon,
    SUM(h.TongTien) AS TongDoanhThu,
    MIN(h.NgayLap) AS NgayDauTien,
    MAX(h.NgayLap) AS NgayMoiNhat
FROM [dbo].[HoaDon] h
JOIN [dbo].[ChiNhanh] cn ON h.MaChiNhanh = cn.MaChiNhanh
WHERE h.TrangThai = N'DA_THANH_TOAN'
GROUP BY cn.MaChiNhanh, cn.TenChiNhanh;
GO

-- -------------------------------------------------
-- 9. FINAL SETTINGS
-- -------------------------------------------------
ALTER DATABASE [Fashion_Central] SET READ_WRITE;
GO

PRINT N'Fashion_Central database created with linked server templates, guards, and aggregation views.';
PRINT N'Next steps:';
PRINT N'  1. Run linked-servers.sql (role = central) if not done.';
PRINT N'  2. Create the 3 site databases, then run central-procs.sql.';
PRINT N'  3. Run seed.sql to populate initial data.';
PRINT N'  4. Configure replication: replication.sql.';
GO