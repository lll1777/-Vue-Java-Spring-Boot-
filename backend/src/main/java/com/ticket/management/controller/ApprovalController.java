package com.ticket.management.controller;

import com.ticket.management.entity.ApprovalInstance;
import com.ticket.management.security.UserPrincipal;
import com.ticket.management.service.ApprovalFlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/approvals")
@CrossOrigin(origins = "*")
public class ApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalController.class);

    private final ApprovalFlowService approvalFlowService;

    public ApprovalController(ApprovalFlowService approvalFlowService) {
        this.approvalFlowService = approvalFlowService;
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyAuthority('APPROVAL_START', 'ROLE_ADMIN')")
    public ResponseEntity<?> startApproval(
            @RequestBody StartApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        ApprovalInstance instance = approvalFlowService.startApproval(
            request.getTicketId(),
            request.getFlowType(),
            userPrincipal.getId()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "审批流程已启动");
        response.put("instanceId", instance != null ? instance.getId() : null);
        response.put("instanceNo", instance != null ? instance.getInstanceNo() : null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{instanceId}/approve")
    @PreAuthorize("hasAnyAuthority('APPROVAL_APPROVE', 'ROLE_ADMIN')")
    public ResponseEntity<?> approve(
            @PathVariable Long instanceId,
            @RequestBody ApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        ApprovalInstance instance = approvalFlowService.approve(
            instanceId,
            userPrincipal.getId(),
            request.getComments()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "审批通过");
        response.put("instance", instance);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{instanceId}/reject")
    @PreAuthorize("hasAnyAuthority('APPROVAL_REJECT', 'ROLE_ADMIN')")
    public ResponseEntity<?> reject(
            @PathVariable Long instanceId,
            @RequestBody ApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        ApprovalInstance instance = approvalFlowService.reject(
            instanceId,
            userPrincipal.getId(),
            request.getComments()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "审批拒绝");
        response.put("instance", instance);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{instanceId}/forward")
    @PreAuthorize("hasAnyAuthority('APPROVAL_FORWARD', 'ROLE_ADMIN')")
    public ResponseEntity<?> forward(
            @PathVariable Long instanceId,
            @RequestBody ForwardApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        ApprovalInstance instance = approvalFlowService.forward(
            instanceId,
            userPrincipal.getId(),
            request.getTargetUserId(),
            request.getReason()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "审批已转发");
        response.put("instance", instance);

        return ResponseEntity.ok(response);
    }

    public static class StartApprovalRequest {
        private Long ticketId;
        private String flowType;

        public Long getTicketId() { return ticketId; }
        public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
        public String getFlowType() { return flowType; }
        public void setFlowType(String flowType) { this.flowType = flowType; }
    }

    public static class ApprovalActionRequest {
        private String comments;

        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }

    public static class ForwardApprovalRequest {
        private Long targetUserId;
        private String reason;

        public Long getTargetUserId() { return targetUserId; }
        public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
