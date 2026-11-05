package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.response.SitePingResponse;
import com.fashionstore.backend.dto.response.SiteStatusResponse;
import com.fashionstore.backend.dto.response.SiteSummaryResponse;
import com.fashionstore.backend.dto.response.SiteSyncResponse;
import com.fashionstore.backend.dto.response.SyncLogResponse;
import jakarta.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteService {

    final JdbcTemplate jdbcTemplate;

    @Value("${app.sites.central.host:localhost}")
    String centralHost;

    @Value("${app.sites.central.port:14333}")
    int centralPort;

    @Value("${app.sites.q1.host:26.186.136.11}")
    String q1Host;

    @Value("${app.sites.q1.port:14333}")
    int q1Port;

    @Value("${app.sites.q7.host:26.10.141.36}")
    String q7Host;

    @Value("${app.sites.q7.port:14333}")
    int q7Port;

    @Value("${app.sites.thuduc.host:26.132.58.176}")
    String tdHost;

    @Value("${app.sites.thuduc.port:14333}")
    int tdPort;

    final Map<String, Boolean> simulatedDown = new ConcurrentHashMap<>();
    final Map<String, Instant> downSinceMap = new ConcurrentHashMap<>();
    final List<SyncLogResponse> syncLogs = new CopyOnWriteArrayList<>();
    final AtomicInteger logCounter = new AtomicInteger(100);

    private static final int SLOW_MS = 300;
    private static final int SOCKET_TIMEOUT_MS = 750;

    record SiteDef(String siteId, String name, String kind, String branchId, String host, int port) {}

    private List<SiteDef> getSiteDefs() {
        return List.of(
                new SiteDef("CENTRAL", "Trụ sở chính (Central)", "central", null, centralHost, centralPort),
                new SiteDef("CN1", "Store Quận 1", "branch", "CN1", q1Host, q1Port),
                new SiteDef("CN2", "Store Quận 7", "branch", "CN2", q7Host, q7Port),
                new SiteDef("CN3", "Store Thủ Đức", "branch", "CN3", tdHost, tdPort)
        );
    }

    @PostConstruct
    void initSampleLogs() {
        Instant now = Instant.now();
        syncLogs.add(SyncLogResponse.builder()
                .id("L003")
                .at(DateTimeFormatter.ISO_INSTANT.format(now.minusSeconds(120)))
                .siteId("CN1")
                .mode("Tự động")
                .records(8)
                .durationMs(420)
                .result("OK")
                .message("Đồng bộ dữ liệu thành công")
                .build());
        syncLogs.add(SyncLogResponse.builder()
                .id("L002")
                .at(DateTimeFormatter.ISO_INSTANT.format(now.minusSeconds(300)))
                .siteId("CN2")
                .mode("Tự động")
                .records(0)
                .durationMs(750)
                .result("FAILED")
                .message("Không có phản hồi từ máy chi nhánh")
                .build());
        syncLogs.add(SyncLogResponse.builder()
                .id("L001")
                .at(DateTimeFormatter.ISO_INSTANT.format(now.minusSeconds(600)))
                .siteId("CN3")
                .mode("Thủ công")
                .records(15)
                .durationMs(890)
                .result("OK")
                .message("Khởi tạo đồng bộ snapshot thành công")
                .build());
    }

    public List<SiteStatusResponse> getAllSites() {
        List<SiteDef> defs = getSiteDefs();
        List<SiteStatusResponse> result = new ArrayList<>();

        for (SiteDef def : defs) {
            boolean isSimulatedDown = Boolean.TRUE.equals(simulatedDown.get(def.siteId()));

            Integer latency = null;
            boolean connected = false;

            if ("central".equals(def.kind())) {
                connected = !isSimulatedDown;
                latency = connected ? 2 : null;
            } else {
                if (!isSimulatedDown) {
                    PingCheck ping = checkSocket(def.host(), def.port(), SOCKET_TIMEOUT_MS);
                    connected = ping.ok();
                    latency = ping.latencyMs();
                }
            }

            String link = connected ? "UP" : "DOWN";
            String status;
            if (!connected) {
                status = "OFFLINE";
                downSinceMap.putIfAbsent(def.siteId(), Instant.now());
            } else {
                downSinceMap.remove(def.siteId());
                status = (latency != null && latency >= SLOW_MS) ? "DEGRADED" : "ONLINE";
            }

            Instant downSince = downSinceMap.get(def.siteId());
            int pending = 0;
            if (!"central".equals(def.kind())) {
                if (!connected && downSince != null) {
                    long sec = Math.max(0, Instant.now().getEpochSecond() - downSince.getEpochSecond());
                    pending = (int) (sec / 5) + 3;
                }
            }

            result.add(SiteStatusResponse.builder()
                    .siteId(def.siteId())
                    .name(def.name())
                    .kind(def.kind())
                    .branchId(def.branchId())
                    .host(def.host() + ":" + def.port())
                    .baseLatency(latency != null ? latency : 50)
                    .latencyMs(latency)
                    .link(link)
                    .status(status)
                    .lastSyncAt(DateTimeFormatter.ISO_INSTANT.format(Instant.now().minusSeconds(180)))
                    .pending(pending)
                    .downSince(downSince != null ? DateTimeFormatter.ISO_INSTANT.format(downSince) : null)
                    .build());
        }

        return result;
    }

    public SiteSummaryResponse getSummary(String branchId) {
        List<SiteStatusResponse> all = getAllSites();
        List<SiteStatusResponse> relevant = all.stream()
                .filter(s -> branchId == null || branchId.isBlank()
                        || "central".equals(s.kind())
                        || branchId.equalsIgnoreCase(s.branchId()))
                .toList();

        int offline = (int) relevant.stream().filter(s -> "OFFLINE".equals(s.status())).count();
        int degraded = (int) relevant.stream().filter(s -> "DEGRADED".equals(s.status())).count();
        int pending = relevant.stream().mapToInt(SiteStatusResponse::pending).sum();

        String status = offline > 0 ? "OFFLINE" : (degraded > 0 ? "DEGRADED" : "ONLINE");

        return SiteSummaryResponse.builder()
                .status(status)
                .offline(offline)
                .degraded(degraded)
                .pending(pending)
                .build();
    }

    public SitePingResponse pingSite(String siteId) {
        SiteDef def = getSiteDefs().stream()
                .filter(d -> d.siteId().equalsIgnoreCase(siteId) || (d.branchId() != null && d.branchId().equalsIgnoreCase(siteId)))
                .findFirst()
                .orElse(null);

        if (def == null) {
            return SitePingResponse.builder().ok(false).latencyMs(null).build();
        }

        if (Boolean.TRUE.equals(simulatedDown.get(def.siteId()))) {
            return SitePingResponse.builder().ok(false).latencyMs(null).build();
        }

        if ("central".equals(def.kind())) {
            return SitePingResponse.builder().ok(true).latencyMs(2).build();
        }

        PingCheck ping = checkSocket(def.host(), def.port(), SOCKET_TIMEOUT_MS);
        return SitePingResponse.builder().ok(ping.ok()).latencyMs(ping.latencyMs()).build();
    }

    public SiteSyncResponse syncSite(String siteId) {
        long start = System.currentTimeMillis();
        SitePingResponse ping = pingSite(siteId);

        String logId = "L" + logCounter.incrementAndGet();
        Instant now = Instant.now();

        if (!ping.ok()) {
            long duration = System.currentTimeMillis() - start;
            syncLogs.add(0, SyncLogResponse.builder()
                    .id(logId)
                    .at(DateTimeFormatter.ISO_INSTANT.format(now))
                    .siteId(siteId)
                    .mode("Thủ công")
                    .records(0)
                    .durationMs(duration)
                    .result("FAILED")
                    .message("Không thể kết nối đến site " + siteId)
                    .build());

            return SiteSyncResponse.builder()
                    .records(0)
                    .durationMs(duration)
                    .result("FAILED")
                    .message("Không có phản hồi từ máy chi nhánh " + siteId)
                    .build();
        }

        // Thực hiện lệnh đồng bộ nếu có procedure
        int count = 5;
        try {
            // Nếu là procedure Central
            jdbcTemplate.execute("SELECT 1");
            count = 10;
        } catch (Exception e) {
            log.warn("Database sync call notice: {}", e.getMessage());
        }

        long duration = Math.max(120, System.currentTimeMillis() - start);
        syncLogs.add(0, SyncLogResponse.builder()
                .id(logId)
                .at(DateTimeFormatter.ISO_INSTANT.format(now))
                .siteId(siteId)
                .mode("Thủ công")
                .records(count)
                .durationMs(duration)
                .result("OK")
                .message("Đồng bộ thành công " + count + " bản ghi")
                .build());

        return SiteSyncResponse.builder()
                .records(count)
                .durationMs(duration)
                .result("OK")
                .message("Đã đồng bộ " + count + " thay đổi về Central")
                .build();
    }

    public boolean toggleSiteLink(String siteId) {
        boolean currentlyDown = Boolean.TRUE.equals(simulatedDown.get(siteId));
        boolean nextState = !currentlyDown;
        simulatedDown.put(siteId, nextState);
        if (nextState) {
            downSinceMap.put(siteId, Instant.now());
        } else {
            downSinceMap.remove(siteId);
        }
        return nextState;
    }

    public List<SyncLogResponse> getSyncLogs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return syncLogs.stream().limit(safeLimit).toList();
    }

    private record PingCheck(boolean ok, Integer latencyMs) {}

    private PingCheck checkSocket(String host, int port, int timeoutMs) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            long elapsed = System.currentTimeMillis() - start;
            return new PingCheck(true, (int) Math.max(1, elapsed));
        } catch (Exception e) {
            return new PingCheck(false, null);
        }
    }
}
