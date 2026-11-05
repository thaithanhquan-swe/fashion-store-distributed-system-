package com.fashionstore.backend.controller;

import com.fashionstore.backend.dto.ApiResponse;
import com.fashionstore.backend.dto.response.SitePingResponse;
import com.fashionstore.backend.dto.response.SiteStatusResponse;
import com.fashionstore.backend.dto.response.SiteSummaryResponse;
import com.fashionstore.backend.dto.response.SiteSyncResponse;
import com.fashionstore.backend.dto.response.SyncLogResponse;
import com.fashionstore.backend.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/sites", "/sites"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Trạng thái Site", description = "API giám sát và đồng bộ phân tán giữa Central và các chi nhánh")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SiteController {

    SiteService siteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lấy trạng thái tất cả các site (Central và các chi nhánh)")
    public ResponseEntity<ApiResponse<List<SiteStatusResponse>>> getAllSites() {
        List<SiteStatusResponse> result = siteService.getAllSites();
        return ResponseEntity.ok(ApiResponse.<List<SiteStatusResponse>>builder()
                .message("Lấy trạng thái các site thành công")
                .result(result)
                .build());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lấy thông tin tóm tắt trạng thái site phục vụ Topbar")
    public ResponseEntity<ApiResponse<SiteSummaryResponse>> getSummary(
            @RequestParam(required = false) String branchId) {
        SiteSummaryResponse result = siteService.getSummary(branchId);
        return ResponseEntity.ok(ApiResponse.<SiteSummaryResponse>builder()
                .message("Lấy tóm tắt trạng thái site thành công")
                .result(result)
                .build());
    }

    @GetMapping("/sync-log")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lấy nhật ký đồng bộ dữ liệu")
    public ResponseEntity<ApiResponse<List<SyncLogResponse>>> getSyncLogs(
            @RequestParam(defaultValue = "30") int limit) {
        List<SyncLogResponse> result = siteService.getSyncLogs(limit);
        return ResponseEntity.ok(ApiResponse.<List<SyncLogResponse>>builder()
                .message("Lấy nhật ký đồng bộ thành công")
                .result(result)
                .build());
    }

    @PostMapping("/{siteId}/ping")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Kiểm tra kết nối và độ trễ tới một site")
    public ResponseEntity<ApiResponse<SitePingResponse>> pingSite(@PathVariable String siteId) {
        SitePingResponse result = siteService.pingSite(siteId);
        return ResponseEntity.ok(ApiResponse.<SitePingResponse>builder()
                .message("Kiểm tra kết nối thành công")
                .result(result)
                .build());
    }

    @PostMapping("/{siteId}/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Kích hoạt đồng bộ thủ công cho một site")
    public ResponseEntity<ApiResponse<SiteSyncResponse>> syncSite(@PathVariable String siteId) {
        SiteSyncResponse result = siteService.syncSite(siteId);
        return ResponseEntity.ok(ApiResponse.<SiteSyncResponse>builder()
                .message(result.message())
                .result(result)
                .build());
    }

    @PostMapping("/{siteId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Bật/tắt giả lập mất kết nối để demo sự cố phân tán")
    public ResponseEntity<ApiResponse<Boolean>> toggleSiteLink(@PathVariable String siteId) {
        boolean nextState = siteService.toggleSiteLink(siteId);
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .message(nextState ? "Đã ngắt kết nối giả lập" : "Đã khôi phục kết nối")
                .result(nextState)
                .build());
    }
}
