-- Phan dung chung: chon database dich trong SSMS truoc khi chay.
-- Hien tai chi kiem tra schema. Chua cai dat stored procedure ban hang.
-- Khong suy doan kieu du lieu, khoa hay rang buoc cua database hien co.
IF DB_NAME() NOT IN (N'Fashion_Central', N'Fashion_Q1', N'Fashion_Q7', N'Fashion_ThuDuc')
BEGIN
    ;THROW 50001, N'Hay chon mot database Fashion truoc khi chay common.sql.', 1;
END;

SELECT
    DB_NAME() AS DatabaseName,
    s.name AS SchemaName,
    t.name AS TableName,
    c.column_id AS ColumnOrder,
    c.name AS ColumnName,
    ty.name AS DataType,
    c.max_length AS MaxLengthBytes,
    c.precision AS NumericPrecision,
    c.scale AS NumericScale,
    c.is_nullable AS IsNullable
FROM sys.tables AS t
JOIN sys.schemas AS s ON s.schema_id = t.schema_id
JOIN sys.columns AS c ON c.object_id = t.object_id
JOIN sys.types AS ty ON ty.user_type_id = c.user_type_id
WHERE s.name = N'dbo'
  AND t.name IN (N'SanPham', N'ChiNhanh', N'TonKho', N'KhachHang', N'HoaDon', N'ChiTietHoaDon')
ORDER BY t.name, c.column_id;
GO

-- TODO sau khi co schema:
-- 1. Stored procedure ban hang tai site: transaction cuc bo, kiem tra ton,
--    tao HoaDon + ChiTietHoaDon, tru TonKho; rollback neu bat ky buoc nao loi.
-- 2. Bao ve ban hang dong thoi de khong am ton; lay gia tu database.
-- 3. Truy van bao cao cuc bo; tong hop lien site dat trong central.sql.
-- 4. Dong bo KhachHang theo co che da chon; Linked Server khong tu replication.
