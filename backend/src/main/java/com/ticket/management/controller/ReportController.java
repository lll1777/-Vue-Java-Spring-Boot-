package com.ticket.management.controller;

import com.ticket.management.security.UserPrincipal;
import com.ticket.management.service.ReportExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final ReportExportService reportExportService;

    public ReportController(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    @PostMapping("/export")
    @PreAuthorize("hasAnyAuthority('REPORT_EXPORT', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportTicketReport(
            @RequestBody ReportExportService.ReportExportRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        
        byte[] data = reportExportService.exportTicketReport(request);
        
        String filename = "工单报表_" + 
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + 
            ".xlsx";
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('DASHBOARD_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        if (departmentId == null && userPrincipal.getDepartmentId() != null) {
            departmentId = userPrincipal.getDepartmentId();
        }
        
        Map<String, Object> statistics = reportExportService.generateDashboardStatistics(
            departmentId, userId, startDate, endDate
        );
        
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/sla")
    @PreAuthorize("hasAnyAuthority('REPORT_SLA_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSLAStatistics(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        if (departmentId == null && userPrincipal.getDepartmentId() != null) {
            departmentId = userPrincipal.getDepartmentId();
        }
        
        Map<String, Object> statistics = reportExportService.generateSLAStatistics(
            departmentId, startDate, endDate
        );
        
        return ResponseEntity.ok(statistics);
    }

    @PostMapping("/export/{configId}")
    @PreAuthorize("hasAnyAuthority('REPORT_EXPORT', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportByConfig(
            @PathVariable Long configId,
            @RequestBody(required = false) Map<String, Object> parameters,
            @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        
        byte[] data = reportExportService.exportTicketReportByConfig(configId, parameters);
        
        String filename = "自定义报表_" + 
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + 
            ".xlsx";
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
    }
}
