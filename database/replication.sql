-- =====================================================================
-- MERGE REPLICATION — Central (publisher + distributor) -> 3 sites
-- Chốt B1 (Merge) theo mục 0.4 của docs/setup.md.
--
-- Run on the CENTRAL machine AFTER:
--   1) linked-servers.sql        (role = central)
--   2) Fashion_Central.sql       (Central database)
--   3) sites/Fashion_Store.sql   (on each of the 3 sites)
--   4) central-procs.sql
--   5) seed.sql
--
-- Prerequisites:
--   * SQL Server Agent is running on Central (Merge Agent và Snapshot Agent chạy ở đây).
--   * The snapshot folder is shared and reachable by all 4 machines, e.g.
--         New-Item -ItemType Directory -Path D:\ReplData -Force
--         New-SmbShare -Name REPLDATA -Path D:\ReplData -FullAccess Everyone
--     then set SnapshotShare below to \\<CENTRAL-PC>\REPLDATA
--   * distributed_user exists on Central and on every site (created by the
--     database scripts) with db_datareader + db_datawriter.
--
-- Run from the `database/` folder:
--     sqlcmd -S <instance> -E -i replication.sql -b -f 65001
-- =====================================================================

USE [master];
GO

-- -------------------------------------------------
-- CONFIG — EDIT BEFORE RUNNING
-- -------------------------------------------------
-- Tất cả máy dùng named instance FASHION trên cổng 14333, nên tên subscriber
-- phải kèm cổng: "<IP>,14333".
:setvar SnapshotShare      "\\CENTRAL-PC\REPLDATA"
:setvar Q1Subscriber       "192.168.1.11,14333"
:setvar Q7Subscriber       "192.168.1.12,14333"
:setvar TDSubscriber       "192.168.1.13,14333"
:setvar DistributorPassword "Fashion@123456"
:setvar SubscriberPassword  "Fashion@123456"

DECLARE @Publisher sysname = CAST(SERVERPROPERTY('ServerName') AS sysname);

-- -------------------------------------------------
-- 1. DISTRIBUTOR (local, on Central)
-- -------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM sys.servers WHERE is_distributor = 1 AND name = @Publisher)
BEGIN
    EXEC sp_adddistributor
        @distributor = @Publisher,
        @password    = N'$(DistributorPassword)';

    EXEC sp_adddistributiondb
        @database = N'distribution',
        @security_mode = 1;

    EXEC sp_adddistpublisher
        @publisher          = @Publisher,
        @distribution_db    = N'distribution',
        @security_mode      = 1,
        @working_directory  = N'$(SnapshotShare)',
        @trusted            = N'false',
        @thirdparty_flag    = 0,
        @publisher_type     = N'MSSQLSERVER',
        @password           = N'$(DistributorPassword)';

    PRINT N'Distributor configured on ' + @Publisher;
END
ELSE
    PRINT N'Distributor already configured on ' + @Publisher;
GO

-- -------------------------------------------------
-- 2. ENABLE MERGE PUBLISHING ON Fashion_Central
-- -------------------------------------------------
USE [Fashion_Central];
GO

IF (SELECT is_merge_published FROM sys.databases WHERE name = N'Fashion_Central') = 0
BEGIN
    EXEC sp_replicationdboption
        @dbname = N'Fashion_Central', @optname = N'merge publish', @value = N'true';
    PRINT N'Enabled merge publish on Fashion_Central.';
END
GO

IF (SELECT is_published FROM sys.databases WHERE name = N'Fashion_Central') = 0
BEGIN
    EXEC sp_replicationdboption
        @dbname = N'Fashion_Central', @optname = N'publish', @value = N'true';
    PRINT N'Enabled publish on Fashion_Central.';
END
GO

-- -------------------------------------------------
-- 3. PUBLICATION 1: Pub_Common (SanPham, ChiNhanh, KhachHang)
--    Publish tới cả 3 site: Q1, Q7, Thủ Đức
-- -------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergepublications WHERE name = N'Pub_Common')
BEGIN
    EXEC sp_addmergepublication
        @publication = N'Pub_Common',
        @description = N'Bang chung: SanPham, ChiNhanh, KhachHang — push tat ca site',
        @status = N'active',
        @allow_push = N'true',
        @allow_pull = N'true',
        @repl_freq = N'continuous',
        @independent_agent = N'true',
        @retention = 14,
        @history_retention = 48;

    EXEC sp_addpublication_snapshot
        @publication = N'Pub_Common',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Merge publication Pub_Common created.';
END
GO

-- Article SanPham (khong loc)
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'SanPham' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Common'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Common',
        @article = N'SanPham',
        @source_object = N'SanPham',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @description = N'SanPham — danh muc chung';
END
GO

-- Article ChiNhanh (khong loc)
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'ChiNhanh' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Common'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Common',
        @article = N'ChiNhanh',
        @source_object = N'ChiNhanh',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @description = N'ChiNhanh — danh muc chung';
END
GO

-- Article KhachHang (khong loc, dong bo hai chieu qua Merge)
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'KhachHang' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Common'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Common',
        @article = N'KhachHang',
        @source_object = N'KhachHang',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @description = N'KhachHang — dong bo hai chieu';
END
GO

-- Push Subscriptions for Pub_Common to all 3 sites
-- To Q1
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergesubscriptions WHERE pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Common') AND subscriber_server = N'$(Q1Subscriber)')
BEGIN
    EXEC sp_addmergesubscription
        @publication = N'Pub_Common',
        @subscriber = N'$(Q1Subscriber)',
        @subscriber_db = N'Fashion_Store',
        @subscription_type = N'push',
        @subscriber_type = 0,
        @sync_type = N'automatic',
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @priority = 1;

    EXEC sp_addmergepushsubscription_agent
        @publication = N'Pub_Common',
        @subscriber = N'$(Q1Subscriber)',
        @subscriber_db = N'Fashion_Store',
        @job_login = NULL,
        @job_password = NULL,
        @subscriber_security_mode = 0,
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Push subscription Pub_Common -> $(Q1Subscriber) created.';
END
GO

-- To Q7
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergesubscriptions WHERE pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Common') AND subscriber_server = N'$(Q7Subscriber)')
BEGIN
    EXEC sp_addmergesubscription
        @publication = N'Pub_Common',
        @subscriber = N'$(Q7Subscriber)',
        @subscriber_db = N'Fashion_Store',
        @subscription_type = N'push',
        @subscriber_type = 0,
        @sync_type = N'automatic',
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @priority = 1;

    EXEC sp_addmergepushsubscription_agent
        @publication = N'Pub_Common',
        @subscriber = N'$(Q7Subscriber)',
        @subscriber_db = N'Fashion_Store',
        @job_login = NULL,
        @job_password = NULL,
        @subscriber_security_mode = 0,
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Push subscription Pub_Common -> $(Q7Subscriber) created.';
END
GO

-- To Thu Duc
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergesubscriptions WHERE pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Common') AND subscriber_server = N'$(TDSubscriber)')
BEGIN
    EXEC sp_addmergesubscription
        @publication = N'Pub_Common',
        @subscriber = N'$(TDSubscriber)',
        @subscriber_db = N'Fashion_Store',
        @subscription_type = N'push',
        @subscriber_type = 0,
        @sync_type = N'automatic',
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @priority = 1;

    EXEC sp_addmergepushsubscription_agent
        @publication = N'Pub_Common',
        @subscriber = N'$(TDSubscriber)',
        @subscriber_db = N'Fashion_Store',
        @job_login = NULL,
        @job_password = NULL,
        @subscriber_security_mode = 0,
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Push subscription Pub_Common -> $(TDSubscriber) created.';
END
GO

-- -------------------------------------------------
-- 4. PUBLICATION 2: Pub_Fashion_Store_Q1 (Branch CN1 -> Q1)
--    NhanVien, TaiKhoan, TonKho, HoaDon, ChiTietHoaDon (filtered CN1)
-- -------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q1')
BEGIN
    EXEC sp_addmergepublication
        @publication = N'Pub_Fashion_Store_Q1',
        @description = N'Bang rieng chi nhanh CN1 (loc MaChiNhanh = ''CN1'')',
        @status = N'active',
        @allow_push = N'true',
        @allow_pull = N'true',
        @repl_freq = N'continuous',
        @independent_agent = N'true',
        @retention = 14,
        @history_retention = 48;

    EXEC sp_addpublication_snapshot
        @publication = N'Pub_Fashion_Store_Q1',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Merge publication Pub_Fashion_Store_Q1 created.';
END
GO

-- NhanVien (subset filter MaChiNhanh = 'CN1')
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'NhanVien' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q1'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q1',
        @article = N'NhanVien',
        @source_object = N'NhanVien',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @subset_filterclause = N'MaChiNhanh = ''CN1''',
        @description = N'NhanVien loc theo MaChiNhanh = CN1';
END
GO

-- TaiKhoan (join filter via NhanVien)
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'TaiKhoan' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q1'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q1',
        @article = N'TaiKhoan',
        @source_object = N'TaiKhoan',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @description = N'TaiKhoan — join filter qua NhanVien';

    EXEC sp_addmergejoinfilter
        @publication = N'Pub_Fashion_Store_Q1',
        @article = N'TaiKhoan',
        @join_article = N'NhanVien',
        @join_filterclause = N'TaiKhoan.[MaNhanVien] = NhanVien.[MaNhanVien]',
        @join_unique_key = 1,
        @type = 1;
END
GO

-- TonKho (subset filter MaChiNhanh = 'CN1')
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'TonKho' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q1'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q1',
        @article = N'TonKho',
        @source_object = N'TonKho',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @subset_filterclause = N'MaChiNhanh = ''CN1''',
        @description = N'TonKho loc theo MaChiNhanh = CN1';
END
GO

-- HoaDon (subset filter MaChiNhanh = 'CN1')
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'HoaDon' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q1'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q1',
        @article = N'HoaDon',
        @source_object = N'HoaDon',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @subset_filterclause = N'MaChiNhanh = ''CN1''',
        @description = N'HoaDon loc theo MaChiNhanh = CN1';
END
GO

-- ChiTietHoaDon (join filter via HoaDon)
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'ChiTietHoaDon' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q1'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q1',
        @article = N'ChiTietHoaDon',
        @source_object = N'ChiTietHoaDon',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @description = N'ChiTietHoaDon — join filter qua HoaDon';

    EXEC sp_addmergejoinfilter
        @publication = N'Pub_Fashion_Store_Q1',
        @article = N'ChiTietHoaDon',
        @join_article = N'HoaDon',
        @join_filterclause = N'ChiTietHoaDon.[MaHoaDon] = HoaDon.[MaHoaDon]',
        @join_unique_key = 1,
        @type = 1;
END
GO

-- Push subscription to Q1
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergesubscriptions WHERE pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q1') AND subscriber_server = N'$(Q1Subscriber)')
BEGIN
    EXEC sp_addmergesubscription
        @publication = N'Pub_Fashion_Store_Q1',
        @subscriber = N'$(Q1Subscriber)',
        @subscriber_db = N'Fashion_Store',
        @subscription_type = N'push',
        @subscriber_type = 0,
        @sync_type = N'automatic',
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @priority = 1;

    EXEC sp_addmergepushsubscription_agent
        @publication = N'Pub_Fashion_Store_Q1',
        @subscriber = N'$(Q1Subscriber)',
        @subscriber_db = N'Fashion_Store',
        @job_login = NULL,
        @job_password = NULL,
        @subscriber_security_mode = 0,
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Push subscription Pub_Fashion_Store_Q1 -> $(Q1Subscriber) created.';
END
GO

-- -------------------------------------------------
-- 5. PUBLICATION 3: Pub_Fashion_Store_Q7 (Branch CN2 -> Q7)
--    NhanVien, TaiKhoan, TonKho, HoaDon, ChiTietHoaDon (filtered CN2)
-- -------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q7')
BEGIN
    EXEC sp_addmergepublication
        @publication = N'Pub_Fashion_Store_Q7',
        @description = N'Bang rieng chi nhanh CN2 (loc MaChiNhanh = ''CN2'')',
        @status = N'active',
        @allow_push = N'true',
        @allow_pull = N'true',
        @repl_freq = N'continuous',
        @independent_agent = N'true',
        @retention = 14,
        @history_retention = 48;

    EXEC sp_addpublication_snapshot
        @publication = N'Pub_Fashion_Store_Q7',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Merge publication Pub_Fashion_Store_Q7 created.';
END
GO

-- NhanVien (subset filter MaChiNhanh = 'CN2')
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'NhanVien' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q7'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q7',
        @article = N'NhanVien',
        @source_object = N'NhanVien',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @subset_filterclause = N'MaChiNhanh = ''CN2''',
        @description = N'NhanVien loc theo MaChiNhanh = CN2';
END
GO

-- TaiKhoan (join filter via NhanVien)
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'TaiKhoan' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q7'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q7',
        @article = N'TaiKhoan',
        @source_object = N'TaiKhoan',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @description = N'TaiKhoan — join filter qua NhanVien';

    EXEC sp_addmergejoinfilter
        @publication = N'Pub_Fashion_Store_Q7',
        @article = N'TaiKhoan',
        @join_article = N'NhanVien',
        @join_filterclause = N'TaiKhoan.[MaNhanVien] = NhanVien.[MaNhanVien]',
        @join_unique_key = 1,
        @type = 1;
END
GO

-- TonKho (subset filter MaChiNhanh = 'CN2')
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'TonKho' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q7'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q7',
        @article = N'TonKho',
        @source_object = N'TonKho',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @subset_filterclause = N'MaChiNhanh = ''CN2''',
        @description = N'TonKho loc theo MaChiNhanh = CN2';
END
GO

-- HoaDon (subset filter MaChiNhanh = 'CN2')
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'HoaDon' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q7'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q7',
        @article = N'HoaDon',
        @source_object = N'HoaDon',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @subset_filterclause = N'MaChiNhanh = ''CN2''',
        @description = N'HoaDon loc theo MaChiNhanh = CN2';
END
GO

-- ChiTietHoaDon (join filter via HoaDon)
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'ChiTietHoaDon' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q7'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_Q7',
        @article = N'ChiTietHoaDon',
        @source_object = N'ChiTietHoaDon',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @description = N'ChiTietHoaDon — join filter qua HoaDon';

    EXEC sp_addmergejoinfilter
        @publication = N'Pub_Fashion_Store_Q7',
        @article = N'ChiTietHoaDon',
        @join_article = N'HoaDon',
        @join_filterclause = N'ChiTietHoaDon.[MaHoaDon] = HoaDon.[MaHoaDon]',
        @join_unique_key = 1,
        @type = 1;
END
GO

-- Push subscription to Q7
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergesubscriptions WHERE pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_Q7') AND subscriber_server = N'$(Q7Subscriber)')
BEGIN
    EXEC sp_addmergesubscription
        @publication = N'Pub_Fashion_Store_Q7',
        @subscriber = N'$(Q7Subscriber)',
        @subscriber_db = N'Fashion_Store',
        @subscription_type = N'push',
        @subscriber_type = 0,
        @sync_type = N'automatic',
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @priority = 1;

    EXEC sp_addmergepushsubscription_agent
        @publication = N'Pub_Fashion_Store_Q7',
        @subscriber = N'$(Q7Subscriber)',
        @subscriber_db = N'Fashion_Store',
        @job_login = NULL,
        @job_password = NULL,
        @subscriber_security_mode = 0,
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Push subscription Pub_Fashion_Store_Q7 -> $(Q7Subscriber) created.';
END
GO

-- -------------------------------------------------
-- 6. PUBLICATION 4: Pub_Fashion_Store_ThuDuc (Branch CN3 -> Thu Duc)
--    NhanVien, TaiKhoan, TonKho, HoaDon, ChiTietHoaDon (filtered CN3)
-- -------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_ThuDuc')
BEGIN
    EXEC sp_addmergepublication
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @description = N'Bang rieng chi nhanh CN3 (loc MaChiNhanh = ''CN3'')',
        @status = N'active',
        @allow_push = N'true',
        @allow_pull = N'true',
        @repl_freq = N'continuous',
        @independent_agent = N'true',
        @retention = 14,
        @history_retention = 48;

    EXEC sp_addpublication_snapshot
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Merge publication Pub_Fashion_Store_ThuDuc created.';
END
GO

-- NhanVien (subset filter MaChiNhanh = 'CN3')
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'NhanVien' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_ThuDuc'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @article = N'NhanVien',
        @source_object = N'NhanVien',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @subset_filterclause = N'MaChiNhanh = ''CN3''',
        @description = N'NhanVien loc theo MaChiNhanh = CN3';
END
GO

-- TaiKhoan (join filter via NhanVien)
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'TaiKhoan' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_ThuDuc'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @article = N'TaiKhoan',
        @source_object = N'TaiKhoan',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @description = N'TaiKhoan — join filter qua NhanVien';

    EXEC sp_addmergejoinfilter
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @article = N'TaiKhoan',
        @join_article = N'NhanVien',
        @join_filterclause = N'TaiKhoan.[MaNhanVien] = NhanVien.[MaNhanVien]',
        @join_unique_key = 1,
        @type = 1;
END
GO

-- TonKho (subset filter MaChiNhanh = 'CN3')
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'TonKho' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_ThuDuc'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @article = N'TonKho',
        @source_object = N'TonKho',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @subset_filterclause = N'MaChiNhanh = ''CN3''',
        @description = N'TonKho loc theo MaChiNhanh = CN3';
END
GO

-- HoaDon (subset filter MaChiNhanh = 'CN3')
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'HoaDon' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_ThuDuc'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @article = N'HoaDon',
        @source_object = N'HoaDon',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @subset_filterclause = N'MaChiNhanh = ''CN3''',
        @description = N'HoaDon loc theo MaChiNhanh = CN3';
END
GO

-- ChiTietHoaDon (join filter via HoaDon)
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergearticles WHERE name = N'ChiTietHoaDon' AND pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_ThuDuc'))
BEGIN
    EXEC sp_addmergearticle
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @article = N'ChiTietHoaDon',
        @source_object = N'ChiTietHoaDon',
        @type = N'logbased',
        @schema_option = 0x000000000803509F,
        @pre_creation_cmd = N'none',
        @description = N'ChiTietHoaDon — join filter qua HoaDon';

    EXEC sp_addmergejoinfilter
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @article = N'ChiTietHoaDon',
        @join_article = N'HoaDon',
        @join_filterclause = N'ChiTietHoaDon.[MaHoaDon] = HoaDon.[MaHoaDon]',
        @join_unique_key = 1,
        @type = 1;
END
GO

-- Push subscription to Thu Duc
IF NOT EXISTS (SELECT 1 FROM dbo.sysmergesubscriptions WHERE pubid = (SELECT pubid FROM dbo.sysmergepublications WHERE name = N'Pub_Fashion_Store_ThuDuc') AND subscriber_server = N'$(TDSubscriber)')
BEGIN
    EXEC sp_addmergesubscription
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @subscriber = N'$(TDSubscriber)',
        @subscriber_db = N'Fashion_Store',
        @subscription_type = N'push',
        @subscriber_type = 0,
        @sync_type = N'automatic',
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @priority = 1;

    EXEC sp_addmergepushsubscription_agent
        @publication = N'Pub_Fashion_Store_ThuDuc',
        @subscriber = N'$(TDSubscriber)',
        @subscriber_db = N'Fashion_Store',
        @job_login = NULL,
        @job_password = NULL,
        @subscriber_security_mode = 0,
        @subscriber_login = N'distributed_user',
        @subscriber_password = N'$(SubscriberPassword)',
        @frequency_type = 4,
        @frequency_interval = 1,
        @frequency_subday = 8,
        @frequency_subday_interval = 1;

    PRINT N'Push subscription Pub_Fashion_Store_ThuDuc -> $(TDSubscriber) created.';
END
GO

-- -------------------------------------------------
-- 7. START THE SNAPSHOT AGENTS (needs SQL Server Agent running)
-- -------------------------------------------------
EXEC sp_startpublication_snapshot @publication = N'Pub_Common';
EXEC sp_startpublication_snapshot @publication = N'Pub_Fashion_Store_Q1';
EXEC sp_startpublication_snapshot @publication = N'Pub_Fashion_Store_Q7';
EXEC sp_startpublication_snapshot @publication = N'Pub_Fashion_Store_ThuDuc';
GO

-- -------------------------------------------------
-- 8. VERIFY
-- -------------------------------------------------
SELECT name, description, status, repl_freq, retention
FROM dbo.sysmergepublications
ORDER BY name;

SELECT p.name AS publication, s.subscriber_server AS subscriber, s.db_name AS destination_db,
       s.subscription_type, s.status
FROM dbo.sysmergesubscriptions s
JOIN dbo.sysmergepublications p ON s.pubid = p.pubid
ORDER BY p.name, s.subscriber_server;

SELECT a.name AS article, a.subset_filterclause
FROM dbo.sysmergearticles a
ORDER BY a.name;

PRINT N'Merge replication configured successfully. Check Snapshot Agent and Merge Agent jobs in SSMS Replication Monitor.';
PRINT N'Ensure the share $(SnapshotShare) is accessible by the SQL Server Agent account from each subscriber machine.';
GO
