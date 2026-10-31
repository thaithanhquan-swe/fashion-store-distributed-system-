-- =====================================================================
-- FASHION_STORE — SITE DATABASE (Quận 1 / Quận 7 / Thủ Đức)
-- Identical database name on all 3 sites (transparency rule): Fashion_Store
-- PREREQUISITE: run `linked-servers.sql` (role = site) first.
-- Run on EACH site machine, from the `database/` folder:
--     sqlcmd -S <instance> -E -i sites\Fashion_Store.sql -b -f 65001
-- The script creates the DB, runs common.sql (8 tables), adds this site's
-- branch row, read-only guards, and the local inventory procedure.
-- Distributed procedures (linked server CENTRAL) live in `sites/site-procs.sql`
-- and must be run after Central's database exists.
-- =====================================================================

USE [master]
GO

-- -------------------------------------------------
-- CONFIG — EDIT PER SITE BEFORE RUNNING
-- Keep exactly ONE site block active (uncomment the one you need).
-- -------------------------------------------------

-- QUẬN 1 (CN1)
:setvar SiteBranch "CN1"
:setvar SiteBranchName "Store Quận 1"
:setvar SiteAddress "123 Đường A, Quận 1, TP.HCM"
:setvar SitePhone "028-38xx-xxxx"
:setvar SiteLinkedServerName "SITE_Q1"

-- QUẬN 7 (CN2)
--:setvar SiteBranch "CN2"
--:setvar SiteBranchName "Store Quận 7"
--:setvar SiteAddress "456 Đường B, Quận 7, TP.HCM"
--:setvar SitePhone "028-54xx-xxxx"
--:setvar SiteLinkedServerName "SITE_Q7"

-- THỦ ĐỨC (CN3)
--:setvar SiteBranch "CN3"
--:setvar SiteBranchName "Store Thủ Đức"
--:setvar SiteAddress "789 Đường C, Thủ Đức, TP.HCM"
--:setvar SitePhone "028-71xx-xxxx"
--:setvar SiteLinkedServerName "SITE_THUDUC"

-- Password of the shared replication login (MUST match Central).
:setvar DistributedPassword "Fashion@123456"

-- -------------------------------------------------
-- 1. CREATE DATABASE Fashion_Store (portable: default file locations)
-- -------------------------------------------------
IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'Fashion_Store')
BEGIN
    ALTER DATABASE [Fashion_Store] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [Fashion_Store];
END
GO

CREATE DATABASE [Fashion_Store];
GO

-- FULL recovery is required for transactional replication.
ALTER DATABASE [Fashion_Store] SET RECOVERY FULL;
GO

USE [Fashion_Store];
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
-- 4. SITE-SPECIFIC DATA: ChiNhanh (only this site's branch)
-- -------------------------------------------------
INSERT INTO [dbo].[ChiNhanh] ([MaChiNhanh], [TenChiNhanh], [DiaChi], [SoDienThoai], [TrangThai])
VALUES (N'$(SiteBranch)', N'$(SiteBranchName)', N'$(SiteAddress)', N'$(SitePhone)', 1);
GO

-- -------------------------------------------------
-- 5. SITE TRIGGER: Validate HoaDon belongs to this branch
-- -------------------------------------------------
CREATE TRIGGER [dbo].[trg_HoaDon_ValidateBranch]
ON [dbo].[HoaDon]
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM inserted i
        WHERE i.MaChiNhanh <> N'$(SiteBranch)'
    )
    BEGIN
        RAISERROR (N'Chỉ được tạo/sửa hóa đơn của chi nhánh hiện tại (%s).', 16, 1, N'$(SiteBranch)');
        ROLLBACK TRANSACTION;
    END
END;
GO

-- -------------------------------------------------
-- 6. SITE TRIGGER: Prevent direct DELETE on replicated tables (read-only on site)
--    Replicated tables: SanPham, KhachHang, NhanVien, TaiKhoan
-- -------------------------------------------------
CREATE TRIGGER [dbo].[trg_SanPham_NoDelete]
ON [dbo].[SanPham]
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;
    RAISERROR (N'Bảng SanPham là chỉ đọc trên site. Dữ liệu đồng bộ từ Central.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO

CREATE TRIGGER [dbo].[trg_KhachHang_NoDelete]
ON [dbo].[KhachHang]
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;
    RAISERROR (N'Bảng KhachHang được đồng bộ 2 chiều. Không xóa trực tiếp trên site.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO

CREATE TRIGGER [dbo].[trg_NhanVien_NoDelete]
ON [dbo].[NhanVien]
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;
    RAISERROR (N'Bảng NhanVien là chỉ đọc trên site. Dữ liệu đồng bộ từ Central.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO

CREATE TRIGGER [dbo].[trg_TaiKhoan_NoDelete]
ON [dbo].[TaiKhoan]
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;
    RAISERROR (N'Bảng TaiKhoan là chỉ đọc trên site. Dữ liệu đồng bộ từ Central.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO

-- -------------------------------------------------
-- 7. LOCAL STORED PROCEDURES (no linked server)
-- -------------------------------------------------

-- sp_DongBoSanPham: No-op on site (SanPham is read-only replica from Central)
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoSanPham]
AS
BEGIN
    SET NOCOUNT ON;
    PRINT N'Site: SanPham is read-only. Central pushes changes via linked server.';
END;
GO

-- sp_CapNhatTonKho: Local inventory update (called by POS/backend)
CREATE OR ALTER PROCEDURE [dbo].[sp_CapNhatTonKho]
    @MaSanPham varchar(10),
    @SoLuongThayDoi int  -- positive for intake, negative for sale
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [dbo].[TonKho]
    SET SoLuongTon = SoLuongTon + @SoLuongThayDoi,
        NgayCapNhat = GETDATE()
    WHERE MaChiNhanh = N'$(SiteBranch)' AND MaSanPham = @MaSanPham;

    IF @@ROWCOUNT = 0
    BEGIN
        -- Auto-create if missing (initial stock = 0 + change)
        INSERT INTO [dbo].[TonKho] (MaChiNhanh, MaSanPham, SoLuongTon, MucTonToiThieu, NgayCapNhat)
        VALUES (N'$(SiteBranch)', @MaSanPham, @SoLuongThayDoi, 5, GETDATE());
    END

    -- Return updated row
    SELECT MaChiNhanh, MaSanPham, SoLuongTon, MucTonToiThieu, NgayCapNhat
    FROM [dbo].[TonKho]
    WHERE MaChiNhanh = N'$(SiteBranch)' AND MaSanPham = @MaSanPham;
END;
GO

-- -------------------------------------------------
-- 8. FINAL SETTINGS
-- -------------------------------------------------
ALTER DATABASE [Fashion_Store] SET READ_WRITE;
GO

PRINT N'Fashion_Store site database created for branch $(SiteBranch) ($(SiteBranchName)).';
PRINT N'Next steps:';
PRINT N'  1. Create the linked server CENTRAL: linked-servers.sql (role = site).';
PRINT N'  2. After Central has Fashion_Central, run sites/site-procs.sql.';
PRINT N'  3. Configure replication: Publication (Central) -> Subscription (this site).';
PRINT N'  4. Run seed.sql on Central to populate initial data.';
GO