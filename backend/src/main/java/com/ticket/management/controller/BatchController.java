package com.ticket.management.controller;

import com.ticket.management.security.UserPrincipal;
import com.ticket.management.service.BatchProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "*")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    private final BatchProcessingService batchProcessingService;

    public BatchController(BatchProcessingService batchProcessingService) {
        this.batchProcessingService = batchProcessingService;
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyAuthority('TICKET_BATCH_ASSIGN', 'ROLE_ADMIN')")
    public ResponseEntity<?> batchAssign(
            @RequestBody BatchAssignRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchAssign(
            request.getTicketIds(),
            request.getAssigneeId(),
            userPrincipal.getId(),
            request.getComments()
        );

        return buildBatchResponse(result);
    }

    @PostMapping("/status")
    @PreAuthorize("hasAnyAuthority('TICKET_BATCH_UPDATE_STATUS', 'ROLE_ADMIN')")
    public ResponseEntity<?> batchUpdateStatus(
            @RequestBody BatchStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchUpdateStatus(
            request.getTicketIds(),
            request.getAction(),
            userPrincipal.getId(),
            request.getComments()
        );

        return buildBatchResponse(result);
    }

    @PostMapping("/close")
    @PreAuthorize("hasAnyAuthority('TICKET_BATCH_CLOSE', 'ROLE_ADMIN')")
    public ResponseEntity<?> batchClose(
            @RequestBody BatchActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchClose(
            request.getTicketIds(),
            userPrincipal.getId(),
            request.getComments()
        );

        return buildBatchResponse(result);
    }

    @PostMapping("/resolve")
    @PreAuthorize("hasAnyAuthority('TICKET_BATCH_RESOLVE', 'ROLE_ADMIN')")
    public ResponseEntity<?> batchResolve(
            @RequestBody BatchActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchResolve(
            request.getTicketIds(),
            userPrincipal.getId(),
            request.getComments()
        );

        return buildBatchResponse(result);
    }

    @PostMapping("/assign/async")
    @PreAuthorize("hasAnyAuthority('TICKET_BATCH_ASSIGN', 'ROLE_ADMIN')")
    public ResponseEntity<?> batchAssignAsync(
            @RequestBody BatchAssignRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchAssignAsync(
            request.getTicketIds(),
            request.getAssigneeId(),
            userPrincipal.getId(),
            request.getComments()
        );

        return buildBatchResponse(result);
    }

    private ResponseEntity<Map<String, Object>> buildBatchResponse(BatchProcessingService.BatchResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.getFailedCount() == 0);
        response.put("totalCount", result.getTotalCount());
        response.put("successCount", result.getSuccessCount());
        response.put("failedCount", result.getFailedCount());
        response.put("successIds", result.getSuccessIds());
        response.put("failedIds", result.getFailedIds());
        response.put("errorMessages", result.getErrorMessages());

        if (result.getFailedCount() > 0) {
            response.put("message", String.format("批量处理完成，成功 %d 条，失败 %d 条", 
                result.getSuccessCount(), result.getFailedCount()));
        } else {
            response.put("message", String.format("批量处理完成，共 %d 条", result.getSuccessCount()));
        }

        return ResponseEntity.ok(response);
    }

    public static class BatchAssignRequest {
        private List<Long> ticketIds;
        private Long assigneeId;
        private String comments;

        public List<Long> getTicketIds() { return ticketIds; }
        public void setTicketIds(List<Long> ticketIds) { this.ticketIds = ticketIds; }
        public Long getAssigneeId() { return assigneeId; }
        public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }

    public static class BatchStatusUpdateRequest {
        private List<Long> ticketIds;
        private String action;
        private String comments;

        public List<Long> getTicketIds() { return ticketIds; }
        public void setTicketIds(List<Long> ticketIds) { this.ticketIds = ticketIds; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }

    public static class BatchActionRequest {
        private List<Long> ticketIds;
        private String comments;

        public List<Long> getTicketIds() { return ticketIds; }
        public void setTicketIds(List<Long> ticketIds) { this.ticketIds = ticketIds; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }
}
