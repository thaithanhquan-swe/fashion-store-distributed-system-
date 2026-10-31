-- =====================================================================
-- COMMON DDL — shared by Central (Fashion_Central) and all 3 Sites (Fashion_Store)
-- Run this on each of the 4 SQL Server instances before site-specific scripts.
-- Encoding: UTF-8 (recommended). Original files were UTF-16 LE; this one is UTF-8.
-- =====================================================================

-- -------------------------------------------------
-- 1. CORE TABLES (8 tables, identical schema everywhere)
-- -------------------------------------------------

-- ChiNhanh: Branch master. Central has 4 rows (CN1, CN2, CN3, CN0). Site has 1 row (its own).
CREATE TABLE [dbo].[ChiNhanh] (
    [MaChiNhanh] [varchar](10) NOT NULL,
    [TenChiNhanh] [nvarchar](100) NOT NULL,
    [DiaChi] [nvarchar](255) NULL,
    [SoDienThoai] [varchar](15) NULL,
    [TrangThai] [bit] NOT NULL DEFAULT 1,
    CONSTRAINT [PK_ChiNhanh] PRIMARY KEY CLUSTERED ([MaChiNhanh] ASC)
) ON [PRIMARY];
GO

-- SanPham: Product master. Central = full catalog (read-write). Sites = replicated subset (read-only).
CREATE TABLE [dbo].[SanPham] (
    [MaSanPham] [varchar](10) NOT NULL,
    [TenSanPham] [nvarchar](150) NOT NULL,
    [LoaiSanPham] [nvarchar](50) NULL,
    [ThuongHieu] [nvarchar](100) NULL,
    [MauSac] [nvarchar](30) NULL,
    [KichThuoc] [varchar](10) NULL,
    [GiaBan] [decimal](18, 2) NOT NULL DEFAULT 0 CHECK ([GiaBan] >= 0),
    [TrangThai] [bit] NOT NULL DEFAULT 1,
    [LastModified] [datetime2](3) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT [PK_SanPham] PRIMARY KEY CLUSTERED ([MaSanPham] ASC)
) ON [PRIMARY];
GO

-- KhachHang: Customer master. REPLICATED TO ALL SITES (two-way per PDF §3.6).
-- No MaChiNhanh column — same customer exists across all sites.
-- D10: Bỏ UNIQUE trên SoDienThoai để tránh xung đột khi replicate/đồng bộ chéo giữa các site;
--      thay bằng nonclustered index để tìm kiếm số điện thoại nhanh.
-- D11: Thêm cột LastModified để phục vụ giải quyết xung đột (conflict resolution / Last-In-Wins).
CREATE TABLE [dbo].[KhachHang] (
    [MaKhachHang] [varchar](10) NOT NULL,
    [HoTen] [nvarchar](100) NOT NULL,
    [SoDienThoai] [varchar](15) NULL,
    [Email] [varchar](100) NULL,
    [DiaChi] [nvarchar](255) NULL,
    [DiemTichLuy] [int] NOT NULL DEFAULT 0,
    [NgayDangKy] [date] NOT NULL DEFAULT CAST(GETDATE() AS date),
    [TrangThai] [bit] NOT NULL DEFAULT 1,
    [LastModified] [datetime2](3) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT [PK_KhachHang] PRIMARY KEY CLUSTERED ([MaKhachHang] ASC)
) ON [PRIMARY];
GO

CREATE NONCLUSTERED INDEX [IX_KhachHang_SoDienThoai] ON [dbo].[KhachHang] ([SoDienThoai] ASC);
GO

-- NhanVien: Employee master. Central = all. Site = only employees of that branch (replicated).
CREATE TABLE [dbo].[NhanVien] (
    [MaNhanVien] [varchar](10) NOT NULL,
    [MaChiNhanh] [varchar](10) NOT NULL,
    [HoTen] [nvarchar](100) NOT NULL,
    [NgaySinh] [date] NULL,
    [GioiTinh] [nvarchar](10) NULL,
    [SoDienThoai] [varchar](15) NULL,
    [Email] [varchar](100) NULL,
    [ChucVu] [nvarchar](50) NULL,
    [TrangThai] [bit] NOT NULL DEFAULT 1,
    [LastModified] [datetime2](3) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT [PK_NhanVien] PRIMARY KEY CLUSTERED ([MaNhanVien] ASC)
) ON [PRIMARY];
GO

-- TaiKhoan: Login accounts. Central = all. Site = replicated (read-only) for local login.
-- Filtered via NhanVien.MaChiNhanh at replication time.
CREATE TABLE [dbo].[TaiKhoan] (
    [MaTaiKhoan] [int] IDENTITY(1,1) NOT NULL,
    [MaNhanVien] [varchar](10) NOT NULL,
    [TenDangNhap] [varchar](50) NOT NULL,
    [MatKhau] [varchar](255) NOT NULL,
    [VaiTro] [varchar](20) NOT NULL CHECK ([VaiTro] IN ('STAFF', 'MANAGER', 'ADMIN')),
    [TrangThai] [bit] NOT NULL DEFAULT 1,
    CONSTRAINT [PK_TaiKhoan] PRIMARY KEY CLUSTERED ([MaTaiKhoan] ASC),
    CONSTRAINT [UQ_TaiKhoan_TenDangNhap] UNIQUE NONCLUSTERED ([TenDangNhap] ASC),
    CONSTRAINT [UQ_TaiKhoan_MaNhanVien] UNIQUE NONCLUSTERED ([MaNhanVien] ASC)
) ON [PRIMARY];
GO

-- HoaDon: Sales invoices. Created at site; Central aggregates via replication.
CREATE TABLE [dbo].[HoaDon] (
    [MaHoaDon] [varchar](20) NOT NULL,
    [MaChiNhanh] [varchar](10) NOT NULL,
    [MaKhachHang] [varchar](10) NULL,
    [MaNhanVien] [varchar](10) NOT NULL,
    [NgayLap] [datetime] NOT NULL DEFAULT GETDATE(),
    [TongTien] [decimal](18, 2) NOT NULL DEFAULT 0,
    [PhuongThucTT] [varchar](30) NULL,
    [TrangThai] [varchar](20) NOT NULL DEFAULT N'DA_THANH_TOAN',
    [LastModified] [datetime2](3) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT [PK_HoaDon] PRIMARY KEY CLUSTERED ([MaHoaDon] ASC)
) ON [PRIMARY];
GO

-- ChiTietHoaDon: Invoice lines.
CREATE TABLE [dbo].[ChiTietHoaDon] (
    [MaHoaDon] [varchar](20) NOT NULL,
    [MaSanPham] [varchar](10) NOT NULL,
    [SoLuong] [int] NOT NULL,
    [DonGia] [decimal](18, 2) NOT NULL,
    [GiamGia] [decimal](18, 2) NOT NULL DEFAULT 0,
    [ThanhTien] [decimal](18, 2) NOT NULL,
    CONSTRAINT [PK_ChiTietHoaDon] PRIMARY KEY CLUSTERED ([MaHoaDon] ASC, [MaSanPham] ASC)
) ON [PRIMARY];
GO

-- TonKho: Inventory per branch. Each site manages its own TonKho (local writes).
-- Central aggregates via view / replication.
CREATE TABLE [dbo].[TonKho] (
    [MaChiNhanh] [varchar](10) NOT NULL,
    [MaSanPham] [varchar](10) NOT NULL,
    [SoLuongTon] [int] NOT NULL DEFAULT 0 CHECK ([SoLuongTon] >= 0),
    [MucTonToiThieu] [int] NOT NULL DEFAULT 5,
    [NgayCapNhat] [datetime] NOT NULL DEFAULT GETDATE(),
    [LastModified] [datetime2](3) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT [PK_TonKho] PRIMARY KEY CLUSTERED ([MaChiNhanh] ASC, [MaSanPham] ASC)
) ON [PRIMARY];
GO

-- -------------------------------------------------
-- 2. FOREIGN KEYS (identical on all 4 instances)
-- -------------------------------------------------
ALTER TABLE [dbo].[ChiTietHoaDon] WITH CHECK ADD CONSTRAINT [FK_ChiTietHoaDon_HoaDon]
    FOREIGN KEY ([MaHoaDon]) REFERENCES [dbo].[HoaDon] ([MaHoaDon]);
GO
ALTER TABLE [dbo].[ChiTietHoaDon] WITH CHECK ADD CONSTRAINT [FK_ChiTietHoaDon_SanPham]
    FOREIGN KEY ([MaSanPham]) REFERENCES [dbo].[SanPham] ([MaSanPham]);
GO
ALTER TABLE [dbo].[HoaDon] WITH CHECK ADD CONSTRAINT [FK_HoaDon_ChiNhanh]
    FOREIGN KEY ([MaChiNhanh]) REFERENCES [dbo].[ChiNhanh] ([MaChiNhanh]);
GO
ALTER TABLE [dbo].[HoaDon] WITH CHECK ADD CONSTRAINT [FK_HoaDon_KhachHang]
    FOREIGN KEY ([MaKhachHang]) REFERENCES [dbo].[KhachHang] ([MaKhachHang]);
GO
ALTER TABLE [dbo].[HoaDon] WITH CHECK ADD CONSTRAINT [FK_HoaDon_NhanVien]
    FOREIGN KEY ([MaNhanVien]) REFERENCES [dbo].[NhanVien] ([MaNhanVien]);
GO
ALTER TABLE [dbo].[NhanVien] WITH CHECK ADD CONSTRAINT [FK_NhanVien_ChiNhanh]
    FOREIGN KEY ([MaChiNhanh]) REFERENCES [dbo].[ChiNhanh] ([MaChiNhanh]);
GO
ALTER TABLE [dbo].[TaiKhoan] WITH CHECK ADD CONSTRAINT [FK_TaiKhoan_NhanVien]
    FOREIGN KEY ([MaNhanVien]) REFERENCES [dbo].[NhanVien] ([MaNhanVien]);
GO
ALTER TABLE [dbo].[TonKho] WITH CHECK ADD CONSTRAINT [FK_TonKho_ChiNhanh]
    FOREIGN KEY ([MaChiNhanh]) REFERENCES [dbo].[ChiNhanh] ([MaChiNhanh]);
GO
ALTER TABLE [dbo].[TonKho] WITH CHECK ADD CONSTRAINT [FK_TonKho_SanPham]
    FOREIGN KEY ([MaSanPham]) REFERENCES [dbo].[SanPham] ([MaSanPham]);
GO

-- -------------------------------------------------
-- 3. INDEXES for replication performance
-- -------------------------------------------------
CREATE NONCLUSTERED INDEX [IX_HoaDon_MaChiNhanh_NgayLap] ON [dbo].[HoaDon] ([MaChiNhanh] ASC, [NgayLap] DESC);
GO
CREATE NONCLUSTERED INDEX [IX_HoaDon_MaNhanVien] ON [dbo].[HoaDon] ([MaNhanVien] ASC);
GO
CREATE NONCLUSTERED INDEX [IX_NhanVien_MaChiNhanh] ON [dbo].[NhanVien] ([MaChiNhanh] ASC);
GO

-- -------------------------------------------------
-- 4. STORED PROCEDURE TEMPLATES
--    Central version uses linked servers (4-part names).
--    Site version is a no-op or local-only (defined in site-specific script).
-- -------------------------------------------------

-- sp_DongBoSanPham: Central pushes product changes to all 3 sites via linked servers.
-- Template — replace @SiteDB with linked server name at Central.
-- On sites, this proc does nothing (products are read-only).
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoSanPham]
AS
BEGIN
    SET NOCOUNT ON;
    -- Implemented in Fashion_Central.sql using 4-part linked server names:
    -- SITE_Q1.Fashion_Store.dbo.SanPham, SITE_Q7.Fashion_Store.dbo.SanPham, SITE_THUDUC.Fashion_Store.dbo.SanPham
    -- Sites: no-op (SanPham is read-only replica).
    PRINT N'sp_DongBoSanPham: no-op on this instance. Central version uses linked servers.';
END;
GO

-- sp_DongBoKhachHang: Two-way sync for KhachHang (PDF §3.6).
-- Central merges changes from all sites; sites pull from Central.
-- Template — actual logic in site-specific scripts.
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoKhachHang]
AS
BEGIN
    SET NOCOUNT ON;
    PRINT N'sp_DongBoKhachHang: template. Implement per instance (Central merges, Sites pull).';
END;
GO

-- sp_DongBoNhanVien_TaiKhoan: Replicate NhanVien + TaiKhoan to sites (read-only, filtered by MaChiNhanh).
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoNhanVien_TaiKhoan]
    @MaChiNhanhSite varchar(10)  -- CN1, CN2, or CN3
AS
BEGIN
    SET NOCOUNT ON;
    PRINT N'sp_DongBoNhanVien_TaiKhoan: template. Central pushes filtered rows to each site.';
END;
GO

-- sp_DongBoHoaDon: Site pushes new invoices to Central; Central aggregates.
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoHoaDon]
AS
BEGIN
    SET NOCOUNT ON;
    PRINT N'sp_DongBoHoaDon: template. Implement per instance.';
END;
GO

-- -------------------------------------------------
-- 5. TRIGGER TEMPLATES
--    Site triggers: validate MaChiNhanh matches site's branch.
--    Central triggers: guard against direct deletes on replicated tables.
-- -------------------------------------------------

-- Site trigger: prevent inserts/updates on HoaDon with wrong MaChiNhanh.
-- Create this on each site (Fashion_Store) with @SiteBranch = 'CN1' | 'CN2' | 'CN3'.
-- Template:
/*
CREATE TRIGGER [dbo].[trg_HoaDon_ValidateBranch]
ON [dbo].[HoaDon]
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM inserted i
        WHERE i.MaChiNhanh <> N'<SITE_BRANCH_CODE>' -- e.g., CN1
    )
    BEGIN
        RAISERROR (N'Chỉ được tạo/sửa hóa đơn của chi nhánh hiện tại (<SITE_BRANCH_CODE>).', 16, 1);
        ROLLBACK TRANSACTION;
    END
END;
GO
*/

-- Central trigger: prevent DELETE on replicated master tables (SanPham, KhachHang, NhanVien, TaiKhoan).
-- Template (create on Fashion_Central):
/*
CREATE TRIGGER [dbo].[trg_Central_GuardReplicatedDelete]
ON [dbo].[SanPham] -- repeat for KhachHang, NhanVien, TaiKhoan
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;
    RAISERROR (N'Không được xóa trực tiếp trên Central. Dữ liệu này được đồng bộ từ các site.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO
*/

-- -------------------------------------------------
-- 6. VIEW TEMPLATE: Central aggregated inventory
-- -------------------------------------------------
/*
CREATE VIEW [dbo].[v_TonKho_ToanBo]
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
*/

PRINT N'common.sql executed: 8 tables, FKs, indexes, SP/Trigger templates created.';
GO