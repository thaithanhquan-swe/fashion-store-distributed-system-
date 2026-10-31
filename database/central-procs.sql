-- =====================================================================
-- CENTRAL DISTRIBUTED PROCEDURES — run on the Central machine AFTER:
--   1) linked-servers.sql (role = central)  -> SITE_Q1, SITE_Q7, SITE_THUDUC
--   2) Fashion_Central.sql                  -> database Fashion_Central
--   3) sites/Fashion_Store.sql on each of the 3 sites (Fashion_Store exists)
-- They use 4-part names, so SQL Server validates the remote databases at
-- creation time — hence this script is separate from Fashion_Central.sql.
-- Run from the `database/` folder:
--     sqlcmd -S <instance> -E -i central-procs.sql -b -f 65001
-- =====================================================================

USE [Fashion_Central];
GO

-- -------------------------------------------------
-- 7. CENTRAL STORED PROCEDURES (use 4-part linked server names)
-- -------------------------------------------------

-- sp_DongBoSanPham: Push product changes from Central to all 3 sites
-- Uses 4-part names: SITE_Q1.Fashion_Store.dbo.SanPham, etc.
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoSanPham]
AS
BEGIN
    SET NOCOUNT ON;

    -- Push to Quận 1 (SITE_Q1)
    UPDATE s
    SET
        s.TenSanPham = c.TenSanPham,
        s.LoaiSanPham = c.LoaiSanPham,
        s.ThuongHieu = c.ThuongHieu,
        s.MauSac = c.MauSac,
        s.KichThuoc = c.KichThuoc,
        s.GiaBan = c.GiaBan,
        s.TrangThai = c.TrangThai,
        s.LastModified = c.LastModified
    FROM [SITE_Q1].[Fashion_Store].[dbo].[SanPham] s
    JOIN [dbo].[SanPham] c ON s.MaSanPham = c.MaSanPham;

    -- Insert new products to Q1
    INSERT INTO [SITE_Q1].[Fashion_Store].[dbo].[SanPham] (
        MaSanPham, TenSanPham, LoaiSanPham, ThuongHieu, MauSac, KichThuoc, GiaBan, TrangThai, LastModified
    )
    SELECT c.MaSanPham, c.TenSanPham, c.LoaiSanPham, c.ThuongHieu, c.MauSac, c.KichThuoc, c.GiaBan, c.TrangThai, c.LastModified
    FROM [dbo].[SanPham] c
    WHERE NOT EXISTS (
        SELECT 1 FROM [SITE_Q1].[Fashion_Store].[dbo].[SanPham] s WHERE s.MaSanPham = c.MaSanPham
    );

    -- Push to Quận 7 (SITE_Q7)
    UPDATE s
    SET
        s.TenSanPham = c.TenSanPham,
        s.LoaiSanPham = c.LoaiSanPham,
        s.ThuongHieu = c.ThuongHieu,
        s.MauSac = c.MauSac,
        s.KichThuoc = c.KichThuoc,
        s.GiaBan = c.GiaBan,
        s.TrangThai = c.TrangThai,
        s.LastModified = c.LastModified
    FROM [SITE_Q7].[Fashion_Store].[dbo].[SanPham] s
    JOIN [dbo].[SanPham] c ON s.MaSanPham = c.MaSanPham;

    INSERT INTO [SITE_Q7].[Fashion_Store].[dbo].[SanPham] (
        MaSanPham, TenSanPham, LoaiSanPham, ThuongHieu, MauSac, KichThuoc, GiaBan, TrangThai, LastModified
    )
    SELECT c.MaSanPham, c.TenSanPham, c.LoaiSanPham, c.ThuongHieu, c.MauSac, c.KichThuoc, c.GiaBan, c.TrangThai, c.LastModified
    FROM [dbo].[SanPham] c
    WHERE NOT EXISTS (
        SELECT 1 FROM [SITE_Q7].[Fashion_Store].[dbo].[SanPham] s WHERE s.MaSanPham = c.MaSanPham
    );

    -- Push to Thủ Đức (SITE_THUDUC)
    UPDATE s
    SET
        s.TenSanPham = c.TenSanPham,
        s.LoaiSanPham = c.LoaiSanPham,
        s.ThuongHieu = c.ThuongHieu,
        s.MauSac = c.MauSac,
        s.KichThuoc = c.KichThuoc,
        s.GiaBan = c.GiaBan,
        s.TrangThai = c.TrangThai,
        s.LastModified = c.LastModified
    FROM [SITE_THUDUC].[Fashion_Store].[dbo].[SanPham] s
    JOIN [dbo].[SanPham] c ON s.MaSanPham = c.MaSanPham;

    INSERT INTO [SITE_THUDUC].[Fashion_Store].[dbo].[SanPham] (
        MaSanPham, TenSanPham, LoaiSanPham, ThuongHieu, MauSac, KichThuoc, GiaBan, TrangThai, LastModified
    )
    SELECT c.MaSanPham, c.TenSanPham, c.LoaiSanPham, c.ThuongHieu, c.MauSac, c.KichThuoc, c.GiaBan, c.TrangThai, c.LastModified
    FROM [dbo].[SanPham] c
    WHERE NOT EXISTS (
        SELECT 1 FROM [SITE_THUDUC].[Fashion_Store].[dbo].[SanPham] s WHERE s.MaSanPham = c.MaSanPham
    );

    PRINT N'sp_DongBoSanPham: Pushed product changes to SITE_Q1, SITE_Q7, SITE_THUDUC.';
END;
GO

-- sp_DongBoKhachHang: Central merges two-way changes from all sites (Conflict Resolution: Last-In-Wins via LastModified)
-- Called by replication or scheduled job
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoKhachHang]
AS
BEGIN
    SET NOCOUNT ON;

    -- Merge from Q1 (Last-In-Wins rule based on LastModified)
    MERGE INTO [dbo].[KhachHang] AS target
    USING (
        SELECT MaKhachHang, HoTen, SoDienThoai, Email, DiaChi, DiemTichLuy, NgayDangKy, TrangThai, LastModified
        FROM [SITE_Q1].[Fashion_Store].[dbo].[KhachHang]
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

    -- Merge from Q7 (Last-In-Wins rule based on LastModified)
    MERGE INTO [dbo].[KhachHang] AS target
    USING (
        SELECT MaKhachHang, HoTen, SoDienThoai, Email, DiaChi, DiemTichLuy, NgayDangKy, TrangThai, LastModified
        FROM [SITE_Q7].[Fashion_Store].[dbo].[KhachHang]
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

    -- Merge from Thủ Đức (Last-In-Wins rule based on LastModified)
    MERGE INTO [dbo].[KhachHang] AS target
    USING (
        SELECT MaKhachHang, HoTen, SoDienThoai, Email, DiaChi, DiemTichLuy, NgayDangKy, TrangThai, LastModified
        FROM [SITE_THUDUC].[Fashion_Store].[dbo].[KhachHang]
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

    PRINT N'sp_DongBoKhachHang: Merged customer changes from all 3 sites using Last-In-Wins rule.';
END;
GO

-- sp_DongBoNhanVien_TaiKhoan: Push filtered employees/accounts to each site
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoNhanVien_TaiKhoan]
    @MaChiNhanhSite varchar(10),  -- CN1, CN2, or CN3
    @LinkedServerName sysname     -- SITE_Q1, SITE_Q7, or SITE_THUDUC
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @sql nvarchar(max);

    -- Dynamic SQL because linked server name is a parameter
    SET @sql = N'
    MERGE INTO [' + @LinkedServerName + N'].[Fashion_Store].[dbo].[NhanVien] AS target
    USING (
        SELECT MaNhanVien, MaChiNhanh, HoTen, NgaySinh, GioiTinh, SoDienThoai, Email, ChucVu, TrangThai
        FROM [dbo].[NhanVien]
        WHERE MaChiNhanh = @MaChiNhanhSite
    ) AS source
    ON target.MaNhanVien = source.MaNhanVien
    WHEN MATCHED THEN
        UPDATE SET
            MaChiNhanh = source.MaChiNhanh,
            HoTen = source.HoTen,
            NgaySinh = source.NgaySinh,
            GioiTinh = source.GioiTinh,
            SoDienThoai = source.SoDienThoai,
            Email = source.Email,
            ChucVu = source.ChucVu,
            TrangThai = source.TrangThai
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (MaNhanVien, MaChiNhanh, HoTen, NgaySinh, GioiTinh, SoDienThoai, Email, ChucVu, TrangThai)
        VALUES (source.MaNhanVien, source.MaChiNhanh, source.HoTen, source.NgaySinh, source.GioiTinh, source.SoDienThoai, source.Email, source.ChucVu, source.TrangThai);';  -- Deletes are not propagated (INSTEAD OF DELETE guard trigger).

    EXEC sp_executesql @sql, N'@MaChiNhanhSite varchar(10)', @MaChiNhanhSite = @MaChiNhanhSite;

    -- Sync TaiKhoan for this branch
    SET @sql = N'
    MERGE INTO [' + @LinkedServerName + N'].[Fashion_Store].[dbo].[TaiKhoan] AS target
    USING (
        SELECT tk.MaTaiKhoan, tk.MaNhanVien, tk.TenDangNhap, tk.MatKhau, tk.VaiTro, tk.TrangThai
        FROM [dbo].[TaiKhoan] tk
        JOIN [dbo].[NhanVien] nv ON tk.MaNhanVien = nv.MaNhanVien
        WHERE nv.MaChiNhanh = @MaChiNhanhSite
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
        VALUES (source.MaNhanVien, source.TenDangNhap, source.MatKhau, source.VaiTro, source.TrangThai);';  -- Deletes are not propagated (INSTEAD OF DELETE guard trigger).

    EXEC sp_executesql @sql, N'@MaChiNhanhSite varchar(10)', @MaChiNhanhSite = @MaChiNhanhSite;

    PRINT N'sp_DongBoNhanVien_TaiKhoan: Pushed to ' + @LinkedServerName + N' for branch ' + @MaChiNhanhSite;
END;
GO

-- sp_DongBoHoaDon: Aggregate invoices from all sites to Central
-- Called by scheduled job or replication
CREATE OR ALTER PROCEDURE [dbo].[sp_DongBoHoaDon]
AS
BEGIN
    SET NOCOUNT ON;

    -- Pull from Q1
    INSERT INTO [dbo].[HoaDon] (MaHoaDon, MaChiNhanh, MaKhachHang, MaNhanVien, NgayLap, TongTien, PhuongThucTT, TrangThai)
    SELECT MaHoaDon, MaChiNhanh, MaKhachHang, MaNhanVien, NgayLap, TongTien, PhuongThucTT, TrangThai
    FROM [SITE_Q1].[Fashion_Store].[dbo].[HoaDon] s
    WHERE NOT EXISTS (SELECT 1 FROM [dbo].[HoaDon] c WHERE c.MaHoaDon = s.MaHoaDon);

    INSERT INTO [dbo].[ChiTietHoaDon] (MaHoaDon, MaSanPham, SoLuong, DonGia, GiamGia, ThanhTien)
    SELECT ct.MaHoaDon, ct.MaSanPham, ct.SoLuong, ct.DonGia, ct.GiamGia, ct.ThanhTien
    FROM [SITE_Q1].[Fashion_Store].[dbo].[ChiTietHoaDon] ct
    WHERE NOT EXISTS (SELECT 1 FROM [dbo].[ChiTietHoaDon] c WHERE c.MaHoaDon = ct.MaHoaDon AND c.MaSanPham = ct.MaSanPham);

    -- Pull from Q7
    INSERT INTO [dbo].[HoaDon] (MaHoaDon, MaChiNhanh, MaKhachHang, MaNhanVien, NgayLap, TongTien, PhuongThucTT, TrangThai)
    SELECT MaHoaDon, MaChiNhanh, MaKhachHang, MaNhanVien, NgayLap, TongTien, PhuongThucTT, TrangThai
    FROM [SITE_Q7].[Fashion_Store].[dbo].[HoaDon] s
    WHERE NOT EXISTS (SELECT 1 FROM [dbo].[HoaDon] c WHERE c.MaHoaDon = s.MaHoaDon);

    INSERT INTO [dbo].[ChiTietHoaDon] (MaHoaDon, MaSanPham, SoLuong, DonGia, GiamGia, ThanhTien)
    SELECT ct.MaHoaDon, ct.MaSanPham, ct.SoLuong, ct.DonGia, ct.GiamGia, ct.ThanhTien
    FROM [SITE_Q7].[Fashion_Store].[dbo].[ChiTietHoaDon] ct
    WHERE NOT EXISTS (SELECT 1 FROM [dbo].[ChiTietHoaDon] c WHERE c.MaHoaDon = ct.MaHoaDon AND c.MaSanPham = ct.MaSanPham);

    -- Pull from Thủ Đức
    INSERT INTO [dbo].[HoaDon] (MaHoaDon, MaChiNhanh, MaKhachHang, MaNhanVien, NgayLap, TongTien, PhuongThucTT, TrangThai)
    SELECT MaHoaDon, MaChiNhanh, MaKhachHang, MaNhanVien, NgayLap, TongTien, PhuongThucTT, TrangThai
    FROM [SITE_THUDUC].[Fashion_Store].[dbo].[HoaDon] s
    WHERE NOT EXISTS (SELECT 1 FROM [dbo].[HoaDon] c WHERE c.MaHoaDon = s.MaHoaDon);

    INSERT INTO [dbo].[ChiTietHoaDon] (MaHoaDon, MaSanPham, SoLuong, DonGia, GiamGia, ThanhTien)
    SELECT ct.MaHoaDon, ct.MaSanPham, ct.SoLuong, ct.DonGia, ct.GiamGia, ct.ThanhTien
    FROM [SITE_THUDUC].[Fashion_Store].[dbo].[ChiTietHoaDon] ct
    WHERE NOT EXISTS (SELECT 1 FROM [dbo].[ChiTietHoaDon] c WHERE c.MaHoaDon = ct.MaHoaDon AND c.MaSanPham = ct.MaSanPham);

    PRINT N'sp_DongBoHoaDon: Aggregated invoices from all 3 sites.';
END;
GO

GO

PRINT N'Central distributed procedures created.';
GO
