-- =====================================================================
-- SEED DATA FOR FASHION STORE DISTRIBUTED SYSTEM
-- Run on Central (Fashion_Central) AFTER common.sql and Fashion_Central.sql
-- Site data will be replicated via publications.
-- IDs use site prefix: Q1-*, Q7-*, TD-* to avoid PK conflicts across sites.
-- =====================================================================

USE [Fashion_Central];
GO

SET NOCOUNT ON;
GO

-- -------------------------------------------------
-- 1. SAN PHAM (Product Master) — Central read-write, replicated to all sites
-- 100 products across diverse fashion categories
-- -------------------------------------------------
INSERT INTO [dbo].[SanPham] ([MaSanPham], [TenSanPham], [LoaiSanPham], [ThuongHieu], [MauSac], [KichThuoc], [GiaBan], [TrangThai]) VALUES
('SP001', N'Áo thun nam basic',                      N'Áo thun',          N'FashionStore',     N'Trắng',              N'M',             199000, 1),
('SP002', N'Áo thun nam basic',                      N'Áo thun',          N'FashionStore',     N'Đen',                N'L',             199000, 1),
('SP003', N'Áo khoác bomber',                        N'Áo khoác',         N'FashionStore',     N'Xanh navy',          N'M',             499000, 1),
('SP004', N'Quần jean slimfit',                      N'Quần',             N'FashionStore',     N'Xanh đen',           N'32',            399000, 1),
('SP005', N'Quần jogger nam',                        N'Quần',             N'FashionStore',     N'Xám',                N'L',             349000, 1),
('SP006', N'Áo sơ mi trắng',                         N'Áo sơ mi',         N'FashionStore',     N'Trắng',              N'S',             299000, 1),
('SP007', N'Áo sơ mi kẻ sọc',                        N'Áo sơ mi',         N'FashionStore',     N'Xanh trắng',         N'M',             319000, 1),
('SP008', N'Váy maxi hoa',                           N'Váy',              N'FashionStore',     N'Hồng',               N'M',             449000, 1),
('SP009', N'Chân váy bút chì',                       N'Chân váy',         N'FashionStore',     N'Đen',                N'S',             279000, 1),
('SP010', N'Đầm công sở',                            N'Đầm',              N'FashionStore',     N'Be',                 N'M',             529000, 1),
('SP011', N'Áo hoodie unisex',                       N'Áo hoodie',        N'FashionStore',     N'Trắng',              N'L',             389000, 1),
('SP012', N'Áo hoodie unisex',                       N'Áo hoodie',        N'FashionStore',     N'Đen',                N'XL',            389000, 1),
('SP013', N'Quần short kaki',                        N'Quần short',       N'FashionStore',     N'Be',                 N'30',            259000, 1),
('SP014', N'Áo len cổ lọ',                           N'Áo len',           N'FashionStore',     N'Kem',                N'M',             359000, 1),
('SP015', N'Áo vest nam',                            N'Áo vest',          N'FashionStore',     N'Đen',                N'L',             899000, 1),
('SP016', N'Áo polo nam piqué classic',              N'Áo polo',          N'UrbanBasic',       N'Xanh navy',          N'L',             289000, 1),
('SP017', N'Áo polo nam piqué classic',              N'Áo polo',          N'UrbanBasic',       N'Trắng',              N'M',             289000, 1),
('SP018', N'Áo polo nam phối cổ viền',               N'Áo polo',          N'UrbanBasic',       N'Đen',                N'XL',            319000, 1),
('SP019', N'Áo polo nữ ôm dáng thêu logo',           N'Áo polo',          N'Elegance',         N'Hồng phấn',          N'S',             279000, 1),
('SP020', N'Áo thun oversize streetwear',            N'Áo thun',          N'StreetTrend',      N'Xám tiêu',           N'L',             249000, 1),
('SP021', N'Áo thun oversize graphic print',         N'Áo thun',          N'StreetTrend',      N'Đen',                N'XL',            269000, 1),
('SP022', N'Áo thun cotton compact cao cấp',         N'Áo thun',          N'UrbanBasic',       N'Xanh rêu',           N'M',             229000, 1),
('SP023', N'Áo thun raglan thể thao',                N'Áo thun',          N'ActiveSport',      N'Trắng/Đỏ',           N'L',             219000, 1),
('SP024', N'Áo thun ba lỗ tanktop nam',              N'Áo thun',          N'ActiveSport',      N'Đen',                N'L',             159000, 1),
('SP025', N'Áo thun croptop cổ vuông nữ',            N'Áo thun',          N'Elegance',         N'Vàng pastel',        N'S',             189000, 1),
('SP026', N'Áo sơ mi Oxford tay dài nam',            N'Áo sơ mi',         N'UrbanBasic',       N'Xanh nhạt',          N'L',             349000, 1),
('SP027', N'Áo sơ mi Oxford tay dài nam',            N'Áo sơ mi',         N'UrbanBasic',       N'Trắng',              N'M',             349000, 1),
('SP028', N'Áo sơ mi cổ tàu đũi thoáng mát',         N'Áo sơ mi',         N'CasualWear',       N'Be',                 N'M',             329000, 1),
('SP029', N'Áo sơ mi dạ flannel caro',               N'Áo sơ mi',         N'CasualWear',       N'Đỏ caro',            N'L',             379000, 1),
('SP030', N'Áo sơ mi dạ flannel caro',               N'Áo sơ mi',         N'CasualWear',       N'Xanh đen caro',      N'XL',            379000, 1),
('SP031', N'Áo sơ mi denim wash cá tính',            N'Áo sơ mi',         N'DenimCo',          N'Xanh chàm',          N'M',             399000, 1),
('SP032', N'Áo sơ mi lụa tơ tằm cổ nơ nữ',           N'Áo sơ mi',         N'Elegance',         N'Trắng ngà',          N'M',             459000, 1),
('SP033', N'Áo sơ mi tay bồng thanh lịch',           N'Áo sơ mi',         N'Elegance',         N'Xanh pastel',        N'S',             369000, 1),
('SP034', N'Áo sơ mi cộc tay họa tiết tropical',     N'Áo sơ mi',         N'CasualWear',       N'Họa tiết',           N'L',             299000, 1),
('SP035', N'Áo sơ mi kẻ sọc dọc slimfit',            N'Áo sơ mi',         N'UrbanBasic',       N'Xám sọc',            N'M',             329000, 1),
('SP036', N'Áo khoác gió 2 lớp trượt nước',          N'Áo khoác',         N'ActiveSport',      N'Đen',                N'L',             429000, 1),
('SP037', N'Áo khoác gió 2 lớp trượt nước',          N'Áo khoác',         N'ActiveSport',      N'Xanh coban',         N'M',             429000, 1),
('SP038', N'Áo khoác denim nam trucker',             N'Áo khoác',         N'DenimCo',          N'Xanh vintage',       N'L',             559000, 1),
('SP039', N'Áo khoác phao siêu nhẹ lông vũ',         N'Áo khoác',         N'UrbanBasic',       N'Đen',                N'XL',            699000, 1),
('SP040', N'Áo khoác phao siêu nhẹ lông vũ',         N'Áo khoác',         N'UrbanBasic',       N'Rêu',                N'L',             699000, 1),
('SP041', N'Áo cardigan len dệt kim mỏng',           N'Áo khoác',         N'CasualWear',       N'Nâu be',             N'M',             359000, 1),
('SP042', N'Áo blazer nam hàn quốc 2 khuy',          N'Áo vest',          N'UrbanBasic',       N'Xám chì',            N'L',             799000, 1),
('SP043', N'Áo blazer nữ công sở dáng suông',        N'Áo vest',          N'Elegance',         N'Kem sữa',            N'M',             689000, 1),
('SP044', N'Áo khoác măng tô dáng dài',              N'Áo khoác',         N'Elegance',         N'Nâu camel',          N'L',            1190000, 1),
('SP045', N'Áo khoác da biker thời thượng',          N'Áo khoác',         N'StreetTrend',      N'Đen',                N'L',             899000, 1),
('SP046', N'Quần jean regular fit basic',            N'Quần',             N'DenimCo',          N'Xanh vừa',           N'31',            399000, 1),
('SP047', N'Quần jean regular fit basic',            N'Quần',             N'DenimCo',          N'Đen',                N'32',            399000, 1),
('SP048', N'Quần jean ống rộng baggy nam',           N'Quần',             N'StreetTrend',      N'Xanh nhạt',          N'30',            429000, 1),
('SP049', N'Quần jean rách gối phong cách',          N'Quần',             N'StreetTrend',      N'Xám khói',           N'31',            459000, 1),
('SP050', N'Quần jean nữ lưng cao skinny',           N'Quần',             N'DenimCo',          N'Xanh đậm',           N'27',            369000, 1),
('SP051', N'Quần jean nữ ống loe retro',             N'Quần',             N'Elegance',         N'Xanh vintage',       N'28',            419000, 1),
('SP052', N'Quần jean nữ ống suông rộng',            N'Quần',             N'StreetTrend',      N'Xanh sáng',          N'29',            399000, 1),
('SP053', N'Quần short jean nam gấu gấp',            N'Quần short',       N'DenimCo',          N'Xanh trung',         N'31',            279000, 1),
('SP054', N'Quần short jean nữ cạp cao',             N'Quần short',       N'CasualWear',       N'Trắng',              N'27',            249000, 1),
('SP055', N'Quần short jean nữ rách lai',            N'Quần short',       N'StreetTrend',      N'Xanh bạc',           N'28',            259000, 1),
('SP056', N'Quần tây nam xếp ly ống đứng',           N'Quần',             N'UrbanBasic',       N'Đen',                N'32',            429000, 1),
('SP057', N'Quần tây nam xếp ly ống đứng',           N'Quần',             N'UrbanBasic',       N'Xanh đen',           N'31',            429000, 1),
('SP058', N'Quần tây nữ ống suông lưng thun',        N'Quần',             N'Elegance',         N'Kem be',             N'M',             389000, 1),
('SP059', N'Quần tây nữ ống loe xẻ tà',              N'Quần',             N'Elegance',         N'Đen',                N'S',             419000, 1),
('SP060', N'Quần kaki túi hộp cargo nam',            N'Quần',             N'StreetTrend',      N'Rêu quân đội',       N'32',            459000, 1),
('SP061', N'Quần kaki túi hộp cargo nam',            N'Quần',             N'StreetTrend',      N'Đen',                N'33',            459000, 1),
('SP062', N'Quần kaki slimfit co giãn',              N'Quần',             N'UrbanBasic',       N'Nâu hạt dẻ',         N'30',            359000, 1),
('SP063', N'Quần jogger nỉ thể thao bo gấu',         N'Quần',             N'ActiveSport',      N'Đen',                N'L',             299000, 1),
('SP064', N'Quần jogger nỉ thể thao bo gấu',         N'Quần',             N'ActiveSport',      N'Xám nhạt',           N'M',             299000, 1),
('SP065', N'Quần đũi dài ống suông mùa hè',          N'Quần',             N'CasualWear',       N'Trắng ngà',          N'L',             319000, 1),
('SP066', N'Đầm chữ A cổ yếm dự tiệc',               N'Đầm',              N'Elegance',         N'Đỏ ruby',            N'M',             599000, 1),
('SP067', N'Đầm lụa maxi hai dây hoa nhí',           N'Đầm',              N'Elegance',         N'Vàng mơ',            N'S',             549000, 1),
('SP068', N'Đầm suông linen thêu hoa thủ công',      N'Đầm',              N'CasualWear',       N'Xanh ngọc',          N'L',             489000, 1),
('SP069', N'Đầm sơ mi công sở kèm thắt lưng',        N'Đầm',              N'Elegance',         N'Xanh navy',          N'M',             529000, 1),
('SP070', N'Đầm babydoll dễ thương tay bồng',        N'Đầm',              N'CasualWear',       N'Trắng',              N'FreeSize',      399000, 1),
('SP071', N'Chân váy chữ A xếp ly ngắn',             N'Chân váy',         N'CasualWear',       N'Nâu caro',           N'S',             259000, 1),
('SP072', N'Chân váy tennis thể thao năng động',     N'Chân váy',         N'ActiveSport',      N'Trắng',              N'M',             229000, 1),
('SP073', N'Chân váy midi voan hoa xòe',             N'Chân váy',         N'Elegance',         N'Họa tiết hoa',       N'FreeSize',      339000, 1),
('SP074', N'Chân váy jean dài xẻ tà trước',          N'Chân váy',         N'DenimCo',          N'Xanh chàm',          N'M',             379000, 1),
('SP075', N'Chân váy da chữ A cá tính',              N'Chân váy',         N'StreetTrend',      N'Đen',                N'S',             349000, 1),
('SP076', N'Áo hoodie zip nỉ bông unisex',           N'Áo hoodie',        N'StreetTrend',      N'Xám lông chuột',     N'L',             419000, 1),
('SP077', N'Áo hoodie zip nỉ bông unisex',           N'Áo hoodie',        N'StreetTrend',      N'Xanh rêu',           N'XL',            419000, 1),
('SP078', N'Áo nỉ sweater cổ tròn basic',            N'Áo hoodie',        N'UrbanBasic',       N'Xanh navy',          N'M',             329000, 1),
('SP079', N'Áo nỉ sweater in chữ nổi',               N'Áo hoodie',        N'StreetTrend',      N'Đen',                N'L',             349000, 1),
('SP080', N'Áo len gile cổ tim retro',               N'Áo len',           N'CasualWear',       N'Xanh rêu phối kem',  N'L',             289000, 1),
('SP081', N'Áo len vặn thừng dày ấm',                N'Áo len',           N'CasualWear',       N'Nâu cà phê',         N'XL',            459000, 1),
('SP082', N'Áo len mỏng dệt tăm ôm body',            N'Áo len',           N'Elegance',         N'Đen',                N'S',             269000, 1),
('SP083', N'Áo khoác dạ tweed tiểu thư',             N'Áo khoác',         N'Elegance',         N'Trắng viền đen',     N'M',             799000, 1),
('SP084', N'Áo khoác dạ tweed tiểu thư',             N'Áo khoác',         N'Elegance',         N'Hồng pastel',        N'S',             799000, 1),
('SP085', N'Áo len croptop tay dài sọc ngang',       N'Áo len',           N'CasualWear',       N'Đen/Trắng',          N'M',             279000, 1),
('SP086', N'Quần short thun thể thao nam',           N'Quần short',       N'ActiveSport',      N'Đen',                N'L',             179000, 1),
('SP087', N'Quần short thun thể thao nam',           N'Quần short',       N'ActiveSport',      N'Xám',                N'XL',            179000, 1),
('SP088', N'Quần legging cạp cao co giãn nữ',        N'Quần',             N'ActiveSport',      N'Đen',                N'M',             219000, 1),
('SP089', N'Quần đùi chạy bộ 2 lớp nam',             N'Quần short',       N'ActiveSport',      N'Xanh dạ quang',      N'L',             239000, 1),
('SP090', N'Bộ đồ thể thao phối sọc năng động',      N'Bộ đồ',            N'ActiveSport',      N'Đen viền trắng',     N'L',             529000, 1),
('SP091', N'Áo thun polo dệt kim cổ mở',             N'Áo polo',          N'UrbanBasic',       N'Trắng be',           N'M',             339000, 1),
('SP092', N'Quần đũi lửng thoáng mát hè',            N'Quần short',       N'CasualWear',       N'Xanh nhạt',          N'31',            219000, 1),
('SP093', N'Áo khoác kimono cách điệu',              N'Áo khoác',         N'CasualWear',       N'Họa tiết sóng',      N'FreeSize',      359000, 1),
('SP094', N'Đầm ôm body sát nách quyến rũ',          N'Đầm',              N'Elegance',         N'Đen',                N'S',             399000, 1),
('SP095', N'Đầm thun suông cổ tròn năng động',       N'Đầm',              N'CasualWear',       N'Xám tiêu',           N'M',             299000, 1),
('SP096', N'Thắt lưng da bò nam khóa tự động',       N'Phụ kiện',         N'UrbanBasic',       N'Đen',                N'FreeSize',      299000, 1),
('SP097', N'Thắt lưng nữ khóa kim loại mảnh',        N'Phụ kiện',         N'Elegance',         N'Nâu sáng',           N'FreeSize',      189000, 1),
('SP098', N'Nón lưỡi trai vintage thêu chữ',         N'Phụ kiện',         N'StreetTrend',      N'Xanh navy',          N'FreeSize',      149000, 1),
('SP099', N'Nón bucket vành tròn vải canvas',        N'Phụ kiện',         N'StreetTrend',      N'Be',                 N'FreeSize',      169000, 1),
('SP100', N'Túi tote canvas thời trang có khóa kéo',  N'Phụ kiện',         N'CasualWear',       N'Trắng ngà',          N'FreeSize',      159000, 1);
GO
-- -------------------------------------------------
-- 2. KHACH HANG (Customers) — Replicated two-way to all sites
-- Site-prefixed IDs: Q1-KH001, Q7-KH001, TD-KH001, etc.
-- -------------------------------------------------
-- Quận 1 customers
INSERT INTO [dbo].[KhachHang] ([MaKhachHang], [HoTen], [SoDienThoai], [Email], [DiaChi], [DiemTichLuy], [NgayDangKy], [TrangThai]) VALUES
('Q1-KH001', N'Nguyễn Văn An',       '0901111001', 'an.q1@email.com', N'12 Nguyễn Huệ, Quận 1',     1200, DATEADD(month, -9, GETDATE()), 1),
('Q1-KH002', N'Trần Thị Bình',       '0901111002', 'binh.q1@email.com', N'45 Lê Lợi, Quận 1',         850,  DATEADD(month, -8, GETDATE()), 1),
('Q1-KH003', N'Lê Hoàng Cường',      '0901111003', 'cuong.q1@email.com', N'78 Đồng Khởi, Quận 1',     2100, DATEADD(month, -7, GETDATE()), 1),
('Q1-KH004', N'Phạm Thị Dung',       '0901111004', 'dung.q1@email.com', N'23 Hai Bà Trưng, Quận 1',   560,  DATEADD(month, -6, GETDATE()), 1),
('Q1-KH005', N'Võ Minh Đức',         '0901111005', 'duc.q1@email.com', N'56 Pasteur, Quận 1',        3200, DATEADD(month, -5, GETDATE()), 1);

-- Quận 7 customers
INSERT INTO [dbo].[KhachHang] ([MaKhachHang], [HoTen], [SoDienThoai], [Email], [DiaChi], [DiemTichLuy], [NgayDangKy], [TrangThai]) VALUES
('Q7-KH001', N'Hoàng Thị Lan',       '0902222001', 'lan.q7@email.com', N'123 Nguyễn Thị Thập, Quận 7', 950,  DATEADD(month, -9, GETDATE()), 1),
('Q7-KH002', N'Đặng Văn Minh',       '0902222002', 'minh.q7@email.com', N'456 Huỳnh Tấn Phát, Quận 7', 1800, DATEADD(month, -8, GETDATE()), 1),
('Q7-KH003', N'Ngô Thị Nga',         '0902222003', 'nga.q7@email.com', N'789 Phạm Văn Hai, Quận 7',   410,  DATEADD(month, -7, GETDATE()), 1),
('Q7-KH004', N'Bùi Quốc Oai',        '0902222004', 'oai.q7@email.com', N'321 Lê Văn Lương, Quận 7',   2750, DATEADD(month, -6, GETDATE()), 1),
('Q7-KH005', N'Đinh Thị Phượng',     '0902222005', 'phuong.q7@email.com', N'654 Nguyễn Văn Linh, Quận 7', 680, DATEADD(month, -5, GETDATE()), 1);

-- Thủ Đức customers
INSERT INTO [dbo].[KhachHang] ([MaKhachHang], [HoTen], [SoDienThoai], [Email], [DiaChi], [DiemTichLuy], [NgayDangKy], [TrangThai]) VALUES
('TD-KH001', N'Trịnh Văn Quang',     '0903333001', 'quang.td@email.com', N'123 Võ Văn Ngân, Thủ Đức',    1350, DATEADD(month, -9, GETDATE()), 1),
('TD-KH002', N'Lý Thị Quyên',        '0903333002', 'quyen.td@email.com', N'456 Đường 3/2, Thủ Đức',      720,  DATEADD(month, -8, GETDATE()), 1),
('TD-KH003', N'Huỳnh Văn Rồng',      '0903333003', 'rong.td@email.com', N'789 Nguyễn Xí, Thủ Đức',       1980, DATEADD(month, -7, GETDATE()), 1),
('TD-KH004', N'Đỗ Thị Sen',          '0903333004', 'sen.td@email.com', N'321 Linh Chiểu, Thủ Đức',      540,  DATEADD(month, -6, GETDATE()), 1),
('TD-KH005', N'Mai Văn Tuấn',        '0903333005', 'tuan.td@email.com', N'654 Hiệp Phú, Thủ Đức',        2450, DATEADD(month, -5, GETDATE()), 1);
GO

-- -------------------------------------------------
-- 3. NHAN VIEN (Employees) — Central has all, sites filtered by MaChiNhanh
-- IDs: NV00000001, NV00000002, ...
-- -------------------------------------------------
-- Quận 1 staff (CN1)
INSERT INTO [dbo].[NhanVien] ([MaNhanVien], [MaChiNhanh], [HoTen], [NgaySinh], [GioiTinh], [SoDienThoai], [Email], [ChucVu], [TrangThai]) VALUES
('NV00000001', 'CN1', N'Nguyễn Văn Staff Q1',   '1995-03-10', N'Nam',  '0911000001', 'staff1.q1@fashionstore.vn', N'Nhân viên bán hàng', 1),
('NV00000002', 'CN1', N'Trần Thị Staff Q1',    '1996-07-22', N'Nữ',   '0911000002', 'staff2.q1@fashionstore.vn', N'Nhân viên bán hàng', 1),
('NV00000003', 'CN1', N'Lê Văn Manager Q1',    '1990-11-05', N'Nam',  '0911000003', 'manager.q1@fashionstore.vn',  N'Quản lý chi nhánh',  1);

-- Quận 7 staff (CN2)
INSERT INTO [dbo].[NhanVien] ([MaNhanVien], [MaChiNhanh], [HoTen], [NgaySinh], [GioiTinh], [SoDienThoai], [Email], [ChucVu], [TrangThai]) VALUES
('NV00000004', 'CN2', N'Phạm Thị Staff Q7',    '1994-05-18', N'Nữ',   '0912000001', 'staff1.q7@fashionstore.vn', N'Nhân viên bán hàng', 1),
('NV00000005', 'CN2', N'Hoàng Văn Staff Q7',   '1997-09-30', N'Nam',  '0912000002', 'staff2.q7@fashionstore.vn', N'Nhân viên bán hàng', 1),
('NV00000006', 'CN2', N'Võ Thị Manager Q7',    '1989-12-12', N'Nữ',   '0912000003', 'manager.q7@fashionstore.vn',  N'Quản lý chi nhánh',  1);

-- Thủ Đức staff (CN3)
INSERT INTO [dbo].[NhanVien] ([MaNhanVien], [MaChiNhanh], [HoTen], [NgaySinh], [GioiTinh], [SoDienThoai], [Email], [ChucVu], [TrangThai]) VALUES
('NV00000007', 'CN3', N'Đặng Văn Staff TD',    '1996-02-14', N'Nam',  '0913000001', 'staff1.td@fashionstore.vn', N'Nhân viên bán hàng', 1),
('NV00000008', 'CN3', N'Bùi Thị Staff TD',     '1998-08-25', N'Nữ',   '0913000002', 'staff2.td@fashionstore.vn', N'Nhân viên bán hàng', 1),
('NV00000009', 'CN3', N'Ngô Văn Manager TD',   '1991-04-07', N'Nam',  '0913000003', 'manager.td@fashionstore.vn',  N'Quản lý chi nhánh',  1);

-- Central admin (CN0)
INSERT INTO [dbo].[NhanVien] ([MaNhanVien], [MaChiNhanh], [HoTen], [NgaySinh], [GioiTinh], [SoDienThoai], [Email], [ChucVu], [TrangThai]) VALUES
('NV00000010', 'CN0', N'Admin Hệ Thống',       '1985-01-01', N'Nam',  '0900000000', 'admin@fashionstore.vn',       N'Quản trị hệ thống',   1);
GO

-- -------------------------------------------------
-- 4. TAI KHOAN (Accounts) — Linked to NhanVien, replicated to sites (read-only)
-- Password hash for '123456' using BCrypt (verified with Spring BCryptPasswordEncoder)
-- MaTaiKhoan is IDENTITY, so we don't specify it.
-- -------------------------------------------------
INSERT INTO [dbo].[TaiKhoan] ([MaNhanVien], [TenDangNhap], [MatKhau], [VaiTro], [TrangThai]) VALUES
('NV00000001', 'staff.q1',    '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'STAFF',   1),
('NV00000002', 'staff2.q1',   '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'STAFF',   1),
('NV00000003', 'manager.q1',  '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'MANAGER', 1),

('NV00000004', 'staff.q7',    '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'STAFF',   1),
('NV00000005', 'staff2.q7',   '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'STAFF',   1),
('NV00000006', 'manager.q7',  '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'MANAGER', 1),

('NV00000007', 'staff.td',    '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'STAFF',   1),
('NV00000008', 'staff2.td',   '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'STAFF',   1),
('NV00000009', 'manager.td',  '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'MANAGER', 1),

('NV00000010', 'admin',       '$2a$10$c1Z/kBQfn5y.CI8tMk6H3e7/U57NO/HjEr0t192yDVzgoNI2MhvIW', 'ADMIN',   1);
GO

-- -------------------------------------------------
-- 5. TON KHO (Inventory) — Each site manages its own; Central aggregates via view
-- Seed with initial stock for all 100 products at each site (CN1, CN2, CN3)
-- -------------------------------------------------
-- Quận 1 (CN1) stock: 100 products
INSERT INTO [dbo].[TonKho] ([MaChiNhanh], [MaSanPham], [SoLuongTon], [MucTonToiThieu], [NgayCapNhat]) VALUES
('CN1', 'SP001',  50, 5, GETDATE()),
('CN1', 'SP002',  45, 5, GETDATE()),
('CN1', 'SP003',  20, 3, GETDATE()),
('CN1', 'SP004',  30, 5, GETDATE()),
('CN1', 'SP005',  25, 5, GETDATE()),
('CN1', 'SP006',  35, 5, GETDATE()),
('CN1', 'SP007',  28, 5, GETDATE()),
('CN1', 'SP008',  15, 3, GETDATE()),
('CN1', 'SP009',  22, 3, GETDATE()),
('CN1', 'SP010',  18, 3, GETDATE()),
('CN1', 'SP011',  40, 5, GETDATE()),
('CN1', 'SP012',  38, 5, GETDATE()),
('CN1', 'SP013',  30, 5, GETDATE()),
('CN1', 'SP014',  20, 3, GETDATE()),
('CN1', 'SP015',  10, 2, GETDATE()),
('CN1', 'SP016',  43, 5, GETDATE()),
('CN1', 'SP017',  50, 5, GETDATE()),
('CN1', 'SP018',  57, 5, GETDATE()),
('CN1', 'SP019',  19, 3, GETDATE()),
('CN1', 'SP020',   3, 5, GETDATE()),
('CN1', 'SP021',  33, 5, GETDATE()),
('CN1', 'SP022',  40, 5, GETDATE()),
('CN1', 'SP023',  47, 5, GETDATE()),
('CN1', 'SP024',  54, 5, GETDATE()),
('CN1', 'SP025',   4, 5, GETDATE()),
('CN1', 'SP026',  23, 3, GETDATE()),
('CN1', 'SP027',  30, 5, GETDATE()),
('CN1', 'SP028',  37, 5, GETDATE()),
('CN1', 'SP029',  44, 5, GETDATE()),
('CN1', 'SP030',  51, 5, GETDATE()),
('CN1', 'SP031',  58, 5, GETDATE()),
('CN1', 'SP032',  20, 3, GETDATE()),
('CN1', 'SP033',  27, 5, GETDATE()),
('CN1', 'SP034',  34, 5, GETDATE()),
('CN1', 'SP035',  41, 5, GETDATE()),
('CN1', 'SP036',  48, 5, GETDATE()),
('CN1', 'SP037',  55, 5, GETDATE()),
('CN1', 'SP038',  17, 3, GETDATE()),
('CN1', 'SP039',  24, 3, GETDATE()),
('CN1', 'SP040',  31, 5, GETDATE()),
('CN1', 'SP041',  38, 5, GETDATE()),
('CN1', 'SP042',  45, 5, GETDATE()),
('CN1', 'SP043',  52, 5, GETDATE()),
('CN1', 'SP044',  59, 5, GETDATE()),
('CN1', 'SP045',   3, 5, GETDATE()),
('CN1', 'SP046',  28, 5, GETDATE()),
('CN1', 'SP047',  35, 5, GETDATE()),
('CN1', 'SP048',  42, 5, GETDATE()),
('CN1', 'SP049',  49, 5, GETDATE()),
('CN1', 'SP050',  56, 5, GETDATE()),
('CN1', 'SP051',  18, 3, GETDATE()),
('CN1', 'SP052',  25, 5, GETDATE()),
('CN1', 'SP053',  32, 5, GETDATE()),
('CN1', 'SP054',  39, 5, GETDATE()),
('CN1', 'SP055',  46, 5, GETDATE()),
('CN1', 'SP056',  53, 5, GETDATE()),
('CN1', 'SP057',  15, 3, GETDATE()),
('CN1', 'SP058',  22, 3, GETDATE()),
('CN1', 'SP059',  29, 5, GETDATE()),
('CN1', 'SP060',  36, 5, GETDATE()),
('CN1', 'SP061',  43, 5, GETDATE()),
('CN1', 'SP062',  50, 5, GETDATE()),
('CN1', 'SP063',  57, 5, GETDATE()),
('CN1', 'SP064',  19, 3, GETDATE()),
('CN1', 'SP065',  26, 5, GETDATE()),
('CN1', 'SP066',   4, 5, GETDATE()),
('CN1', 'SP067',  40, 5, GETDATE()),
('CN1', 'SP068',  47, 5, GETDATE()),
('CN1', 'SP069',  54, 5, GETDATE()),
('CN1', 'SP070',  16, 3, GETDATE()),
('CN1', 'SP071',  23, 3, GETDATE()),
('CN1', 'SP072',  30, 5, GETDATE()),
('CN1', 'SP073',  37, 5, GETDATE()),
('CN1', 'SP074',  44, 5, GETDATE()),
('CN1', 'SP075',  51, 5, GETDATE()),
('CN1', 'SP076',  58, 5, GETDATE()),
('CN1', 'SP077',  20, 3, GETDATE()),
('CN1', 'SP078',  27, 5, GETDATE()),
('CN1', 'SP079',  34, 5, GETDATE()),
('CN1', 'SP080',  41, 5, GETDATE()),
('CN1', 'SP081',  48, 5, GETDATE()),
('CN1', 'SP082',  55, 5, GETDATE()),
('CN1', 'SP083',   3, 5, GETDATE()),
('CN1', 'SP084',  24, 3, GETDATE()),
('CN1', 'SP085',  31, 5, GETDATE()),
('CN1', 'SP086',  38, 5, GETDATE()),
('CN1', 'SP087',  45, 5, GETDATE()),
('CN1', 'SP088',  52, 5, GETDATE()),
('CN1', 'SP089',  59, 5, GETDATE()),
('CN1', 'SP090',  21, 3, GETDATE()),
('CN1', 'SP091',  28, 5, GETDATE()),
('CN1', 'SP092',  35, 5, GETDATE()),
('CN1', 'SP093',  42, 5, GETDATE()),
('CN1', 'SP094',  49, 5, GETDATE()),
('CN1', 'SP095',  56, 5, GETDATE()),
('CN1', 'SP096',   4, 5, GETDATE()),
('CN1', 'SP097',  25, 5, GETDATE()),
('CN1', 'SP098',  32, 5, GETDATE()),
('CN1', 'SP099',  39, 5, GETDATE()),
('CN1', 'SP100',  46, 5, GETDATE());

-- Quận 7 (CN2) stock: 100 products
INSERT INTO [dbo].[TonKho] ([MaChiNhanh], [MaSanPham], [SoLuongTon], [MucTonToiThieu], [NgayCapNhat]) VALUES
('CN2', 'SP001',  40, 5, GETDATE()),
('CN2', 'SP002',  35, 5, GETDATE()),
('CN2', 'SP003',  18, 3, GETDATE()),
('CN2', 'SP004',  28, 5, GETDATE()),
('CN2', 'SP005',  22, 3, GETDATE()),
('CN2', 'SP006',  30, 5, GETDATE()),
('CN2', 'SP007',  25, 5, GETDATE()),
('CN2', 'SP008',  12, 2, GETDATE()),
('CN2', 'SP009',  20, 3, GETDATE()),
('CN2', 'SP010',  15, 3, GETDATE()),
('CN2', 'SP011',  35, 5, GETDATE()),
('CN2', 'SP012',  32, 5, GETDATE()),
('CN2', 'SP013',  28, 5, GETDATE()),
('CN2', 'SP014',  18, 3, GETDATE()),
('CN2', 'SP015',   8, 2, GETDATE()),
('CN2', 'SP016',  24, 5, GETDATE()),
('CN2', 'SP017',  35, 5, GETDATE()),
('CN2', 'SP018',  46, 5, GETDATE()),
('CN2', 'SP019',  17, 3, GETDATE()),
('CN2', 'SP020',  28, 5, GETDATE()),
('CN2', 'SP021',  39, 5, GETDATE()),
('CN2', 'SP022',  50, 5, GETDATE()),
('CN2', 'SP023',  21, 5, GETDATE()),
('CN2', 'SP024',  32, 5, GETDATE()),
('CN2', 'SP025',  43, 5, GETDATE()),
('CN2', 'SP026',  14, 3, GETDATE()),
('CN2', 'SP027',  25, 5, GETDATE()),
('CN2', 'SP028',  36, 5, GETDATE()),
('CN2', 'SP029',  47, 5, GETDATE()),
('CN2', 'SP030',   2, 5, GETDATE()),
('CN2', 'SP031',  29, 5, GETDATE()),
('CN2', 'SP032',  40, 5, GETDATE()),
('CN2', 'SP033',  51, 5, GETDATE()),
('CN2', 'SP034',  22, 5, GETDATE()),
('CN2', 'SP035',  33, 5, GETDATE()),
('CN2', 'SP036',  44, 5, GETDATE()),
('CN2', 'SP037',  15, 3, GETDATE()),
('CN2', 'SP038',  26, 5, GETDATE()),
('CN2', 'SP039',   4, 5, GETDATE()),
('CN2', 'SP040',  48, 5, GETDATE()),
('CN2', 'SP041',  19, 3, GETDATE()),
('CN2', 'SP042',  30, 5, GETDATE()),
('CN2', 'SP043',  41, 5, GETDATE()),
('CN2', 'SP044',  12, 3, GETDATE()),
('CN2', 'SP045',  23, 5, GETDATE()),
('CN2', 'SP046',  34, 5, GETDATE()),
('CN2', 'SP047',  45, 5, GETDATE()),
('CN2', 'SP048',  16, 3, GETDATE()),
('CN2', 'SP049',  27, 5, GETDATE()),
('CN2', 'SP050',  38, 5, GETDATE()),
('CN2', 'SP051',   2, 5, GETDATE()),
('CN2', 'SP052',  20, 5, GETDATE()),
('CN2', 'SP053',  31, 5, GETDATE()),
('CN2', 'SP054',  42, 5, GETDATE()),
('CN2', 'SP055',  13, 3, GETDATE()),
('CN2', 'SP056',  24, 5, GETDATE()),
('CN2', 'SP057',  35, 5, GETDATE()),
('CN2', 'SP058',  46, 5, GETDATE()),
('CN2', 'SP059',  17, 3, GETDATE()),
('CN2', 'SP060',  28, 5, GETDATE()),
('CN2', 'SP061',  39, 5, GETDATE()),
('CN2', 'SP062',  50, 5, GETDATE()),
('CN2', 'SP063',  21, 5, GETDATE()),
('CN2', 'SP064',  32, 5, GETDATE()),
('CN2', 'SP065',  43, 5, GETDATE()),
('CN2', 'SP066',  14, 3, GETDATE()),
('CN2', 'SP067',  25, 5, GETDATE()),
('CN2', 'SP068',  36, 5, GETDATE()),
('CN2', 'SP069',  47, 5, GETDATE()),
('CN2', 'SP070',  18, 3, GETDATE()),
('CN2', 'SP071',  29, 5, GETDATE()),
('CN2', 'SP072',  40, 5, GETDATE()),
('CN2', 'SP073',  51, 5, GETDATE()),
('CN2', 'SP074',  22, 5, GETDATE()),
('CN2', 'SP075',   2, 5, GETDATE()),
('CN2', 'SP076',  44, 5, GETDATE()),
('CN2', 'SP077',  15, 3, GETDATE()),
('CN2', 'SP078',  26, 5, GETDATE()),
('CN2', 'SP079',  37, 5, GETDATE()),
('CN2', 'SP080',  48, 5, GETDATE()),
('CN2', 'SP081',  19, 3, GETDATE()),
('CN2', 'SP082',  30, 5, GETDATE()),
('CN2', 'SP083',  41, 5, GETDATE()),
('CN2', 'SP084',   4, 5, GETDATE()),
('CN2', 'SP085',  23, 5, GETDATE()),
('CN2', 'SP086',  34, 5, GETDATE()),
('CN2', 'SP087',  45, 5, GETDATE()),
('CN2', 'SP088',  16, 3, GETDATE()),
('CN2', 'SP089',  27, 5, GETDATE()),
('CN2', 'SP090',  38, 5, GETDATE()),
('CN2', 'SP091',  49, 5, GETDATE()),
('CN2', 'SP092',  20, 5, GETDATE()),
('CN2', 'SP093',  31, 5, GETDATE()),
('CN2', 'SP094',  42, 5, GETDATE()),
('CN2', 'SP095',  13, 3, GETDATE()),
('CN2', 'SP096',  24, 5, GETDATE()),
('CN2', 'SP097',  35, 5, GETDATE()),
('CN2', 'SP098',  46, 5, GETDATE()),
('CN2', 'SP099',  17, 3, GETDATE()),
('CN2', 'SP100',  28, 5, GETDATE());

-- Thủ Đức (CN3) stock: 100 products
INSERT INTO [dbo].[TonKho] ([MaChiNhanh], [MaSanPham], [SoLuongTon], [MucTonToiThieu], [NgayCapNhat]) VALUES
('CN3', 'SP001',  45, 5, GETDATE()),
('CN3', 'SP002',  42, 5, GETDATE()),
('CN3', 'SP003',  22, 3, GETDATE()),
('CN3', 'SP004',  32, 5, GETDATE()),
('CN3', 'SP005',  28, 5, GETDATE()),
('CN3', 'SP006',  33, 5, GETDATE()),
('CN3', 'SP007',  27, 5, GETDATE()),
('CN3', 'SP008',  16, 3, GETDATE()),
('CN3', 'SP009',  24, 3, GETDATE()),
('CN3', 'SP010',  20, 3, GETDATE()),
('CN3', 'SP011',  38, 5, GETDATE()),
('CN3', 'SP012',  35, 5, GETDATE()),
('CN3', 'SP013',  32, 5, GETDATE()),
('CN3', 'SP014',  22, 3, GETDATE()),
('CN3', 'SP015',  12, 2, GETDATE()),
('CN3', 'SP016',  48, 5, GETDATE()),
('CN3', 'SP017',  19, 3, GETDATE()),
('CN3', 'SP018',   3, 5, GETDATE()),
('CN3', 'SP019',  45, 5, GETDATE()),
('CN3', 'SP020',  16, 3, GETDATE()),
('CN3', 'SP021',  29, 5, GETDATE()),
('CN3', 'SP022',  42, 5, GETDATE()),
('CN3', 'SP023',  55, 5, GETDATE()),
('CN3', 'SP024',  26, 5, GETDATE()),
('CN3', 'SP025',  39, 5, GETDATE()),
('CN3', 'SP026',  52, 5, GETDATE()),
('CN3', 'SP027',  23, 5, GETDATE()),
('CN3', 'SP028',  36, 5, GETDATE()),
('CN3', 'SP029',  49, 5, GETDATE()),
('CN3', 'SP030',  20, 3, GETDATE()),
('CN3', 'SP031',  33, 5, GETDATE()),
('CN3', 'SP032',  46, 5, GETDATE()),
('CN3', 'SP033',  17, 3, GETDATE()),
('CN3', 'SP034',  30, 5, GETDATE()),
('CN3', 'SP035',  43, 5, GETDATE()),
('CN3', 'SP036',  56, 5, GETDATE()),
('CN3', 'SP037',  27, 5, GETDATE()),
('CN3', 'SP038',  40, 5, GETDATE()),
('CN3', 'SP039',  53, 5, GETDATE()),
('CN3', 'SP040',  24, 5, GETDATE()),
('CN3', 'SP041',  37, 5, GETDATE()),
('CN3', 'SP042',   4, 5, GETDATE()),
('CN3', 'SP043',  21, 3, GETDATE()),
('CN3', 'SP044',  34, 5, GETDATE()),
('CN3', 'SP045',  47, 5, GETDATE()),
('CN3', 'SP046',  18, 3, GETDATE()),
('CN3', 'SP047',  31, 5, GETDATE()),
('CN3', 'SP048',  44, 5, GETDATE()),
('CN3', 'SP049',  57, 5, GETDATE()),
('CN3', 'SP050',  28, 5, GETDATE()),
('CN3', 'SP051',  41, 5, GETDATE()),
('CN3', 'SP052',  54, 5, GETDATE()),
('CN3', 'SP053',  25, 5, GETDATE()),
('CN3', 'SP054',  38, 5, GETDATE()),
('CN3', 'SP055',  51, 5, GETDATE()),
('CN3', 'SP056',  22, 5, GETDATE()),
('CN3', 'SP057',  35, 5, GETDATE()),
('CN3', 'SP058',  48, 5, GETDATE()),
('CN3', 'SP059',  19, 3, GETDATE()),
('CN3', 'SP060',  32, 5, GETDATE()),
('CN3', 'SP061',   3, 5, GETDATE()),
('CN3', 'SP062',  16, 3, GETDATE()),
('CN3', 'SP063',  29, 5, GETDATE()),
('CN3', 'SP064',  42, 5, GETDATE()),
('CN3', 'SP065',  55, 5, GETDATE()),
('CN3', 'SP066',  26, 5, GETDATE()),
('CN3', 'SP067',  39, 5, GETDATE()),
('CN3', 'SP068',  52, 5, GETDATE()),
('CN3', 'SP069',  23, 5, GETDATE()),
('CN3', 'SP070',  36, 5, GETDATE()),
('CN3', 'SP071',  49, 5, GETDATE()),
('CN3', 'SP072',   4, 5, GETDATE()),
('CN3', 'SP073',  33, 5, GETDATE()),
('CN3', 'SP074',  46, 5, GETDATE()),
('CN3', 'SP075',  17, 3, GETDATE()),
('CN3', 'SP076',  30, 5, GETDATE()),
('CN3', 'SP077',  43, 5, GETDATE()),
('CN3', 'SP078',  56, 5, GETDATE()),
('CN3', 'SP079',  27, 5, GETDATE()),
('CN3', 'SP080',  40, 5, GETDATE()),
('CN3', 'SP081',  53, 5, GETDATE()),
('CN3', 'SP082',  24, 5, GETDATE()),
('CN3', 'SP083',  37, 5, GETDATE()),
('CN3', 'SP084',  50, 5, GETDATE()),
('CN3', 'SP085',  21, 3, GETDATE()),
('CN3', 'SP086',  34, 5, GETDATE()),
('CN3', 'SP087',  47, 5, GETDATE()),
('CN3', 'SP088',  18, 3, GETDATE()),
('CN3', 'SP089',  31, 5, GETDATE()),
('CN3', 'SP090',  44, 5, GETDATE()),
('CN3', 'SP091',   3, 5, GETDATE()),
('CN3', 'SP092',  28, 5, GETDATE()),
('CN3', 'SP093',  41, 5, GETDATE()),
('CN3', 'SP094',  54, 5, GETDATE()),
('CN3', 'SP095',  25, 5, GETDATE()),
('CN3', 'SP096',  38, 5, GETDATE()),
('CN3', 'SP097',  51, 5, GETDATE()),
('CN3', 'SP098',  22, 5, GETDATE()),
('CN3', 'SP099',  35, 5, GETDATE()),
('CN3', 'SP100',  48, 5, GETDATE());
GO

-- -------------------------------------------------
-- 6. HOA DON & CHI TIET HOA DON (Sample Invoices)
-- Site-prefixed invoice IDs: Q1-HD000001, Q7-HD000001, TD-HD000001
-- -------------------------------------------------
-- Quận 1 invoices
INSERT INTO [dbo].[HoaDon] ([MaHoaDon], [MaChiNhanh], [MaKhachHang], [MaNhanVien], [NgayLap], [TongTien], [PhuongThucTT], [TrangThai]) VALUES
('Q1-HD000001', 'CN1', 'Q1-KH001', 'NV00000001', DATEADD(day, -6, GETDATE()), 398000, N'Tiền mặt', N'DA_THANH_TOAN'),
('Q1-HD000002', 'CN1', 'Q1-KH002', 'NV00000001', DATEADD(day, -5, GETDATE()), 499000, N'Chuyển khoản', N'DA_THANH_TOAN'),
('Q1-HD000003', 'CN1', 'Q1-KH003', 'NV00000002', DATEADD(day, -3, GETDATE()), 748000, N'Tiền mặt', N'DA_THANH_TOAN');

INSERT INTO [dbo].[ChiTietHoaDon] ([MaHoaDon], [MaSanPham], [SoLuong], [DonGia], [GiamGia], [ThanhTien]) VALUES
('Q1-HD000001', 'SP001', 2, 199000, 0, 398000),
('Q1-HD000002', 'SP003', 1, 499000, 0, 499000),
('Q1-HD000003', 'SP004', 1, 399000, 0, 399000),
('Q1-HD000003', 'SP005', 1, 349000, 0, 349000);

-- Quận 7 invoices
INSERT INTO [dbo].[HoaDon] ([MaHoaDon], [MaChiNhanh], [MaKhachHang], [MaNhanVien], [NgayLap], [TongTien], [PhuongThucTT], [TrangThai]) VALUES
('Q7-HD000001', 'CN2', 'Q7-KH001', 'NV00000004', DATEADD(day, -6, GETDATE()), 299000, N'Tiền mặt', N'DA_THANH_TOAN'),
('Q7-HD000002', 'CN2', 'Q7-KH002', 'NV00000005', DATEADD(day, -4, GETDATE()), 838000, N'Chuyển khoản', N'DA_THANH_TOAN');

INSERT INTO [dbo].[ChiTietHoaDon] ([MaHoaDon], [MaSanPham], [SoLuong], [DonGia], [GiamGia], [ThanhTien]) VALUES
('Q7-HD000001', 'SP006', 1, 299000, 0, 299000),
('Q7-HD000002', 'SP007', 1, 319000, 0, 319000),
('Q7-HD000002', 'SP008', 1, 449000, 0, 449000),
('Q7-HD000002', 'SP009', 1, 279000, -27900, 251100); -- with discount

-- Thủ Đức invoices
INSERT INTO [dbo].[HoaDon] ([MaHoaDon], [MaChiNhanh], [MaKhachHang], [MaNhanVien], [NgayLap], [TongTien], [PhuongThucTT], [TrangThai]) VALUES
('TD-HD000001', 'CN3', 'TD-KH001', 'NV00000007', DATEADD(day, -5, GETDATE()), 529000, N'Tiền mặt', N'DA_THANH_TOAN'),
('TD-HD000002', 'CN3', 'TD-KH003', 'NV00000008', DATEADD(day, -2, GETDATE()), 1288000, N'Thẻ', N'DA_THANH_TOAN');

INSERT INTO [dbo].[ChiTietHoaDon] ([MaHoaDon], [MaSanPham], [SoLuong], [DonGia], [GiamGia], [ThanhTien]) VALUES
('TD-HD000001', 'SP010', 1, 529000, 0, 529000),
('TD-HD000002', 'SP011', 2, 389000, 0, 778000),
('TD-HD000002', 'SP012', 1, 389000, 0, 389000),
('TD-HD000002', 'SP013', 1, 259000, -138000, 121000); -- bundle discount
GO

-- -------------------------------------------------
-- 7. VERIFICATION QUERIES
-- -------------------------------------------------
PRINT N'=== SEED DATA SUMMARY ===';
SELECT 'SanPham' AS Bang, COUNT(*) AS SoLuong FROM [dbo].[SanPham];
SELECT 'KhachHang' AS Bang, COUNT(*) AS SoLuong FROM [dbo].[KhachHang];
SELECT 'NhanVien' AS Bang, COUNT(*) AS SoLuong FROM [dbo].[NhanVien];
SELECT 'TaiKhoan' AS Bang, COUNT(*) AS SoLuong FROM [dbo].[TaiKhoan];
SELECT 'TonKho' AS Bang, COUNT(*) AS SoLuong FROM [dbo].[TonKho];
SELECT 'HoaDon' AS Bang, COUNT(*) AS SoLuong FROM [dbo].[HoaDon];
SELECT 'ChiTietHoaDon' AS Bang, COUNT(*) AS SoLuong FROM [dbo].[ChiTietHoaDon];

PRINT N'=== KhachHang by site prefix ===';
SELECT
    CASE
        WHEN MaKhachHang LIKE 'Q1-%' THEN 'Quận 1'
        WHEN MaKhachHang LIKE 'Q7-%' THEN 'Quận 7'
        WHEN MaKhachHang LIKE 'TD-%' THEN 'Thủ Đức'
        ELSE 'Other'
    END AS Site,
    COUNT(*) AS SoLuong
FROM [dbo].[KhachHang]
GROUP BY
    CASE
        WHEN MaKhachHang LIKE 'Q1-%' THEN 'Quận 1'
        WHEN MaKhachHang LIKE 'Q7-%' THEN 'Quận 7'
        WHEN MaKhachHang LIKE 'TD-%' THEN 'Thủ Đức'
        ELSE 'Other'
    END;

PRINT N'=== NhanVien by branch ===';
SELECT MaChiNhanh, COUNT(*) AS SoLuong FROM [dbo].[NhanVien] GROUP BY MaChiNhanh;

PRINT N'=== TonKho by branch ===';
SELECT MaChiNhanh, COUNT(*) AS SoSP, SUM(SoLuongTon) AS TongTon FROM [dbo].[TonKho] GROUP BY MaChiNhanh;

PRINT N'=== HoaDon by branch ===';
SELECT MaChiNhanh, COUNT(*) AS SoHD, SUM(TongTien) AS DoanhThu FROM [dbo].[HoaDon] GROUP BY MaChiNhanh;

PRINT N'Seed data inserted successfully.';
GO