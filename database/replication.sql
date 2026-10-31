-- =====================================================================
-- TRANSACTIONAL REPLICATION — Central (publisher + distributor) -> 3 sites
--
-- Run on the CENTRAL machine AFTER:
--   1) linked-servers.sql        (role = central)
--   2) Fashion_Central.sql       (Central database)
--   3) sites/Fashion_Store.sql   (on each of the 3 sites)
--   4) central-procs.sql
--   5) seed.sql
--
-- Prerequisites:
--   * SQL Server Agent is running on Central (push subscriptions run there).
--   * The snapshot folder is shared and reachable by all 4 machines, e.g.
--         net share REPLDATA=C:\Replication\Snapshot /GRANT:Everyone,FULL
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
:setvar SnapshotShare      "\\CENTRAL-PC\REPLDATA"
:setvar Q1Subscriber       "192.168.1.11"
:setvar Q7Subscriber       "192.168.1.12"
:setvar TDSubscriber       "192.168.1.13"
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
-- 2. ENABLE PUBLISHING ON Fashion_Central
-- -------------------------------------------------
USE [Fashion_Central];
GO

IF (SELECT is_published FROM sys.databases WHERE name = N'Fashion_Central') = 0
    EXEC sp_replicationdboption
        @dbname = N'Fashion_Central', @optname = N'publish', @value = N'true';
GO

-- -------------------------------------------------
-- 3. PUBLICATIONS + ARTICLES + PUSH SUBSCRIPTIONS
--    One publication per site; NhanVien and TaiKhoan are row-filtered by
--    MaChiNhanh (TaiKhoan via a subquery on NhanVien).
-- -------------------------------------------------
DECLARE @sites TABLE (
    Branch     varchar(10)  NOT NULL,
    Publication sysname     NOT NULL,
    Subscriber  sysname     NOT NULL
);

INSERT INTO @sites (Branch, Publication, Subscriber) VALUES
    ('CN1', N'Pub_Fashion_Store_Q1',     N'$(Q1Subscriber)'),
    ('CN2', N'Pub_Fashion_Store_Q7',     N'$(Q7Subscriber)'),
    ('CN3', N'Pub_Fashion_Store_ThuDuc', N'$(TDSubscriber)');

DECLARE @Branch varchar(10), @Pub sysname, @Sub sysname;
DECLARE @Filter nvarchar(400), @TkFilter nvarchar(400);

DECLARE site_cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT Branch, Publication, Subscriber FROM @sites;

OPEN site_cur;
FETCH NEXT FROM site_cur INTO @Branch, @Pub, @Sub;

WHILE @@FETCH_STATUS = 0
BEGIN
    -- Drop a previous definition so the script can be re-run
    IF EXISTS (SELECT 1 FROM dbo.syspublications WHERE name = @Pub)
    BEGIN
        EXEC sp_dropsubscription @publication = @Pub, @article = N'all', @subscriber = N'all';
        EXEC sp_droppublication  @publication = @Pub;
    END

    EXEC sp_addpublication
        @publication        = @Pub,
        @description        = N'Fashion_Store snapshot for branch ' + @Branch,
        @sync_method        = N'native',
        @retention          = 0,
        @allow_push         = N'true',
        @allow_pull         = N'false',
        @allow_anonymous    = N'false',
        @enabled_for_internet = N'false',
        @independent_agent  = N'true',
        @immediate_sync     = N'false',
        @repl_freq          = N'continuous',
        @status             = N'active';

    EXEC sp_addpublication_snapshot
        @publication              = @Pub,
        @frequency_type           = 4,   -- daily
        @frequency_interval       = 1,
        @frequency_subday         = 8,   -- hours
        @frequency_subday_interval = 12;

    -- Product master: full copy, read-only on the site
    EXEC sp_addarticle
        @publication        = @Pub,
        @article            = N'SanPham',
        @source_object      = N'SanPham',
        @type               = N'logbased',
        @schema_option      = 0x000000000803509F,
        @pre_creation_cmd   = N'none',
        @description        = N'SanPham (Central -> site, read-only)';

    -- Customers: full copy; site -> Central changes use sp_DongBoKhachHang
    EXEC sp_addarticle
        @publication        = @Pub,
        @article            = N'KhachHang',
        @source_object      = N'KhachHang',
        @type               = N'logbased',
        @schema_option      = 0x000000000803509F,
        @pre_creation_cmd   = N'none',
        @description        = N'KhachHang (two-way via linked-server procedure)';

    -- Employees: only this branch
    SET @Filter = N'MaChiNhanh = ''' + @Branch + N'''';
    EXEC sp_addarticle
        @publication        = @Pub,
        @article            = N'NhanVien',
        @source_object      = N'NhanVien',
        @type               = N'logbased',
        @schema_option      = 0x000000000803509F,
        @pre_creation_cmd   = N'none',
        @filter_clause      = @Filter,
        @description        = N'NhanVien filtered by branch';

    -- Accounts: only accounts of this branch's employees (offline login)
    SET @TkFilter = N'MaNhanVien IN (SELECT MaNhanVien FROM NhanVien WHERE MaChiNhanh = '''
                    + @Branch + N''')';
    EXEC sp_addarticle
        @publication        = @Pub,
        @article            = N'TaiKhoan',
        @source_object      = N'TaiKhoan',
        @type               = N'logbased',
        @schema_option      = 0x000000000803509F,
        @pre_creation_cmd   = N'none',
        @filter_clause      = @TkFilter,
        @description        = N'TaiKhoan filtered by NhanVien.MaChiNhanh';

    -- Push subscription to the site
    EXEC sp_addsubscription
        @publication       = @Pub,
        @subscriber        = @Sub,
        @destination_db    = N'Fashion_Store',
        @subscription_type = N'push',
        @sync_type         = N'automatic',
        @article           = N'all',
        @update_mode       = N'read only';

    EXEC sp_addpushsubscription_agent
        @publication             = @Pub,
        @subscriber              = @Sub,
        @subscriber_db           = N'Fashion_Store',
        @job_login               = NULL,   -- use the SQL Server Agent account
        @job_password            = NULL,
        @subscriber_security_mode = 0,     -- SQL login on the subscriber
        @subscriber_login        = N'distributed_user',
        @subscriber_password     = N'$(SubscriberPassword)',
        @frequency_type          = 4,
        @frequency_interval      = 1,
        @frequency_subday        = 8,
        @frequency_subday_interval = 1;

    PRINT N'Publication created: ' + @Pub + N' -> ' + @Sub + N' (Fashion_Store)';

    FETCH NEXT FROM site_cur INTO @Branch, @Pub, @Sub;
END

CLOSE site_cur;
DEALLOCATE site_cur;
GO

-- -------------------------------------------------
-- 4. START THE SNAPSHOT AGENTS (needs SQL Server Agent running)
-- -------------------------------------------------
EXEC sp_startpublication_snapshot @publication = N'Pub_Fashion_Store_Q1';
EXEC sp_startpublication_snapshot @publication = N'Pub_Fashion_Store_Q7';
EXEC sp_startpublication_snapshot @publication = N'Pub_Fashion_Store_ThuDuc';
GO

-- -------------------------------------------------
-- 5. VERIFY
-- -------------------------------------------------
SELECT name, status, sync_method, repl_freq
FROM dbo.syspublications
ORDER BY name;

SELECT p.name AS publication, s.subscriber, s.destination_db, s.subscription_type, s.status
FROM dbo.syspublications p
JOIN dbo.syssubscriptions s ON s.pubid = p.pubid
ORDER BY p.name;

SELECT a.publication, a.article, a.filter_clause
FROM dbo.sysarticles a
ORDER BY a.publication, a.article;
GO

PRINT N'Replication configured. Check the Snapshot Agent and Distribution Agent jobs in SSMS.';
PRINT N'If a snapshot fails, verify the share $(SnapshotShare) is reachable from every machine.';
GO
