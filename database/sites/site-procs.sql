-- =====================================================================
-- SITE DISTRIBUTED PROCEDURES — run on EACH site AFTER:
--   1) linked-servers.sql (role = site)   -> linked server CENTRAL
--   2) sites/Fashion_Store.sql            -> database Fashion_Store
--   3) Central has created Fashion_Central and its tables
-- Run from the `database/` folder:
--     sqlcmd -S <instance> -E -i sites\site-procs.sql -b -f 65001
-- =====================================================================

USE [Fashion_Store];
GO

-- -------------------------------------------------
-- CONFIG — must match the branch of THIS site
-- -------------------------------------------------
:setvar SiteBranch "CN1"

-- QUẬN 7 (CN2)   : :setvar SiteBranch "CN2"
-- THỦ ĐỨC (CN3)  : :setvar SiteBranch "CN3"

-- sp_DongBoKhachHang: Pull latest from Central (two-way sync per PDF §3.6)
-- Uses linked server to Central: CENTRAL.Fashion_Central.dbo.KhachHang
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoKhachHang]
AS
BEGIN
    SET NOCOUNT ON;
    -- MERGE from Central to Site (Conflict Resolution: Last-In-Wins via LastModified)
    MERGE INTO [dbo].[KhachHang] AS target
    USING (
        SELECT MaKhachHang, HoTen, SoDienThoai, Email, DiaChi, DiemTichLuy, NgayDangKy, TrangThai, LastModified
        FROM [CENTRAL].[Fashion_Central].[dbo].[KhachHang]
    ) AS source
    ON target.MaKhachHang = source.MaKhachHang
    WHEN MATCHED AND source.LastModified > target.LastModified THEN
        UPDATE SET
            HoTen = source.HoTen,
            SoDienThoai = source.SoDienThoai,
            Email = source.Email,
            DiaChi = source.DiaChi,
            DiemTichLuy = source.DiemTichLuy,
            NgayDangKy = source.NgayDangKy,
            TrangThai = source.TrangThai,
            LastModified = source.LastModified
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (MaKhachHang, HoTen, SoDienThoai, Email, DiaChi, DiemTichLuy, NgayDangKy, TrangThai, LastModified)
        VALUES (source.MaKhachHang, source.HoTen, source.SoDienThoai, source.Email, source.DiaChi, source.DiemTichLuy, source.NgayDangKy, source.TrangThai, source.LastModified);
END;
GO

-- sp_DongBoNhanVien_TaiKhoan: Pull filtered employees/accounts for this branch from Central
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoNhanVien_TaiKhoan]
AS
BEGIN
    SET NOCOUNT ON;
    -- Sync NhanVien for this branch only
    MERGE INTO [dbo].[NhanVien] AS target
    USING (
        SELECT MaNhanVien, MaChiNhanh, HoTen, NgaySinh, GioiTinh, SoDienThoai, Email, ChucVu, TrangThai, LastModified
        FROM [CENTRAL].[Fashion_Central].[dbo].[NhanVien]
        WHERE MaChiNhanh = N'$(SiteBranch)'
    ) AS source
    ON target.MaNhanVien = source.MaNhanVien
    WHEN MATCHED AND source.LastModified > target.LastModified THEN
        UPDATE SET
            MaChiNhanh = source.MaChiNhanh,
            HoTen = source.HoTen,
            NgaySinh = source.NgaySinh,
            GioiTinh = source.GioiTinh,
            SoDienThoai = source.SoDienThoai,
            Email = source.Email,
            ChucVu = source.ChucVu,
            TrangThai = source.TrangThai,
            LastModified = source.LastModified
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (MaNhanVien, MaChiNhanh, HoTen, NgaySinh, GioiTinh, SoDienThoai, Email, ChucVu, TrangThai, LastModified)
        VALUES (source.MaNhanVien, source.MaChiNhanh, source.HoTen, source.NgaySinh, source.GioiTinh, source.SoDienThoai, source.Email, source.ChucVu, source.TrangThai, source.LastModified);

    -- Sync TaiKhoan for employees of this branch
    MERGE INTO [dbo].[TaiKhoan] AS target
    USING (
        SELECT tk.MaTaiKhoan, tk.MaNhanVien, tk.TenDangNhap, tk.MatKhau, tk.VaiTro, tk.TrangThai
        FROM [CENTRAL].[Fashion_Central].[dbo].[TaiKhoan] tk
        JOIN [CENTRAL].[Fashion_Central].[dbo].[NhanVien] nv ON tk.MaNhanVien = nv.MaNhanVien
        WHERE nv.MaChiNhanh = N'$(SiteBranch)'
    ) AS source
    ON target.MaTaiKhoan = source.MaTaiKhoan
    WHEN MATCHED THEN
        UPDATE SET
            MaNhanVien = source.MaNhanVien,
            TenDangNhap = source.TenDangNhap,
            MatKhau = source.MatKhau,
            VaiTro = source.VaiTro,
            TrangThai = source.TrangThai
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (MaNhanVien, TenDangNhap, MatKhau, VaiTro, TrangThai)
        VALUES (source.MaNhanVien, source.TenDangNhap, source.MatKhau, source.VaiTro, source.TrangThai);  -- Deletes are not propagated (INSTEAD OF DELETE guard trigger).
END;
GO

-- sp_DongBoHoaDon: Push new invoices from this site to Central
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoHoaDon]
AS
BEGIN
    SET NOCOUNT ON;
    -- Insert new invoices to Central (idempotent via PK)
    INSERT INTO [CENTRAL].[Fashion_Central].[dbo].[HoaDon] (
        MaHoaDon, MaChiNhanh, MaKhachHang, MaNhanVien, NgayLap, TongTien, PhuongThucTT, TrangThai
    )
    SELECT MaHoaDon, MaChiNhanh, MaKhachHang, MaNhanVien, NgayLap, TongTien, PhuongThucTT, TrangThai
    FROM [dbo].[HoaDon] h
    WHERE NOT EXISTS (
        SELECT 1 FROM [CENTRAL].[Fashion_Central].[dbo].[HoaDon] c WHERE c.MaHoaDon = h.MaHoaDon
    );

    -- Insert new invoice lines
    INSERT INTO [CENTRAL].[Fashion_Central].[dbo].[ChiTietHoaDon] (
        MaHoaDon, MaSanPham, SoLuong, DonGia, GiamGia, ThanhTien
    )
    SELECT ct.MaHoaDon, ct.MaSanPham, ct.SoLuong, ct.DonGia, ct.GiamGia, ct.ThanhTien
    FROM [dbo].[ChiTietHoaDon] ct
    WHERE NOT EXISTS (
        SELECT 1 FROM [CENTRAL].[Fashion_Central].[dbo].[ChiTietHoaDon] c
        WHERE c.MaHoaDon = ct.MaHoaDon AND c.MaSanPham = ct.MaSanPham
    );
END;
GO

GO

PRINT N'Site distributed procedures created for branch $(SiteBranch).';
GO
