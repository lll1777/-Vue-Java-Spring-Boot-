package com.ticket.management.controller;

import com.ticket.management.entity.User;
import com.ticket.management.repository.UserRepository;
import com.ticket.management.security.DataPermissionService;
import com.ticket.management.service.BatchProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "*")
public class SecureBatchController {

    private static final Logger logger = LoggerFactory.getLogger(SecureBatchController.class);

    private final BatchProcessingService batchProcessingService;
    private final DataPermissionService dataPermissionService;
    private final UserRepository userRepository;

    private static final Set<String> ADMIN_ROLES = new HashSet<>(Arrays.asList(
        "ROLE_ADMIN",
        "ROLE_SUPER_ADMIN",
        "ROLE_SYSTEM_ADMIN"
    ));

    public SecureBatchController(
            BatchProcessingService batchProcessingService,
            DataPermissionService dataPermissionService,
            UserRepository userRepository) {
        this.batchProcessingService = batchProcessingService;
        this.dataPermissionService = dataPermissionService;
        this.userRepository = userRepository;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(ADMIN_ROLES::contains);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未认证");
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new SecurityException("用户不存在: " + username));
    }

    private ResponseEntity<Map<String, Object>> buildForbiddenResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", HttpStatus.FORBIDDEN.value());
        response.put("message", message);
        response.put("timestamp", new Date());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<?> batchAssign(
            @RequestBody BatchAssignRequest request,
            Authentication authentication) {
        
        logger.info("Batch assign requested by user: {}", authentication.getName());
        
        if (!isAdmin()) {
            logger.warn("Non-admin user {} attempted batch assign", authentication.getName());
            return buildForbiddenResponse("权限不足：批量分配功能仅管理员可使用");
        }
        
        dataPermissionService.validateBatchOperation(request.getTicketIds(), "批量分配");
        
        User currentUser = getCurrentUser();
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchAssign(
            request.getTicketIds(),
            request.getAssigneeId(),
            currentUser.getId(),
            request.getComments()
        );

        return buildBatchResponse(result);
    }

    @PostMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<?> batchUpdateStatus(
            @RequestBody BatchStatusUpdateRequest request,
            Authentication authentication) {
        
        logger.info("Batch status update requested by user: {}", authentication.getName());
        
        if (!isAdmin()) {
            logger.warn("Non-admin user {} attempted batch status update", authentication.getName());
            return buildForbiddenResponse("权限不足：批量状态更新功能仅管理员可使用");
        }
        
        dataPermissionService.validateBatchOperation(request.getTicketIds(), "批量状态更新");
        
        User currentUser = getCurrentUser();
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchUpdateStatus(
            request.getTicketIds(),
            request.getAction(),
            currentUser.getId(),
            request.getComments()
        );

        return buildBatchResponse(result);
    }

    @PostMapping("/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<?> batchClose(
            @RequestBody BatchActionRequest request,
            Authentication authentication) {
        
        logger.info("Batch close requested by user: {}", authentication.getName());
        
        if (!isAdmin()) {
            logger.warn("Non-admin user {} attempted batch close", authentication.getName());
            return buildForbiddenResponse("权限不足：批量关闭功能仅管理员可使用");
        }
        
        dataPermissionService.validateBatchOperation(request.getTicketIds(), "批量关闭");
        
        User currentUser = getCurrentUser();
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchClose(
            request.getTicketIds(),
            currentUser.getId(),
            request.getComments()
        );

        return buildBatchResponse(result);
    }

    @PostMapping("/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<?> batchResolve(
            @RequestBody BatchActionRequest request,
            Authentication authentication) {
        
        logger.info("Batch resolve requested by user: {}", authentication.getName());
        
        if (!isAdmin()) {
            logger.warn("Non-admin user {} attempted batch resolve", authentication.getName());
            return buildForbiddenResponse("权限不足：批量解决功能仅管理员可使用");
        }
        
        dataPermissionService.validateBatchOperation(request.getTicketIds(), "批量解决");
        
        User currentUser = getCurrentUser();
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchResolve(
            request.getTicketIds(),
            currentUser.getId(),
            request.getComments()
        );

        return buildBatchResponse(result);
    }

    @PostMapping("/assign/async")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<?> batchAssignAsync(
            @RequestBody BatchAssignRequest request,
            Authentication authentication) {
        
        logger.info("Batch async assign requested by user: {}", authentication.getName());
        
        if (!isAdmin()) {
            logger.warn("Non-admin user {} attempted async batch assign", authentication.getName());
            return buildForbiddenResponse("权限不足：批量异步分配功能仅管理员可使用");
        }
        
        User currentUser = getCurrentUser();
        
        BatchProcessingService.BatchResult result = batchProcessingService.batchAssignAsync(
            request.getTicketIds(),
            request.getAssigneeId(),
            currentUser.getId(),
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
