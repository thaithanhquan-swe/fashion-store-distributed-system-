-- =====================================================================
-- MERGE REPLICATION — Central (Publisher + Distributor) -> 3 sites
--
-- Chosen model: B1 — each site is the write owner of its own branch rows,
-- Merge keeps Central and the sites in sync in BOTH directions.
--
-- Run on the CENTRAL machine AFTER:
--   1) linked-servers.sql        (role = central)
--   2) Fashion_Central.sql       (Central database; runs common.sql)
--   3) sites/Fashion_Store.sql   (on each of the 3 sites; runs common.sql)
--   4) central-procs.sql
--   5) seed.sql                  (Central only — sites receive data by snapshot)
--
-- Prerequisites:
--   * common.sql (2026-11-03) adds a `rowguid uniqueidentifier ROWGUIDCOL`
--     column to all 8 tables. Merge replication needs it on BOTH sides.
--     Re-run common.sql on every machine if the column is missing.
--   * SQL Server Agent is running on Central (push subscriptions run there).
--   * The snapshot folder is shared and reachable by the Merge Agent, e.g.
--         New-Item -ItemType Directory -Path D:\ReplData -Force
--         New-SmbShare -Name REPLDATA -Path D:\ReplData -FullAccess Everyone
--     then set SnapshotShare below to \\<CENTRAL-PC>\REPLDATA
--   * distributed_user exists on Central and on every site (created by the
--     database scripts) with db_datareader + db_datawriter.
--     The site triggers (trg_*_NoDelete, trg_HoaDon_ValidateBranch) bypass
--     themselves when SYSTEM_USER = 'distributed_user', so the Merge Agent
--     MUST connect as that login (see @subscriber_login below).
--
-- Run from the `database/` folder:
--     sqlcmd -S localhost\FASHION -E -i replication.sql -b -f 65001
-- =====================================================================

USE [master];
GO

-- -------------------------------------------------
-- CONFIG — EDIT BEFORE RUNNING
-- -------------------------------------------------
-- All machines run the named instance FASHION on fixed port 14333, so the
-- subscriber names must include the port: "<IP>,14333".
-- (Using "<IP>\FASHION" instead requires SQL Server Browser + UDP 1434.)
:setvar SnapshotShare      "\\CENTRAL-PC\REPLDATA"
:setvar Q1Subscriber       "192.168.1.11,14333"
:setvar Q7Subscriber       "192.168.1.12,14333"
:setvar TDSubscriber       "192.168.1.13,14333"
:setvar SubscriberPassword  "Fashion@123456"

-- -------------------------------------------------
-- 0. PRECHECK — merge needs the rowguid column on every article
-- -------------------------------------------------
USE [Fashion_Central];
GO

DECLARE @missing int;

SELECT @missing = COUNT(*)
FROM sys.tables t
WHERE t.name IN (N'ChiNhanh', N'SanPham', N'KhachHang', N'NhanVien',
                 N'TaiKhoan', N'HoaDon', N'ChiTietHoaDon', N'TonKho')
  AND COL_LENGTH(N'dbo.' + t.name, N'rowguid') IS NULL;

IF @missing > 0
    THROW 51000, N'Thieu cot rowguid tren mot so bang. Tao lai DB tu Fashion_Central.sql / sites\Fashion_Store.sql (ban 2026-11-03) hoac ALTER TABLE them rowguid tren ca 4 may, roi chay lai script nay.', 1;
GO

-- -------------------------------------------------
-- 1. DISTRIBUTOR (local, on Central)
-- -------------------------------------------------
USE [master];
GO

DECLARE @Publisher sysname = CAST(SERVERPROPERTY('ServerName') AS sysname);

IF NOT EXISTS (SELECT 1 FROM sys.servers WHERE is_distributor = 1 AND name = @Publisher)
BEGIN
    -- Windows authentication: the SQL Server Agent account on Central is sysadmin,
    -- so no distributor_admin password is needed (it is auto-generated).
    EXEC sp_adddistributor
        @distributor = @Publisher;

    -- max_distretention (hours) must be >= publication @retention (14 days).
    EXEC sp_adddistributiondb
        @database         = N'distribution',
        @security_mode    = 1,
        @max_distretention = 720,     -- 30 days
        @history_retention = 48;

    EXEC sp_adddistpublisher
        @publisher          = @Publisher,
        @distribution_db    = N'distribution',
        @security_mode      = 1,                     -- Windows auth to the Publisher
        @working_directory  = N'$(SnapshotShare)',
        @trusted            = N'false',
        @thirdparty_flag    = 0;

    PRINT N'Distributor configured on ' + @Publisher;
END
ELSE
    PRINT N'Distributor already configured on ' + @Publisher;
GO

-- -------------------------------------------------
-- 2. ENABLE MERGE PUBLISHING ON Fashion_Central
--    NOTE: the option name is 'merge publish', not 'publish'.
-- -------------------------------------------------
USE [Fashion_Central];
GO

IF (SELECT is_merge_published FROM sys.databases WHERE name = N'Fashion_Central') = 0
    EXEC sp_replicationdboption
        @dbname = N'Fashion_Central', @optname = N'merge publish', @value = N'true';
GO

-- -------------------------------------------------
-- 3. PUBLICATIONS + ARTICLES + PUSH SUBSCRIPTIONS
--
--    One merge publication per branch. Every publication carries:
--      * SanPham, KhachHang            -> no filter (shared master data)
--      * NhanVien, TonKho, HoaDon      -> subset filter  MaChiNhanh = 'CNx'
--      * TaiKhoan                      -> join filter    NhanVien.MaNhanVien
--      * ChiTietHoaDon                 -> join filter    HoaDon.MaHoaDon
--
--    ChiNhanh is NOT published: sites/Fashion_Store.sql already inserts the
--    site's own branch row, and branch master data does not change at runtime.
-- -------------------------------------------------
USE [Fashion_Central];
GO

DECLARE @sites TABLE (
    Branch      varchar(10) NOT NULL,
    Publication sysname     NOT NULL,
    Subscriber  sysname     NOT NULL
);

INSERT INTO @sites (Branch, Publication, Subscriber) VALUES
    ('CN1', N'Pub_Fashion_Store_Q1',      N'$(Q1Subscriber)'),
    ('CN2', N'Pub_Fashion_Store_Q7',      N'$(Q7Subscriber)'),
    ('CN3', N'Pub_Fashion_Store_ThuDuc',  N'$(TDSubscriber)');

DECLARE @Branch varchar(10), @Pub sysname, @Sub sysname, @Subset nvarchar(400);

DECLARE site_cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT Branch, Publication, Subscriber FROM @sites;

OPEN site_cur;
FETCH NEXT FROM site_cur INTO @Branch, @Pub, @Sub;

WHILE @@FETCH_STATUS = 0
BEGIN
    -- Drop a previous definition so the script can be re-run.
    -- Any subscriber (old IP) must be removed before the publication.
    IF EXISTS (SELECT 1 FROM dbo.sysmergepublications WHERE name = @Pub)
    BEGIN
        DECLARE @OldSub sysname, @OldDb sysname;

        DECLARE sub_cur CURSOR LOCAL FAST_FORWARD FOR
            SELECT s.subscriber_server, s.db_name
            FROM dbo.sysmergesubscriptions s
            JOIN dbo.sysmergepublications p ON p.pubid = s.pubid
            WHERE p.name = @Pub;

        OPEN sub_cur;
        FETCH NEXT FROM sub_cur INTO @OldSub, @OldDb;

        WHILE @@FETCH_STATUS = 0
        BEGIN
            EXEC sp_dropmergesubscription
                @publication       = @Pub,
                @subscriber        = @OldSub,
                @subscriber_db     = @OldDb,
                @subscription_type = N'push';

            FETCH NEXT FROM sub_cur INTO @OldSub, @OldDb;
        END

        CLOSE sub_cur;
        DEALLOCATE sub_cur;

        EXEC sp_droppublication @publication = @Pub;
        PRINT N'Dropped previous publication: ' + @Pub;
    END

    -- ---- 3.1 Publication ------------------------------------------------
    EXEC sp_addmergepublication
        @publication                          = @Pub,
        @description                          = N'Merge publication for branch ' + @Branch,
        @sync_mode                            = N'native',
        @retention                            = 14,          -- days
        @allow_push                           = N'true',
        @allow_pull                           = N'false',
        @allow_anonymous                      = N'false',
        @allow_subscriber_initiated_snapshot  = N'false',
        @allow_web_synchronization            = N'false',
        @allow_partition_realignment          = N'true',
        @automatic_reinitialization_policy    = 0,
        @conflict_logging                     = N'publisher',
        @max_concurrent_merge                 = 0;
    -- NOTE: merge publications have no @repl_freq / @status (always continuous/active).

    -- ---- 3.2 Shared articles (no filter) --------------------------------
    EXEC sp_addmergearticle
        @publication            = @Pub,
        @article                = N'SanPham',
        @source_object          = N'SanPham',
        @type                   = N'table',
        @pre_creation_cmd       = N'none',
        @partition_options      = 0,
        @force_invalidate_snapshot  = 1,
        @force_reinit_subscription  = 1;

    -- Customers are written by every site (offline sales) -> two-way merge
    EXEC sp_addmergearticle
        @publication            = @Pub,
        @article                = N'KhachHang',
        @source_object          = N'KhachHang',
        @type                   = N'table',
        @pre_creation_cmd       = N'none',
        @partition_options      = 0,
        @force_invalidate_snapshot  = 1,
        @force_reinit_subscription  = 1;

    -- ---- 3.3 Branch-filtered articles -----------------------------------
    SET @Subset = N'MaChiNhanh = ''' + @Branch + N'''';

    EXEC sp_addmergearticle
        @publication            = @Pub,
        @article                = N'NhanVien',
        @source_object          = N'NhanVien',
        @type                   = N'table',
        @pre_creation_cmd       = N'none',
        @subset_filterclause    = @Subset,
        @partition_options      = 2,          -- non-overlapping, one subscriber per branch
        @force_invalidate_snapshot  = 1,
        @force_reinit_subscription  = 1;

    EXEC sp_addmergearticle
        @publication            = @Pub,
        @article                = N'TonKho',
        @source_object          = N'TonKho',
        @type                   = N'table',
        @pre_creation_cmd       = N'none',
        @subset_filterclause    = @Subset,
        @partition_options      = 2,
        @force_invalidate_snapshot  = 1,
        @force_reinit_subscription  = 1;

    EXEC sp_addmergearticle
        @publication            = @Pub,
        @article                = N'HoaDon',
        @source_object          = N'HoaDon',
        @type                   = N'table',
        @pre_creation_cmd       = N'none',
        @subset_filterclause    = @Subset,
        @partition_options      = 2,
        @force_invalidate_snapshot  = 1,
        @force_reinit_subscription  = 1;

    -- ---- 3.4 Derived articles (join filters) ----------------------------
    -- TaiKhoan: only accounts belonging to this branch's employees
    EXEC sp_addmergearticle
        @publication            = @Pub,
        @article                = N'TaiKhoan',
        @source_object          = N'TaiKhoan',
        @type                   = N'table',
        @pre_creation_cmd       = N'none',
        @partition_options      = 2,
        @identityrangemanagementoption = N'manual',
        @force_invalidate_snapshot  = 1,
        @force_reinit_subscription  = 1;

    EXEC sp_addmergefilter
        @publication        = @Pub,
        @article            = N'TaiKhoan',
        @filtername         = N'flt_TaiKhoan_NhanVien',
        @join_articlename   = N'NhanVien',
        @join_filterclause  = N'[TaiKhoan].[MaNhanVien] = [NhanVien].[MaNhanVien]',
        @join_unique_key    = 1,
        @filter_type        = 1;

    -- ChiTietHoaDon: only lines of invoices that belong to this branch
    EXEC sp_addmergearticle
        @publication            = @Pub,
        @article                = N'ChiTietHoaDon',
        @source_object          = N'ChiTietHoaDon',
        @type                   = N'table',
        @pre_creation_cmd       = N'none',
        @partition_options      = 2,
        @force_invalidate_snapshot  = 1,
        @force_reinit_subscription  = 1;

    EXEC sp_addmergefilter
        @publication        = @Pub,
        @article            = N'ChiTietHoaDon',
        @filtername         = N'flt_ChiTietHoaDon_HoaDon',
        @join_articlename   = N'HoaDon',
        @join_filterclause  = N'[ChiTietHoaDon].[MaHoaDon] = [HoaDon].[MaHoaDon]',
        @join_unique_key    = 1,
        @filter_type        = 1;

    -- ---- 3.5 Snapshot agent ---------------------------------------------
    EXEC sp_addpublication_snapshot
        @publication               = @Pub,
        @frequency_type            = 4,     -- daily
        @frequency_interval        = 1,
        @frequency_subday          = 8,     -- hours
        @frequency_subday_interval = 12,
        @publisher_security_mode   = 1;

    -- ---- 3.6 Push subscription + Merge Agent ----------------------------
    -- Global subscription: required to set a conflict-resolution priority
    -- (local/anonymous subscriptions must keep the default 0.0).
    EXEC sp_addmergesubscription
        @publication            = @Pub,
        @subscriber             = @Sub,
        @subscriber_db          = N'Fashion_Store',
        @subscription_type      = N'push',
        @subscriber_type        = N'global',
        @sync_type              = N'automatic',  -- apply the initial snapshot
        @subscription_priority  = 75.00;         -- Publisher (Central) = 100.00

    EXEC sp_addmergepushsubscription_agent
        @publication              = @Pub,
        @subscriber               = @Sub,
        @subscriber_db            = N'Fashion_Store',
        @publisher_security_mode  = 1,      -- Windows auth to the local Publisher
        @subscriber_security_mode = 0,      -- SQL login on the subscriber
        @subscriber_login         = N'distributed_user',
        @subscriber_password      = N'$(SubscriberPassword)',
        @frequency_type           = 4,      -- daily...
        @frequency_interval       = 1,
        @frequency_subday         = 4,      -- ...but every N minutes
        @frequency_subday_interval = 5;
    -- Agent job runs under the SQL Server Agent service account
    -- (no @job_login/@job_password given).

    PRINT N'Merge publication created: ' + @Pub + N' -> ' + @Sub + N' (Fashion_Store)';

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
USE [Fashion_Central];
GO

SELECT name, description, retention, retention_period_unit
FROM dbo.sysmergepublications
ORDER BY name;

EXEC sp_helpmergepublication @publication = N'Pub_Fashion_Store_Q1';

EXEC sp_helpmergearticle @publication = N'Pub_Fashion_Store_Q1';

SELECT p.name AS publication, s.subscriber_server, s.db_name,
       s.subscriber_type, s.priority, s.status
FROM dbo.sysmergepublications p
JOIN dbo.sysmergesubscriptions s ON s.pubid = p.pubid
ORDER BY p.name;
-- Join filter (TaiKhoan -> NhanVien, ChiTietHoaDon -> HoaDon) chỉ xem được
-- trong SSMS: chuột phải publication -> Properties -> Filter Rows.
GO

PRINT N'Merge replication configured. Check the Snapshot Agent and Merge Agent jobs in SSMS.';
PRINT N'If a snapshot fails, verify the share $(SnapshotShare) is reachable from every machine.';
GO
