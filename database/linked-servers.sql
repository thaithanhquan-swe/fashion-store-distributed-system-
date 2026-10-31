-- =====================================================================
-- LINKED SERVERS — run BEFORE the database scripts
--   * On CENTRAL : creates SITE_Q1, SITE_Q7, SITE_THUDUC
--   * On each SITE: creates CENTRAL
-- Transparency rule (b): linked server names are fixed, database names are
-- identical on all 3 sites (Fashion_Store), Central is Fashion_Central.
--
-- Run from the `database/` folder:
--     sqlcmd -S <instance> -E -i linked-servers.sql -b -f 65001
-- =====================================================================

USE [master]
GO

-- -------------------------------------------------
-- CONFIG — EDIT BEFORE RUNNING
-- -------------------------------------------------

-- "central" on the Central machine, "site" on Q1 / Q7 / Thủ Đức
:setvar Role "central"

-- Machine name or IP of each node (instance name after a backslash if named)
:setvar CentralHost "192.168.1.10"
:setvar Q1Host      "192.168.1.11"
:setvar Q7Host      "192.168.1.12"
:setvar TDHost      "192.168.1.13"

-- Must match the password used when the login was created (site/Central scripts)
:setvar DistributedPassword "Fashion@123456"

-- OLE DB provider. "MSOLEDBSQL" works on SQL Server 2019/2022 with the
-- Microsoft OLE DB Driver installed (recommended). On older machines that
-- only have the legacy driver, change to "SQLNCLI11".
:setvar Provider "MSOLEDBSQL"

DECLARE @Role sysname = N'$(Role)';
DECLARE @Provider sysname = N'$(Provider)';
DECLARE @Password nvarchar(128) = N'$(DistributedPassword)';

-- -------------------------------------------------
-- CENTRAL: linked servers to the 3 sites
-- -------------------------------------------------
IF @Role = N'central'
BEGIN
    IF EXISTS (SELECT 1 FROM sys.servers WHERE name = N'SITE_Q1')
        EXEC sp_dropserver @server = N'SITE_Q1', @droplogins = 'droplogins';
    EXEC sp_addlinkedserver
        @server = N'SITE_Q1', @srvproduct = N'', @provider = @Provider,
        @datasrc = N'$(Q1Host)', @catalog = N'Fashion_Store',
        @provstr = N'TrustServerCertificate=yes;';
    EXEC sp_addlinkedsrvlogin
        @rmtsrvname = N'SITE_Q1', @useself = N'FALSE', @locallogin = NULL,
        @rmtuser = N'distributed_user', @rmtpassword = @Password;
    EXEC sp_serveroption @server = N'SITE_Q1', @optname = N'rpc out', @optvalue = N'true';

    IF EXISTS (SELECT 1 FROM sys.servers WHERE name = N'SITE_Q7')
        EXEC sp_dropserver @server = N'SITE_Q7', @droplogins = 'droplogins';
    EXEC sp_addlinkedserver
        @server = N'SITE_Q7', @srvproduct = N'', @provider = @Provider,
        @datasrc = N'$(Q7Host)', @catalog = N'Fashion_Store',
        @provstr = N'TrustServerCertificate=yes;';
    EXEC sp_addlinkedsrvlogin
        @rmtsrvname = N'SITE_Q7', @useself = N'FALSE', @locallogin = NULL,
        @rmtuser = N'distributed_user', @rmtpassword = @Password;
    EXEC sp_serveroption @server = N'SITE_Q7', @optname = N'rpc out', @optvalue = N'true';

    IF EXISTS (SELECT 1 FROM sys.servers WHERE name = N'SITE_THUDUC')
        EXEC sp_dropserver @server = N'SITE_THUDUC', @droplogins = 'droplogins';
    EXEC sp_addlinkedserver
        @server = N'SITE_THUDUC', @srvproduct = N'', @provider = @Provider,
        @datasrc = N'$(TDHost)', @catalog = N'Fashion_Store',
        @provstr = N'TrustServerCertificate=yes;';
    EXEC sp_addlinkedsrvlogin
        @rmtsrvname = N'SITE_THUDUC', @useself = N'FALSE', @locallogin = NULL,
        @rmtuser = N'distributed_user', @rmtpassword = @Password;
    EXEC sp_serveroption @server = N'SITE_THUDUC', @optname = N'rpc out', @optvalue = N'true';

    PRINT N'Linked servers created on Central: SITE_Q1, SITE_Q7, SITE_THUDUC.';
END
-- -------------------------------------------------
-- SITE: linked server back to Central
-- -------------------------------------------------
ELSE IF @Role = N'site'
BEGIN
    IF EXISTS (SELECT 1 FROM sys.servers WHERE name = N'CENTRAL')
        EXEC sp_dropserver @server = N'CENTRAL', @droplogins = 'droplogins';
    EXEC sp_addlinkedserver
        @server = N'CENTRAL', @srvproduct = N'', @provider = @Provider,
        @datasrc = N'$(CentralHost)', @catalog = N'Fashion_Central',
        @provstr = N'TrustServerCertificate=yes;';
    EXEC sp_addlinkedsrvlogin
        @rmtsrvname = N'CENTRAL', @useself = N'FALSE', @locallogin = NULL,
        @rmtuser = N'distributed_user', @rmtpassword = @Password;
    EXEC sp_serveroption @server = N'CENTRAL', @optname = N'rpc out', @optvalue = N'true';

    PRINT N'Linked server created on site: CENTRAL.';
END
ELSE
BEGIN
    RAISERROR (N'@Role must be ''central'' or ''site''. Edit the CONFIG block.', 16, 1);
END
GO

-- -------------------------------------------------
-- VERIFY
-- -------------------------------------------------
SELECT name, product, provider, data_source, is_linked
FROM sys.servers
WHERE is_linked = 1
ORDER BY name;
GO

-- After the remote database exists, test connectivity with, for example:
--   SELECT TOP (1) * FROM [SITE_Q1].[Fashion_Store].[dbo].[SanPham];   -- on Central
--   SELECT TOP (1) * FROM [CENTRAL].[Fashion_Central].[dbo].[SanPham]; -- on a site
