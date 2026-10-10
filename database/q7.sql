-- May 3: Store B - Quan 7 / CN02.
-- Chay trong SSMS sau khi restore database hien co.
-- Script chi doc; khong tao database, bang hoac Linked Server.
USE [Fashion_Q7];
GO

SELECT DB_NAME() AS DatabaseName, @@SERVERNAME AS ServerName, N'CN02' AS MaChiNhanh;

SELECT name, provider, data_source, is_data_access_enabled
FROM sys.servers
WHERE name = N'CENTRAL';
GO

-- Chay sau khi doi chieu schema va cau hinh ket noi:
-- EXEC sys.sp_testlinkedserver N'CENTRAL';
-- SELECT * FROM dbo.TonKho WHERE MaChiNhanh = N'CN02';
-- SELECT * FROM dbo.HoaDon WHERE MaChiNhanh = N'CN02';
-- Kiem tra phan manh: hai truy van sau phai tra ve 0 dong.
-- SELECT * FROM dbo.TonKho WHERE MaChiNhanh <> N'CN02' OR MaChiNhanh IS NULL;
-- SELECT * FROM dbo.HoaDon WHERE MaChiNhanh <> N'CN02' OR MaChiNhanh IS NULL;

-- TODO: Doi chieu rang buoc phan manh CN02 tren database hien co.
-- TODO: Cau hinh replication KhachHang qua Central.
