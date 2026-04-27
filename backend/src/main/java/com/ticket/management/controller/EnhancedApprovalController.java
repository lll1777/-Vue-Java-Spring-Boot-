package com.ticket.management.controller;

import com.ticket.management.dto.*;
import com.ticket.management.entity.ApprovalInstance;
import com.ticket.management.security.UserPrincipal;
import com.ticket.management.service.EnhancedApprovalFlowService;
import com.ticket.management.statemachine.ApprovalStateMachine.RejectTarget;
import com.ticket.management.statemachine.ApprovalStateMachine.SignType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "审批管理", description = "多级审批流接口（增强版）")
@RestController
@RequestMapping("/api/approvals")
public class EnhancedApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedApprovalController.class);

    private final EnhancedApprovalFlowService approvalFlowService;

    public EnhancedApprovalController(EnhancedApprovalFlowService approvalFlowService) {
        this.approvalFlowService = approvalFlowService;
    }

    @Operation(summary = "提交多级审批", description = "为工单提交多级审批流：提交→部门审批→主管审批→终审→归档")
    @PostMapping("/submit")
    @PreAuthorize("hasPermission(#dto.ticketId, 'TICKET_SUBMIT_APPROVAL')")
    public ResponseEntity<ApiResponse<ApprovalDTO>> submitApproval(
            @RequestBody SubmitApprovalDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        logger.info("User {} submitting approval for ticket {}", principal.getUsername(), dto.getTicketId());
        
        ApprovalInstance instance = approvalFlowService.startMultiLevelApproval(
            dto.getTicketId(), 
            principal.getUserId()
        );
        
        ApprovalDTO result = ApprovalDTO.fromEntity(instance);
        
        return ResponseEntity.ok(ApiResponse.success(result, "审批已提交"));
    }

    @Operation(summary = "审批通过", description = "审批人通过当前审批节点")
    @PostMapping("/{instanceId}/approve")
    @PreAuthorize("hasPermission(#instanceId, 'APPROVAL_APPROVE')")
    public ResponseEntity<ApiResponse<ApprovalDTO>> approve(
            @PathVariable Long instanceId,
            @RequestBody ApprovalActionDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        logger.info("User {} approving instance {}", principal.getUsername(), instanceId);
        
        ApprovalInstance instance = approvalFlowService.approve(
            instanceId, 
            principal.getUserId(), 
            dto.getComments()
        );
        
        ApprovalDTO result = ApprovalDTO.fromEntity(instance);
        
        return ResponseEntity.ok(ApiResponse.success(result, "审批通过"));
    }

    @Operation(summary = "审批驳回", description = "审批人驳回，支持驳回至发起人、上一节点、指定节点或第一节点")
    @PostMapping("/{instanceId}/reject")
    @PreAuthorize("hasPermission(#instanceId, 'APPROVAL_REJECT')")
    public ResponseEntity<ApiResponse<ApprovalDTO>> reject(
            @PathVariable Long instanceId,
            @RequestBody RejectApprovalDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        logger.info("User {} rejecting instance {}, target: {}", 
            principal.getUsername(), instanceId, dto.getRejectTarget());
        
        RejectTarget target;
        try {
            target = RejectTarget.valueOf(dto.getRejectTarget());
        } catch (IllegalArgumentException e) {
            target = RejectTarget.INITIATOR;
        }
        
        ApprovalInstance instance = approvalFlowService.reject(
            instanceId,
            principal.getUserId(),
            dto.getComments(),
            target,
            dto.getTargetNodeId()
        );
        
        ApprovalDTO result = ApprovalDTO.fromEntity(instance);
        
        return ResponseEntity.ok(ApiResponse.success(result, "审批驳回"));
    }

    @Operation(summary = "撤回审批", description = "发起人撤回审批申请")
    @PostMapping("/{instanceId}/recall")
    public ResponseEntity<ApiResponse<ApprovalDTO>> recall(
            @PathVariable Long instanceId,
            @RequestBody RecallApprovalDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        logger.info("User {} recalling instance {}", principal.getUsername(), instanceId);
        
        ApprovalInstance instance = approvalFlowService.recall(
            instanceId,
            principal.getUserId(),
            dto.getReason()
        );
        
        ApprovalDTO result = ApprovalDTO.fromEntity(instance);
        
        return ResponseEntity.ok(ApiResponse.success(result, "审批已撤回"));
    }

    @Operation(summary = "加签", description = "支持前加签、后加签、并签三种加签类型")
    @PostMapping("/{instanceId}/add-sign")
    @PreAuthorize("hasPermission(#instanceId, 'APPROVAL_ADD_SIGN')")
    public ResponseEntity<ApiResponse<ApprovalDTO>> addSign(
            @PathVariable Long instanceId,
            @RequestBody AddSignDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        logger.info("User {} adding sign to instance {}, type: {}, signUser: {}", 
            principal.getUsername(), instanceId, dto.getSignType(), dto.getSignUserId());
        
        SignType signType;
        try {
            signType = SignType.valueOf(dto.getSignType());
        } catch (IllegalArgumentException e) {
            signType = SignType.AFTER_SIGN;
        }
        
        ApprovalInstance instance = approvalFlowService.addSign(
            instanceId,
            principal.getUserId(),
            dto.getSignUserId(),
            signType,
            dto.getReason()
        );
        
        ApprovalDTO result = ApprovalDTO.fromEntity(instance);
        
        return ResponseEntity.ok(ApiResponse.success(result, "加签成功"));
    }

    @Operation(summary = "转签/转发", description = "将审批转签给其他人")
    @PostMapping("/{instanceId}/forward")
    @PreAuthorize("hasPermission(#instanceId, 'APPROVAL_FORWARD')")
    public ResponseEntity<ApiResponse<ApprovalDTO>> forward(
            @PathVariable Long instanceId,
            @RequestBody ForwardDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        logger.info("User {} forwarding instance {} to {}", 
            principal.getUsername(), instanceId, dto.getTargetUserId());
        
        ApprovalInstance instance = approvalFlowService.forward(
            instanceId,
            principal.getUserId(),
            dto.getTargetUserId(),
            dto.getReason()
        );
        
        ApprovalDTO result = ApprovalDTO.fromEntity(instance);
        
        return ResponseEntity.ok(ApiResponse.success(result, "转签成功"));
    }
}
