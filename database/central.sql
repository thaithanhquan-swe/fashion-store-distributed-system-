-- May 1: Central Server / Fashion_Central.
-- Chay trong SSMS sau khi restore database hien co.
-- Script chi doc; khong tao database, bang hoac Linked Server.
USE [Fashion_Central];
GO

SELECT DB_NAME() AS DatabaseName, @@SERVERNAME AS ServerName;

-- Kiem tra cac ket noi can cau hinh theo README muc 24-27.
SELECT name, provider, data_source, is_data_access_enabled
FROM sys.servers
WHERE name IN (N'SITE_Q1', N'SITE_Q7', N'SITE_THUDUC');
GO

-- Chi chay truy van tong hop khi ca ba Linked Server da san sang.
-- EXEC sys.sp_testlinkedserver N'SITE_Q1';
-- EXEC sys.sp_testlinkedserver N'SITE_Q7';
-- EXEC sys.sp_testlinkedserver N'SITE_THUDUC';
/*
SELECT N'Q1' AS Site, MaChiNhanh, MaSanPham, SoLuongTon
FROM [SITE_Q1].[Fashion_Q1].[dbo].[TonKho]
UNION ALL
SELECT N'Q7', MaChiNhanh, MaSanPham, SoLuongTon
FROM [SITE_Q7].[Fashion_Q7].[dbo].[TonKho]
UNION ALL
SELECT N'THUDUC', MaChiNhanh, MaSanPham, SoLuongTon
FROM [SITE_THUDUC].[Fashion_ThuDuc].[dbo].[TonKho];
*/

-- TODO: Them bao cao HoaDon/ChiTietHoaDon sau khi doi chieu schema.
-- TODO: Cau hinh replication KhachHang voi cac site (xem docs/architecture.md).
