-- May 2: Store A - Quan 1 / CN01.
-- Chay trong SSMS sau khi restore database hien co.
-- Script chi doc; khong tao database, bang hoac Linked Server.
USE [Fashion_Q1];
GO

SELECT DB_NAME() AS DatabaseName, @@SERVERNAME AS ServerName, N'CN01' AS MaChiNhanh;

SELECT name, provider, data_source, is_data_access_enabled
FROM sys.servers
WHERE name = N'CENTRAL';
GO

-- Chay sau khi doi chieu schema va cau hinh ket noi:
-- EXEC sys.sp_testlinkedserver N'CENTRAL';
-- SELECT * FROM dbo.TonKho WHERE MaChiNhanh = N'CN01';
-- SELECT * FROM dbo.HoaDon WHERE MaChiNhanh = N'CN01';
-- Kiem tra phan manh: hai truy van sau phai tra ve 0 dong.
-- SELECT * FROM dbo.TonKho WHERE MaChiNhanh <> N'CN01' OR MaChiNhanh IS NULL;
-- SELECT * FROM dbo.HoaDon WHERE MaChiNhanh <> N'CN01' OR MaChiNhanh IS NULL;

-- TODO: Doi chieu rang buoc phan manh CN01 tren database hien co.
-- TODO: Cau hinh replication KhachHang qua Central.
